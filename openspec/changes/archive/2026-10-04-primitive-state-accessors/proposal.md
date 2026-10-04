## Why

Post `inline-field-mutation-apply` and `flat-entity-state`, the parser writes decoded primitive values directly into flat state slots without ever materializing a wrapper object on the decode path. The accessor contract, however, still goes through `Object` — every consumer read (`entity.getProperty(fp)`, `EntityState.getValueForFieldPath(state, fp)`) re-boxes an `int` / `long` / `float` on the way out.

This re-boxing is the only remaining cost preventing consumers from staying primitive end-to-end. It also blocks a structural win for downstream UIs like clarity-analyzer: they currently `state.copy()` the entire entity on every `@OnEntityUpdated` so an FX-thread action can read arbitrary fields later. A sparse per-field snapshot would replace the full copy — but that requires primitive accessors on a snapshot object, not just on live state.

The companion investigation into markus-wa/demoinfocs-golang's 2026 AI-assisted perf pass (`mw/perf2026`, ~19% throughput) identified two allocation-cache ideas (pre-boxed `simulationTime` cache, per-reader f32 direct-mapped cache). Both are band-aids for a boxing decoder; clarity's primitive flat state makes the disease preventable rather than curable. Primitive accessors are the dual-use API that makes that already-done work visible to consumers — anyone using clarity can opt into primitive end-to-end reads regardless of whether the analyzer ever ships a companion change.

## What Changes

- Add primitive getters on `EntityState` / `Entity`: `getInt(FieldPath)`, `getLong(FieldPath)`, `getFloat(FieldPath)`, plus `getObject(FieldPath)` as the escape hatch for non-scalar field types (Vector, String, handles already surfaced as structured types). On flat impls getters read directly off the slot backing; on nested/tree impls they unbox the stored wrapper. No allocation at the accessor boundary.
- Add `EntityState.captureChanged(FieldPath[] fps, int num) → StateDelta`: a sparse snapshot sized to `num` that holds only the named field paths' values. Intended for cross-thread hand-off where the full state would otherwise need to be copied.
- Add `StateDelta` type with the same primitive-getter contract as `EntityState`, plus `FieldPath[] fields()` to enumerate what it covers. Queries for unknown fields return a documented default (zero for primitives, `null` for `getObject`) — no exception.
- Add `EntityState.applyFrom(StateDelta, FieldPath)` (single-field) and `EntityState.applyAll(StateDelta)` (absorb everything the delta covers) as in-place merge primitives: consumers holding a long-lived consumer-side `EntityState` can absorb deltas without allocating.
- No breaking changes. The existing `getValueForFieldPath` / `getProperty` paths remain unchanged.

## Capabilities

### New Capabilities
- `primitive-state-accessors`: primitive-typed read contract on `EntityState` / `Entity` plus a sparse `StateDelta` snapshot for cross-thread or deferred-apply consumers, with in-place `applyFrom` / `applyAll` merge primitives for long-lived consumer-side states.

## Impact

- `src/main/java/skadistats/clarity/state/` — new `StateDelta` interface plus concrete impl.
- `src/main/java/skadistats/clarity/state/EntityState.java` — new static dispatchers (`getInt` / `getLong` / `getFloat` / `getObject` / `captureChanged` / `applyFrom` / `applyAll`) mirroring the existing `getValueForFieldPath(state, fp)` shape.
- `src/main/java/skadistats/clarity/state/s1/S1EntityState.java` and `s2/S2EntityState.java` — new abstract methods with engine-specific `FieldPath` subtypes.
- All six concrete impls (`S1FlatEntityState`, `S1ObjectArrayEntityState`, `S2FlatEntityState`, `S2NestedArrayEntityState`, `S2NestedEntityState`, `S2TreeMapEntityState`) — implementations of the new abstract methods.
- `src/main/java/skadistats/clarity/model/Entity.java` — thin delegating primitive getters.
- No impact to decoder, dispatch, or processor wiring — this is an additive read-side API.
- Downstream consumer `clarity-analyzer` has a companion change (`sparse-state-delta-updates`) that depends on this landing first; whether that companion actually ships is gated on the §8 bench outcome in `tasks.md`.

## Non-goals (explicit)

- **Primitive-typed `@OnEntityPropertyChanged` handlers.** The dispatch signature still boxes when invoking generic listener methods. A follow-up change could introduce primitive-specialized handler shapes (`@OnEntityIntPropertyChanged`, `FloatConsumer`-shaped callback), but that is API-surface growth and narrower than the pull-side win. Scoped out; revisit once consumer demand is visible.
- **Allocation caches inside the decoder** (pre-boxed sim-time, per-reader f32 cache from demoinfocs-golang `mw/perf2026`). Clarity's primitive decode path already never boxes, so these are moot on the parse side. They would only apply at the listener-dispatch boundary if we kept the boxed-push API — which is exactly what the non-goal above leaves open.
