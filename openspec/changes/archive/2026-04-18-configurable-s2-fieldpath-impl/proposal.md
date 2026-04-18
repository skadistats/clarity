## Why

The sealed hierarchy `S2FieldPath permits S2LongFieldPath, S2ModifiableFieldPath` models a relationship that does not exist in the code. The two branches have inverted lifecycles: `S2LongFieldPath` is an immutable value used as a map key and handed to event consumers, while `S2ModifiableFieldPath` is a short-lived scratch cursor reused across `FieldOp.execute` calls. Nothing ever reads them polymorphically — every consumer wants exactly one side. The false sibling relationship forces casts in `S2TreeMapEntityState` (`(S2LongFieldPath) fpX`) and leaks long-encoding details (`S2LongFieldPathFormat.set/down/last`) into state code that should be impl-agnostic.

On top of that, the field-path representation is currently hard-coded. There is no way to experiment with alternative encodings (interned, array-backed, packed-short) without touching every state impl. We want the field-path representation to be a runner-level configuration knob — pick one at parse startup, everything downstream adapts.

## What Changes

- **BREAKING (internal):** Remove the modifiable branch from sealed `S2FieldPath`. `S2FieldPath` becomes the immutable key contract only.
- Drop `S2ModifiableFieldPath` / `S2LongModifiableFieldPath` as `S2FieldPath` subtypes. Re-introduce them as a separate concept: `S2FieldPathBuilder` (interface) and `S2LongFieldPathBuilder` (impl). The builder is **not** a `S2FieldPath`.
- Add range-construction methods to `S2FieldPath` directly (`childAt`, `upperBoundForSubtreeAt`). Hoist `Comparable<S2FieldPath>` to the interface so no middle tier is needed. `S2FieldPath` implementors satisfy the full immutable contract.
- Introduce `S2FieldPathType` — an enum that pairs a concrete `S2FieldPath` impl with its matching `S2FieldPathBuilder` factory. Adding a new path impl means adding one enum entry.
- Add `withFieldPathType(...)` knob to `SimpleRunner` / `ControllableRunner` alongside the existing `withStateType(...)` knob. Default: `LONG`.
- Rewire `S2FieldReader` to obtain builders from the configured `S2FieldPathType` and to produce `S2FieldPath`-typed keys via `builder.snapshot()`.
- Rewrite `S2TreeMapEntityState` to key on `S2FieldPath` directly with no casts: `Object2ObjectAVLTreeMap<S2FieldPath, Object>`. Range operations call `fp.childAt(...)` / `fp.upperBoundForSubtreeAt(...)` instead of reaching into `S2LongFieldPathFormat`.
- Update `S2FlatEntityState` and `S2NestedArrayEntityState` to accept `S2FieldPath` at the interface boundary and walk it via `get(i)/last()` without needing to know the concrete type.
- `FieldOp.execute(int op, S2FieldPathBuilder b, BitStream bs)` — signature tightened from `S2ModifiableFieldPath` to `S2FieldPathBuilder`.

## Capabilities

### New Capabilities
- `configurable-fieldpath-impl`: runner-level selection of the `S2FieldPath` implementation via `S2FieldPathType`, including the builder pairing contract and the default (`LONG`).

### Modified Capabilities
- `treemap-nested-state`: keys typed as `S2FieldPath` (not `S2LongFieldPath`); range ops via interface methods; no long-format leakage; no casts at `applyMutation` / `write` boundaries.
- `flat-entity-state`: accept `S2FieldPath` without downcasting to a concrete impl.
- `nested-entity-state`: accept `S2FieldPath` without downcasting to a concrete impl.
- `fieldop-switch-dispatch`: `FieldOp.execute` signature takes `S2FieldPathBuilder` in place of `S2ModifiableFieldPath`; fast/debug paths hold a builder produced by the configured `S2FieldPathType`.

## Impact

- `skadistats.clarity.model.s2`: `S2FieldPath` reshape; new `S2FieldPathBuilder` + `S2LongFieldPathBuilder`; new `S2FieldPathType` enum; `S2LongFieldPath` picks up range-op methods.
- `skadistats.clarity.io.s2`: `S2FieldReader` configured with `S2FieldPathType`; builder obtained from type; `FieldOp.execute` signature change.
- `skadistats.clarity.state.s2`: all three S2 state impls migrated off concrete-path casts.
- `skadistats.clarity.processor.runner`: `SimpleRunner` / `ControllableRunner` gain `withFieldPathType(...)`; default `LONG` preserves current behaviour.
- JMH harness (`src/jmh`): benchmarks must cover the new virtual-dispatch hot path (compare, range-op, FieldOp-on-builder) to catch regressions.
- No downstream API impact: event handlers continue to receive `FieldPath`; `S2FieldPath` remains a subtype. `clarity-analyzer` is unaffected.
- Accept/revert gate: a full-replay benchmark regression >2% on the Dota 2 corpus sinks the change.
