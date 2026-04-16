## Why

Entity-state class names in `skadistats.clarity.model.state` are inconsistently prefixed. Some S2 state classes carry no engine prefix (`FlatEntityState`, `NestedArrayEntityState`, `TreeMapEntityState`), while their S1 counterpart is prefixed (`S1FlatEntityState`). One S1 state (`ObjectArrayEntityState`) is missing its prefix. `AbstractS2EntityState` has the prefix in the wrong word position. This asymmetry makes unprefixed S2 classes look like the "default" and makes cross-engine comparisons visually noisy.

Aligning every state and state-adjacent interface under a consistent `S1…` / `S2…` prefix pattern makes the engine boundary visible at a glance in every import, reference, and file listing.

## What Changes

Rename 7 types. Pure refactor — no behavior change, no API semantics change.

S1:
- `ObjectArrayEntityState` → `S1ObjectArrayEntityState`

S2:
- `AbstractS2EntityState` → `S2AbstractEntityState` (flip prefix to leading position)
- `FlatEntityState` → `S2FlatEntityState`
- `NestedArrayEntityState` → `S2NestedArrayEntityState`
- `NestedArrayEntityStateIterator` → `S2NestedArrayEntityStateIterator`
- `TreeMapEntityState` → `S2TreeMapEntityState`
- `NestedEntityState` (interface) → `S2NestedEntityState`

**BREAKING** for any external caller that directly references these class names. Clarity's public entry points (processor annotations, `EntityState` interface, `S1EntityStateType` / `S2EntityStateType` enums) are unchanged.

Out of scope (explicitly not renamed):
- `FieldLayout`, `FieldLayoutBuilder`, `PrimitiveType` — S2-only layout support, not "state" classes. Whether they belong under `model/state/` at all is a separate question.
- Sub-package reorganization (`model/state/s1/`, `model/state/s2/`) — rejected; prefix preserves a flat directory where S1/S2 pairs sort together.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

Each of the following specs references one or more of the renamed classes in its prose and needs a MODIFIED delta that substitutes the new names. No requirement semantics change; only naming.

- `flat-entity-state`: updates references to `FlatEntityState` → `S2FlatEntityState`.
- `nested-entity-state`: updates references to `NestedArrayEntityState`, `NestedArrayEntityStateIterator`, `NestedEntityState`, `AbstractS2EntityState` to their `S2…` forms.
- `treemap-nested-state`: updates references to `TreeMapEntityState`, `NestedEntityState`, `AbstractS2EntityState`.
- `pointer-state-tracking`: updates references to `AbstractS2EntityState` and related S2 state classes.
- `state-mutation`: updates references to affected S1/S2 state classes.
- `mutation-trace-bench`: updates single class-name reference.
- `entity-update-commit`: updates single class-name reference.

## Impact

- **Code**: ~30 files in `clarity` touch these class names (direct references + tests). Mechanical IDE rename.
- **Public API surface**: renames are visible to any downstream code that imports the concrete state classes. Clarity Analyzer (`/home/spheenik/projects/clarity/clarity-analyzer`) and the examples subprojects should compile cleanly; no analyzer code is expected to reference these classes directly, but compile must be verified.
- **Specs**: 7 capability specs updated via MODIFIED deltas carried through the archive flow.
- **Docs / comments**: scan and fix stale class-name mentions in javadocs and inline comments.
