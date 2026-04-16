## Purpose

NestedArrayEntityState with Entry-based nested array storage and own Field+Entry traversal, dispatching on StateMutation at the leaf. NestedEntityState is an internal interface used only by NestedArrayEntityState.
## Requirements
### Requirement: NestedEntityState interface
The `NestedEntityState` interface SHALL be an internal detail of `NestedArrayEntityState`. It SHALL NOT be used as a common interface for all S2 EntityState implementations. It SHALL continue to provide nested index-based state access with the methods: `get(int idx)`, `set(int idx, Object value)`, `has(int idx)`, `clear(int idx)`, `sub(int idx)`, `isSub(int idx)`, `length()`, `capacity(int wantedSize, boolean shrinkIfNeeded)`.

#### Scenario: Interface is internal to NestedArrayEntityState
- **WHEN** `NestedEntityState` is used in the codebase
- **THEN** it is only referenced by `NestedArrayEntityState` and its inner `Entry` class
- **AND** it is NOT implemented by `TreeMapEntityState` or any other EntityState implementation

### Requirement: NestedArrayEntityState owns its traversal
`NestedArrayEntityState` SHALL implement `EntityState` directly. It SHALL provide its own `applyMutation(FieldPath, StateMutation)` that traverses the Field hierarchy via `field.getChild(idx)` and the Entry hierarchy via `entry.sub(idx)`. At the leaf, it SHALL dispatch on the `StateMutation` type.

#### Scenario: applyMutation with WriteValue
- **WHEN** `applyMutation(fp, WriteValue(value))` is called on NestedArrayEntityState
- **THEN** the traversal navigates to the correct Entry via Field+Entry hierarchy
- **AND** the value is stored via `entry.set(idx, value)`

#### Scenario: applyMutation with ResizeVector
- **WHEN** `applyMutation(fp, ResizeVector(count))` is called on NestedArrayEntityState
- **THEN** the traversal navigates to the correct Entry
- **AND** the sub-entry is resized via `entry.sub(idx).capacity(count, true)`
- **AND** the method returns true (capacity changed)

#### Scenario: applyMutation with SwitchPointer
- **WHEN** `applyMutation(fp, SwitchPointer(newSerializer))` is called on NestedArrayEntityState
- **AND** the new serializer differs from the current one
- **THEN** the existing sub-entry is cleared and a new one is created
- **WHEN** newSerializer is null
- **THEN** the sub-entry is cleared

#### Scenario: getValueForFieldPath traversal
- **WHEN** `getValueForFieldPath(fp)` is called on NestedArrayEntityState
- **THEN** it traverses the Field+Entry hierarchy and returns the value at the leaf
- **AND** it does NOT call `field.getValue(NestedEntityState, int)`

#### Scenario: Capacity ensured from Field structural info
- **WHEN** the traversal encounters a node shorter than the required index
- **THEN** it determines the required capacity from the parent Field type: `SerializerField` → `serializer.getFieldCount()`, `ArrayField` → `length`
- **AND** it ensures the Entry capacity directly, without calling `field.ensureCapacity`

#### Scenario: Existing behavior preserved
- **WHEN** `NestedArrayEntityState` is used as the S2 entity state
- **THEN** all field operations (value get/set, vector resize, pointer switching, capacity management) produce identical results to the previous implementation

### Requirement: EntryRef slot release is transitive

When `NestedArrayEntityState` releases an `EntryRef`, it SHALL also release every `EntryRef` transitively reachable through that Entry's `state[]` array, returning all of their slots to the freelist. No Entry SHALL remain in `entries` after its sole navigation path from the root has been removed.

The transitive release SHALL be triggered from both mutation primitives that remove navigation edges:

- `Entry.set(idx, value)` when `state[idx]` was an `EntryRef`
- `Entry.capacity(wantedSize, true)` when the shrink discards slots containing `EntryRef`s

The walk SHALL read `state[]` arrays and mutate `this.entries` and `this.freeEntries` directly; no sharing check or clone step is required because `state[]`, `entries`, and `freeEntries` are owned outright by `this` after `copy()` performs eager deep cloning.

#### Scenario: Overwriting an EntryRef releases its whole subtree

- **GIVEN** an Entry `A` where `A.state[i]` is `EntryRef(r1)` whose Entry `E1` has `E1.state[j]` = `EntryRef(r2)`
- **WHEN** `A.set(i, null)` is called
- **THEN** slot `r1` is returned to the freelist
- **AND** slot `r2` is returned to the freelist
- **AND** any further `EntryRef` transitively reachable from `E1.state[]` is also returned to the freelist

#### Scenario: Shrinking a vector releases EntryRefs in the discarded tail

- **GIVEN** an Entry whose `state[]` has length `N` and contains `EntryRef`s at indices ≥ `M`
- **WHEN** `Entry.capacity(M, true)` is called (shrink)
- **THEN** every `EntryRef` at indices `M..N-1` is recursively released before `state[]` is reallocated
- **AND** every Entry transitively reachable from those EntryRefs is returned to the freelist

### Requirement: NestedArrayEntityState provides a unified direct-write method

`NestedArrayEntityState` SHALL provide `write(FieldPath fp, Object decoded)` that traverses the Field hierarchy and dispatches at the leaf on the field's structural type — `WriteValue`-equivalent behavior for leaf fields, `ResizeVector`-equivalent for vector resize (leaf decoded as Integer count), `SwitchPointer`-equivalent for pointer-type fields (leaf decoded as Serializer). No `StateMutation` record is allocated; the field's own shape determines the dispatch.

`write` SHALL be observably equivalent to `applyMutation(fp, field.createMutation(decoded, fp.last() + 1))`. The existing `applyMutation(fp, StateMutation)` path is retained for non-reader callers (debug, baseline, programmatic writes).

#### Scenario: write on NestedArrayEntityState dispatches on leaf kind

- **WHEN** `nestedState.write(fp, decoded)` is called
- **THEN** the traversal navigates to the target Entry via the Field+Entry hierarchy
- **AND** at the leaf, the write proceeds based on the field's structural type — scalar write, vector resize, or pointer switch
- **AND** no `StateMutation` is allocated
- **AND** the return value matches what `applyMutation(fp, field.createMutation(decoded, ...))` would have returned

### Requirement: NestedArrayEntityState mutates state in place during readFields

`S2FieldReader.readFieldsFast` SHALL mutate `NestedArrayEntityState` in place as each field is decoded, via `state.write(fp, decoded)`. No staging of mutations into `FieldChanges.mutations[]` SHALL occur on the fast path.

Packet-level atomicity is provided by the `entity-update-commit` capability: `readFields` throwing aborts the replay run; `queuedUpdates` are cleared in the `finally` block; no state rollback is attempted. `copy()` is eager (see `NestedArrayEntityState.copy() is an eager deep copy`) and is invoked only at baseline materialization and consumer-side snapshot points, not for per-packet atomicity.

#### Scenario: Fast-path decode mutates NestedArrayEntityState immediately

- **WHEN** `readFieldsFast` processes a field whose state is `NestedArrayEntityState`
- **THEN** it calls `state.write(fp, decoded)` immediately after decode
- **AND** no entry is appended to `FieldChanges.mutations[]`
- **AND** the accumulated `capacityChanged` bit is updated from the return value of `write`

#### Scenario: Atomicity via throw-aborts-replay

- **WHEN** a packet decode throws after `NestedArrayEntityState` has been partially mutated in place
- **THEN** the exception propagates through `processAndRunPacketEntities`, which clears `queuedUpdates` in its `finally` block
- **AND** the replay run is expected to abort; no rollback of the partial mutations is performed

### Requirement: NestedArrayEntityState.copy() is an eager deep copy

`NestedArrayEntityState.copy()` SHALL return a state that is fully independent of the original at the moment of return. No `entries` list, `Entry` instance, `state[]` array, `freeEntries` deque, or `pointerSerializers` array SHALL be shared with the original after `copy()` returns. Subsequent mutations on either state SHALL NOT be observable from the other, without any additional per-write bookkeeping.

`copy()` SHALL:
1. Clone `pointerSerializers` via `Arrays.copyOf`.
2. Allocate a new `ArrayList<Entry>(entries.size())`; for each non-null `Entry` in the original's `entries`, append a freshly cloned `Entry` with `Arrays.copyOf(state, state.length)`. Null slots are preserved as null.
3. Clone `freeEntries` as a new `ArrayDeque<>(freeEntries)` (or null, matching the original's state).

Slot stability SHALL be preserved — every Entry in the clone occupies the same slab index it occupied in the original. `EntryRef.idx` values embedded in `state[]` arrays remain valid in the clone.

#### Scenario: copy() returns fully independent state

- **WHEN** `copy()` is invoked on a NestedArrayEntityState
- **THEN** the returned state's `entries` list, every non-null `Entry` and its `state[]` array, `freeEntries` deque, and `pointerSerializers` array are newly allocated
- **AND** no `Entry`, `state[]`, deque, or array in the returned state is `==` to any in the original
- **AND** every mutation on the copy (set, capacity, createEntryRef, clearEntryRef, releaseEntryRef) leaves the original's observable state unchanged
- **AND** vice versa

#### Scenario: Slot indices are preserved across copy

- **WHEN** the original has an Entry at `entries.get(k)` referenced by `EntryRef(k)` stored in some parent Entry's `state[]`
- **THEN** the copy has the Entry clone at `entries.get(k)` referenced by the same `EntryRef(k)` in the copy's cloned parent `state[]`
- **AND** the `EntryRef.idx` values stored in cloned `state[]` arrays do not need rewriting

#### Scenario: Subsequent writes do not cross-affect

- **WHEN** `copy()` is invoked and the copy is then mutated via `applyMutation`, `write`, or any structural mutation
- **THEN** no ownership check or clone-on-write operation occurs during the mutation — the write proceeds directly on the copy's independently-allocated data
- **AND** the original's state is bit-for-bit unchanged
