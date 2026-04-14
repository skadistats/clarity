## ADDED Requirements

### Requirement: Refs slot release is transitive

When `FlatEntityState` releases a `refs` slot that holds a sub-Entry, it SHALL also release every `FieldLayout.Ref` and `FieldLayout.SubState` slot transitively reachable through that Entry's `data` via its `rootLayout`, returning all of them to `freeSlots`. No sub-Entry or value-Ref SHALL remain in `refs` after its sole navigation path from the root has been removed.

The transitive release SHALL be triggered from both mutation primitives that remove navigation edges:

- `switchPointer` when an existing sub-Entry is replaced or cleared (the slot formerly occupied by the old sub-Entry)
- `resizeVector` shrink when the truncated tail of `sub.data` contains `FieldLayout.Ref` or `FieldLayout.SubState` slot indices

The walk SHALL only read `data` byte arrays (which may be shared under COW) and SHALL only mutate `this.refs` and `this.freeSlots` (per-copy after `ensureRefsModifiable`). It SHALL NOT modify any shared `data` content. `ensureRefsModifiable` SHALL be called before any `freeRefSlot` invocation performed during the release walk.

Plain `FieldLayout.Ref` leaves hold arbitrary `Object` values (not sub-Entries); their slots SHALL be freed non-recursively via `freeRefSlot`. `FieldLayout.SubState` slots hold sub-Entries and SHALL be released recursively.

#### Scenario: SwitchPointer releases the whole sub-Entry subtree

- **GIVEN** a pointer SubState whose current sub-Entry `E` has a `FieldLayout.Ref` at offset `r` storing slot `s1`, and a nested `FieldLayout.SubState` at offset `s` storing slot `s2` whose own sub-Entry contains further Ref/SubState slots
- **WHEN** `SwitchPointer` replaces or clears that pointer
- **THEN** the top-level slot of `E` is returned to `freeSlots`
- **AND** slot `s1` is returned to `freeSlots`
- **AND** slot `s2` and every further Ref/SubState slot reachable from its sub-Entry are returned to `freeSlots`

#### Scenario: ResizeVector shrink releases slots in the discarded tail

- **GIVEN** a vector sub-Entry whose element layout contains `FieldLayout.Ref` or `FieldLayout.SubState` positions, and element indices `[M..N-1]` are about to be dropped by a shrink from length `N` to `M`
- **WHEN** `resizeVector` applies the shrink
- **THEN** for each element index `i` in `[M..N-1]`, every occupied Ref or SubState slot reachable through that element is returned to `freeSlots` before `sub.data` is reallocated
- **AND** `ensureRefsModifiable` is called prior to those `freeRefSlot` operations

#### Scenario: Release preserves two-axis COW

- **GIVEN** two FlatEntityState copies `A` and `B` that share the same `refs` container (both with `refsModifiable = false`) and the same Entry `data` byte arrays
- **WHEN** `B` performs a release walk as part of `switchPointer` or `resizeVector`
- **THEN** `B` first calls `ensureRefsModifiable` so that the mutations hit a per-copy `refs` and `freeSlots`
- **AND** `A.refs` and `A.freeSlots` are unchanged
- **AND** no shared `data` byte array is mutated during the walk
