## ADDED Requirements

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
