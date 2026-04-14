## Why

Both `NestedArrayEntityState` and `FlatEntityState` recycle sub-Entry storage via a slab allocator (`entries: ArrayList<Entry>` + `freeEntries` for Nested; `refs: List<Object>` + `freeSlots` for Flat). When a sub-Entry is replaced or a vector is shrunk, only the **top-level** slot is returned to the freelist. Any slots referenced transitively from the discarded subtree are never released — their Entry objects linger in the slab as zombies, and their slots are never reused.

Leak sites:

- `NestedArrayEntityState.Entry.set(idx, value)` calls `clearEntryRef` only on the directly replaced ref, not on refs inside the subtree.
- `NestedArrayEntityState.Entry.capacity(wantedSize, true)` shrink drops the tail of `state[]` without inspecting the abandoned slots.
- `FlatEntityState.switchPointer` calls `freeRefSlot(oldSlot)` without recursing into the sub-Entry's Ref/SubState positions.
- `FlatEntityState.resizeVector` shrink truncates `sub.data` without releasing Ref/SubState slots referenced by the discarded tail elements.

This is memory hygiene, not correctness — state produces correct results, but the slab grows beyond what's actually referenced.

## What Changes

- `NestedArrayEntityState`: introduce `releaseEntryRef(EntryRef)` that walks the subtree via `state[]` and returns every `EntryRef` to the freelist. Wire it into `Entry.set` (overwrite of an `EntryRef`) and `Entry.capacity` (shrink tail).
- `FlatEntityState`: introduce `releaseRefSlot(int slot)` + `releaseRefsInEntry(Entry, FieldLayout, int)` that walk the Entry via its `FieldLayout`, freeing `FieldLayout.Ref` and `FieldLayout.SubState` slots recursively. Wire into `switchPointer` (replaces the direct `freeRefSlot(oldSlot)`) and the shrink branch of `resizeVector` (walk the discarded tail).
- Walks only read from (possibly shared) `state[]`/`data` arrays and only mutate per-copy `entries`/`freeEntries` / `refs`/`freeSlots` — COW sharing is preserved across copies.

## Capabilities

### Modified Capabilities

- `nested-entity-state` — adds the transitive release invariant for `EntryRef` slots.
- `flat-entity-state` — adds the transitive release invariant for `refs` slots.

## Impact

- `skadistats.clarity.model.state.NestedArrayEntityState` — internal cleanup, no API change.
- `skadistats.clarity.model.state.FlatEntityState` — internal cleanup, no API change.
- No externally visible behaviour change except lower steady-state heap usage for entities with frequent vector resizes or pointer switches.
- All 151 state tests continue to pass.
