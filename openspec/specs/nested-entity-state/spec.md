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

### Requirement: NestedArrayEntityState provides O(1) copy-on-write via owner pointers

`NestedArrayEntityState` SHALL provide constant-time `copy()` via owner-pointer lazy COW. The per-`Entry` `boolean modifiable` flag SHALL be replaced by a `NestedArrayEntityState owner` pointer on each `Entry`. Ownership of the slab (the `entries` list and its companion `freeEntries` deque) SHALL be governed by a new `entriesOwner` field on `NestedArrayEntityState`. Ownership of the `pointerSerializers` array SHALL be governed by `pointerSerializersOwner` (shared with the `flat-entity-state` capability, on `AbstractS2EntityState`).

`copy()` SHALL:
1. Share `entries` by reference
2. Share `freeEntries` by reference
3. Share `pointerSerializers` by reference (owner = null)
4. Set `entriesOwner = null` on the new state (meaning: not owned until first slab-mutating write)
5. Leave per-Entry `owner` references untouched — the new state's identity will not match them, so first write through any Entry triggers per-Entry COW
6. NOT construct any `Entry` wrappers
7. NOT allocate an `ArrayList` or `ArrayDeque`
8. NOT iterate `entries` to rebuild the `freeEntries` deque (today's `markFree` loop is removed)

On first write that needs to mutate the slab (`createEntryRef`, `clearEntryRef`, `releaseEntryRef`, or any path that calls `markFree` or `entries.set`):
- If `entriesOwner != this`: clone `entries` (new `ArrayList<>(entries)`) and `freeEntries` (new `ArrayDeque<>(freeEntries)` or null if original is null) together, and set `entriesOwner = this`
- `entries` and `freeEntries` SHALL always be cloned together — they represent one logical allocator state

On first write that needs to mutate an `Entry` (its `state` array contents, or its `state.length` via `capacity`):
- Invoke the slab-ownership check first (above)
- If `entries.get(slot).owner != this`: construct a new `Entry` with a cloned `state` array and `owner = this`, assign it at `entries.set(slot, cloned)`
- The per-Entry `owner` pointer also governs the `state` array — a single pointer is sufficient; the state array is never shared between two Entries with distinct owners

Slot indices (`EntryRef.idx`) remain valid across COW — they index into whichever `entries` container the current state currently owns.

The previous `Entry.modifiable` flag machinery SHALL be removed in favor of the owner-pointer mechanism above.

#### Scenario: copy() is O(1)

- **WHEN** `copy()` is invoked on a NestedArrayEntityState
- **THEN** the method completes in constant time regardless of entity size or sub-entry count
- **AND** no `Entry` object is constructed
- **AND** no `ArrayList` is allocated
- **AND** no `ArrayDeque` is allocated
- **AND** no iteration over `entries` occurs

#### Scenario: First write clones slab once

- **WHEN** a NestedArrayEntityState is copied via `copy()`
- **AND** the copy performs its first state-mutating write
- **THEN** the copy detects `entriesOwner != this`, clones `entries` and `freeEntries` together, and sets `entriesOwner = this`
- **AND** the original NestedArrayEntityState's `entries` and `freeEntries` remain unmodified

#### Scenario: First write to a sub-Entry clones only that Entry

- **WHEN** a NestedArrayEntityState is copied and the copy writes to a sub-Entry at slab index k
- **THEN** the slab is cloned (if not already), the `Entry` at index k is cloned (wrapper + `state` array), and the clone is placed at `entries[k]` with `owner = copy`
- **AND** entries at indices other than k are untouched
- **AND** the original's `Entry` at index k remains in the original's `entries`

#### Scenario: Copy and modify independently

- **WHEN** a NestedArrayEntityState is copied
- **AND** several writes land on the copy across different sub-entries
- **THEN** exactly those sub-entries whose `state` arrays were written to have their state arrays cloned
- **AND** sub-entries not touched by the copy's writes remain shared by reference with the original
- **AND** the original's state is observably unchanged

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

Packet-level atomicity is provided by the caller (`Entities.processAndRunPacketEntities`) via snapshot-before-decode and rollback-on-failure — see the `entity-update-commit` capability. Because `copy()` is now O(1), the snapshot cost per touched entity is negligible.

#### Scenario: Fast-path decode mutates NestedArrayEntityState immediately

- **WHEN** `readFieldsFast` processes a field whose state is `NestedArrayEntityState`
- **THEN** it calls `state.write(fp, decoded)` immediately after decode
- **AND** no entry is appended to `FieldChanges.mutations[]`
- **AND** the accumulated `capacityChanged` bit is updated from the return value of `write`

#### Scenario: Atomicity via snapshot-and-rollback

- **WHEN** a packet decode throws after `NestedArrayEntityState` has been partially mutated in place
- **THEN** the entity's pre-packet state (captured via `snapshotAndCopy` in `Entities`) is restored via `entity.setState(snapshot)`
- **AND** no observer sees the partial mutations
