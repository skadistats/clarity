## ADDED Requirements

### Requirement: Primitive read contract on State and Entity

`State` and `Entity` SHALL expose primitive-typed read accessors — `getInt(FieldPath)`, `getLong(FieldPath)`, `getFloat(FieldPath)`, and `getObject(FieldPath)` — that return the value at the given field path without allocating a wrapper object. The existing `Object`-typed read API (`getValueForFieldPath`, `getProperty`) SHALL continue to function and SHALL be implemented as a boxing layer over the primitive getters.

#### Scenario: Primitive read round-trips a decoded value

- **WHEN** a consumer calls `state.getInt(fp)` on a field path whose field is typed as an unsigned 32-bit integer and whose slot was most recently written with value `42`
- **THEN** the call SHALL return `42` as an `int`, without allocating an `Integer`

#### Scenario: Object accessor covers non-scalar fields

- **WHEN** a consumer calls `state.getObject(fp)` on a field path whose field is a vector, string, or handle type
- **THEN** the call SHALL return the structured value reference that `getValueForFieldPath` would have returned, unchanged

#### Scenario: Read against an unpopulated sub-tree on a nested state

- **WHEN** the concrete state implementation is tree-shaped (e.g., `nested-entity-state`) and a consumer calls `state.getInt(fp)` on a field path that navigates through a sub-tree which has never been materialized (e.g., an unset variable-array slot)
- **THEN** the call SHALL return the zero default (`0` for `getInt`/`getLong`, `0.0f` for `getFloat`, `null` for `getObject`), matching `getValueForFieldPath`'s existing behavior for the same case

#### Scenario: Read-side contract is storage-shape agnostic

- **WHEN** the concrete state implementation is either flat (typed primitive arrays) or nested (tree of containers with boxed wrapper leaves)
- **THEN** the primitive *read* accessors (`getInt` / `getLong` / `getFloat`) SHALL return the primitive value without allocating at the accessor boundary. On flat state this is a direct typed-array load; on nested state this is a wrapper lookup followed by an allocation-free unbox. Decode-side allocation cost (boxing on write) is a property of the storage impl and is not introduced by this API

#### Scenario: Generic read path preserves existing behavior

- **WHEN** a consumer calls `state.getValueForFieldPath(fp)` on any field path
- **THEN** the return value SHALL be identical to pre-change behavior (same boxing outcome, same null semantics, same field-type coverage), with boxing now performed only at the generic edge rather than inside storage

### Requirement: Sparse StateDelta snapshot

`State` SHALL expose `StateDelta captureChanged(FieldPath[] fps, int num)` that produces a sparse, immutable snapshot of the primitive values at exactly the first `num` field paths in the input array. The returned `StateDelta` SHALL provide the same primitive-read contract as `State` — `getInt`, `getLong`, `getFloat`, `getObject` — scoped to the captured set, plus `FieldPath[] fields()` to enumerate coverage. Allocation of a delta SHALL be proportional to `num`, not to total state size.

#### Scenario: Capture of three changed fields

- **WHEN** a consumer calls `state.captureChanged(new FieldPath[]{fpA, fpB, fpC}, 3)` where `fpA` is an int field, `fpB` is a float field, and `fpC` is a vector field
- **THEN** the returned `StateDelta` SHALL return `fpA`'s current int value via `getInt(fpA)`, `fpB`'s current float value via `getFloat(fpB)`, and `fpC`'s current vector reference via `getObject(fpC)`

#### Scenario: Delta is decoupled from subsequent state mutations

- **WHEN** a consumer captures a delta at time T, then the underlying state's `fpA` slot is mutated at time T+1
- **THEN** reading `getInt(fpA)` on the delta SHALL still return the value from time T

#### Scenario: Unknown field path returns documented default

- **WHEN** a consumer calls `getInt(fpX)` on a delta where `fpX` was not part of the input array passed to `captureChanged`
- **THEN** the call SHALL return `0` (for primitive getters) or `null` (for `getObject`), without throwing

#### Scenario: Wrong primitive type returns documented default

- **WHEN** a consumer calls `getInt(fpB)` on a delta where `fpB` is a float field
- **THEN** the call SHALL return `0`, without throwing

### Requirement: In-place delta merge

`State` SHALL expose `applyFrom(StateDelta delta, FieldPath fp)` that writes the value for `fp` from `delta` into the corresponding flat slot of the target state, in place and without allocation. Fields of `delta` not named by `fp` in a given call SHALL NOT be touched on the target.

#### Scenario: Single-field merge into a long-lived target state

- **WHEN** a consumer holds a persistent `State` instance `fxState` and receives a `StateDelta` containing `fpA`
- **AND** the consumer calls `fxState.applyFrom(delta, fpA)`
- **THEN** `fxState.getInt(fpA)` SHALL subsequently return the value `delta.getInt(fpA)` returned, and no other fields of `fxState` SHALL have changed

#### Scenario: Object-typed merge copies the reference

- **WHEN** `fp` is a vector field and the delta was captured when the source state held reference `V`
- **AND** the consumer calls `target.applyFrom(delta, fp)`
- **THEN** `target.getObject(fp) == V` SHALL be true by reference identity (no defensive copy of the vector)

#### Scenario: Merge preserves source storage shape end-to-end

- **WHEN** a `StateDelta` is captured from a source state and merged into a target state of the same impl shape (flat → flat, or nested → nested)
- **THEN** the merge SHALL NOT introduce any boxing that the source did not already pay. For flat-to-flat, primitives stay primitive throughout. For nested-to-nested, the delta carries the already-allocated wrapper reference from source to target; no new `Integer` / `Float` is allocated during `applyFrom`

### Requirement: Allocation-free primitive reads

A `getInt` / `getLong` / `getFloat` call on a fresh `State` SHALL allocate zero objects. A `getObject` call SHALL allocate nothing beyond what the stored reference already dictates (i.e., no wrappers around the stored object).

#### Scenario: Read loop does not grow the heap

- **WHEN** a consumer reads the same primitive field path on a live state in a tight loop
- **THEN** allocation profiling SHALL report zero bytes allocated per loop iteration attributable to the read itself
