## MODIFIED Requirements

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

## ADDED Requirements

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

## REMOVED Requirements

### Requirement: NestedArrayEntityState provides O(1) copy-on-write via owner pointers

**Reason**: CP-6 of `accelerate-flat-entity-state` eliminated the packet-atomicity requirement that motivated owner-pointer COW. Actual `copy()` call sites (baseline CREATE/RECREATE, `ClientFrame.Capsule`, clarity-analyzer per-event snapshots) all mutate on the live-state side after every snapshot, which under COW pushes the clone into the parser's hot path via `makeWritable`. Eager `copy()` performs the same total clone work, moves it out of the decode inner loop, and removes the per-write owner-check tax along with significant bookkeeping complexity.

**Migration**: `NestedArrayEntityState.copy()` becomes an eager deep copy (see `NestedArrayEntityState.copy() is an eager deep copy`). The per-`Entry` `owner` pointer, the slab-level `entriesOwner`, and the shared `pointerSerializersOwner` (on `AbstractS2EntityState`) are deleted. Owner-enforcement helpers are removed from `set`, `capacity`, `sub`, `createEntryRef`, `clearEntryRef`, and `releaseEntryRef`. No external API consumer is affected.
