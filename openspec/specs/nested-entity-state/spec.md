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

The walk SHALL only read `state[]` arrays (which may be shared under COW) and SHALL only mutate `this.entries` and `this.freeEntries` (which are per-copy). It SHALL NOT modify any shared `state[]` content.

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

#### Scenario: Release preserves COW

- **GIVEN** two copies `A` and `B` that share a `state[]` array containing `EntryRef(r)`
- **WHEN** `B` releases `EntryRef(r)` via a shrink or overwrite
- **THEN** `B.entries[r]` is set to null and `r` is added to `B.freeEntries`
- **AND** `A.entries[r]` remains the original Entry
- **AND** `A.freeEntries` is unchanged
- **AND** neither copy mutates the shared `state[]` array during the walk

