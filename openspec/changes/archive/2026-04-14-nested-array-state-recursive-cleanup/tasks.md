## 1. NestedArrayEntityState

- [x] 1.1 Add `releaseEntryRef(EntryRef)` helper that walks `state[]` of the referenced Entry, recursively releasing nested `EntryRef`s, then calls `clearEntryRef`
- [x] 1.2 Replace the direct `clearEntryRef` call in `Entry.set` with `releaseEntryRef`
- [x] 1.3 In `Entry.capacity` shrink branch, walk `state[wantedSize..curSize)` and call `releaseEntryRef` on each `EntryRef` before reallocating the array

## 2. FlatEntityState

- [x] 2.1 Add `releaseRefSlot(int slot)` helper that inspects `refs.get(slot)`, recurses via `releaseRefsInEntry` if it's an Entry, then calls `freeRefSlot`
- [x] 2.2 Add `releaseRefsInEntry(Entry, FieldLayout, int)` helper that walks Composite/Array layouts, frees plain `Ref` slots via `freeRefSlot`, and recurses into `SubState` slots via `releaseRefSlot`
- [x] 2.3 Replace the direct `freeRefSlot(oldSlot)` call in `switchPointer` with `releaseRefSlot(oldSlot)`
- [x] 2.4 In `resizeVector` shrink branch, after `hasAnyOccupiedPath`, call `ensureRefsModifiable` and walk `sub.data` elements in `[newCount..oldCount)`, calling `releaseRefsInEntry` on each before the arraycopy

## 3. Slab hygiene tests

- [x] 3.1 Add package-private `slabSize()` / `freeSlotCount()` to `NestedArrayEntityState` and `FlatEntityState` for test-only inspection
- [x] 3.2 Add `slabImpls` data provider (NESTED_ARRAY, FLAT) and `liveSlabCount` helper in `EntityStateTest`
- [x] 3.3 Test: `switchPointerToNullReleasesNestedSubtree` — clearing an outer pointer recursively releases the whole nested subtree (slab returns to baseline)
- [x] 3.4 Test: `switchPointerToDifferentSerializerReleasesOldSubtree` — switching to a simpler serializer reclaims slots from the deeper old subtree
- [x] 3.5 Test: `resizeVectorShrinkReleasesDroppedSubEntries` — shrink releases slab slots referenced by the discarded tail
- [x] 3.6 Test: `resizeVectorToZeroReleasesAllElementSubEntries` — resize to 0 releases all element slots
- [x] 3.7 Test: `freedSlotsAreReusedBySubsequentAllocations` — after clear+reallocate, slab size is stable (slots reused, not appended)
- [x] 3.8 Test: `releaseOnCopyDoesNotAffectOriginal` — release on a copy leaves the original's slab live count unchanged and its data intact

## 4. Verification

- [x] 4.1 Full `EntityStateTest` suite (163 tests across NESTED_ARRAY, TREE_MAP, FLAT) passes
- [x] 4.2 Full clarity test suite passes
