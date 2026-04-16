## 1. Pre-change baseline

- [x] 1.1 Baseline re-used from archived `accelerate-flat-entity-state/RESULTS.md` (post-CP-6 HEAD ≈ `ebe8d2f`): `EntityStateParseBench` FLAT = 1646 ms, NESTED_ARRAY = 1730 ms, TREE_MAP = 2057 ms (3 warmup + 10 measurement, single-shot; Dota 2 replay `replays/dota/s2/340/8168882574_1198277651.dem`, JDK 21.0.10). `-prof gc` FLAT alloc = 9.11 GB/inv. No fresh baseline capture — measurement budget is one end-of-change `EntityStateParseBench` run (see §6).
- [x] 1.2 Inventory confirmed: COW production surface is `AbstractS2EntityState.java` (`pointerSerializersOwner`), `FlatEntityState.java`, `NestedArrayEntityState.java`. `NestedArrayEntityStateIterator.java` is clean (its earlier `modifiable` match was a field-path `unmodifiable()` call, unrelated). No other production file references COW machinery.
- [x] 1.3 Catalogue: `FlatEntityStateCowTest` — `copyPerformsNoByteClone` + `deepNestedWriteClonesOnlyTouchedPath` are pure COW (DELETE); `copyThenStringWriteClonesRootOnly` + `copyThenSwitchPointerClonesPointerSerializersInCopyOnly` have value-correctness tails worth keeping (STRIP identity checks, KEEP value assertions). `NestedArrayEntityStateCowTest` — `copyZeroContainerAllocations` + `writeToSubEntryClonesOnlyTouchedSlab` are pure COW (DELETE); `tracedWritesOnCopyMatchApplyingToFresh` is pure value-equality (KEEP as-is); `copyThenSwitchPointerClonesPointerSerializersInCopyOnly` same treatment as Flat's. Rename both classes to `*CopyTest`.

## 2. AbstractS2EntityState

- [x] 2.1 Deleted `pointerSerializersOwner` field.
- [x] 2.2 Copy constructor clones `pointerSerializers` unconditionally via `.clone()`.
- [x] 2.3 Deleted `ensurePointerSerializersOwned()` helper. 5 call sites remain in `FlatEntityState` (3) and `NestedArrayEntityState` (2); removed with §3/§4.
- [x] 2.4 Grep confirmed: no `pointerSerializersOwner` references outside the three state classes. No copy-constructor lines in FlatEntityState/NestedArrayEntityState touch `pointerSerializersOwner` (both use `super(other)` which now clones directly).

## 3. FlatEntityState

- [x] 3.1 Deleted `Entry.owner` field; constructor is `Entry(FieldLayout, byte[])`.
- [x] 3.2 Deleted `refsOwner` field. `pointerSerializersOwner` lives on `AbstractS2EntityState` and was handled in §2.
- [x] 3.3 Deleted `rootEntryWritable()`, `makeWritable`, `ensureRefsOwned()`. `Consumer<Entry>` import removed.
- [x] 3.4 Copy constructor: clone `refs` + `freeSlots` arrays, deep-clone each `Entry` in `refs[0..refsSize-1]`, clone `rootEntry`. `pointerSerializers` cloned via `super(other)`.
- [x] 3.5 SubState descent in `applyMutation` simplified to direct `sub = (Entry) refs[slot]`; no `makeWritable`.
- [x] 3.6 `writeValue`: `ensureRefsOwned()` removed from Ref branch.
- [x] 3.7 `resizeVector`: `ensureRefsOwned()` and `makeWritable(sub, ...)` calls removed; mutate `sub.data`/`sub.rootLayout`/`refs`/`freeSlots` directly.
- [x] 3.8 `switchPointer`: `ensureRefsOwned()` + `ensurePointerSerializersOwned()` removed; direct mutation.
- [x] 3.9 `decodeInto` SubState descent simplified same as §3.5. Primitive/InlineString leaf paths unchanged (no owner checks were in them anyway).
- [x] 3.10 `write` SubState descent simplified same as §3.5.
- [x] 3.11 `releaseRefsInEntry` / `releaseRefSlot` / `freeRefSlot`: no ownership pre-conditions anymore; straight slab mutation.
- [x] 3.12 Audited remaining methods — `getValueForFieldPath`, `fieldPathIterator`/`walk`, `lazyCreateSubEntry`, `growVectorIfNeeded` — no residual owner references.
- [x] 3.13 `markSubEntriesNonModifiable` absent (verified via grep at start of §1.2).

## 4. NestedArrayEntityState

- [x] 4.1 Deleted `Entry.owner` field; `Entry.toString` no longer references owner state.
- [x] 4.2 Deleted `entriesOwner` field.
- [x] 4.3 Deleted `makeWritable(Entry, int)`, `rootEntryWritable()`, `ensureEntriesOwned()`, `markFreeAlreadyOwned()` helpers.
- [x] 4.4 Copy constructor: clones `entries` via explicit non-null-Entry loop (each new Entry bound to `this` via inner-class semantics with cloned `state[]`), clones `freeEntries` via `new ArrayDeque<>(freeEntries)`. `pointerSerializers` cloned via `super(other)`.
- [x] 4.5 `Entry.set` already had no owner check; removed obsolete "Caller must have made this Entry writable" comment.
- [x] 4.6 `Entry.capacity` already had no owner check.
- [x] 4.7 `Entry.subEntry`: replaced `makeWritable(entries.get(entryRef.idx), entryRef.idx)` call in `subEntryForWrite` with direct `entries.get(entryRef.idx)`.
- [x] 4.8 `createEntryRef`, `clearEntryRef`, `releaseEntryRef`: removed `ensureEntriesOwned()` preamble; straight slab mutation. `markFreeAlreadyOwned` folded into `clearEntryRef` (direct `freeEntries.add(idx)`).
- [x] 4.9 `applyMutation`, `write`: `rootEntryWritable()` → `rootEntry()`. `getValueForFieldPath` was already owner-free.
- [x] 4.10 Release walks are straight mutation (no "may be shared" distinction).
- [x] 4.11 `NestedArrayEntityStateIterator` confirmed clean — its earlier `modifiable` hit was `fp.unmodifiable()` (field-path API), unrelated to COW.

## 5. Tests

- [x] 5.1 Replaced `FlatEntityStateCowTest` with `FlatEntityStateCopyTest`: 4 value-correctness tests (primitive, deep-nested, inline-string, switch-pointer independence).
- [x] 5.2 `FlatEntityStateCopyTest` is the new home; all remaining tests are value-correctness.
- [x] 5.3 Same for `NestedArrayEntityStateCowTest` → `NestedArrayEntityStateCopyTest`: 4 value-correctness tests including `tracedWritesOnCopyMatchApplyingToFresh` (kept as-is, pure value equality).
- [x] 5.4 `./gradlew test` — green after also stripping 3 COW-identity assertions in `FlatEntityStateDecodeIntoTest` (`decodeIntoAfterCopyClonesRootOnlyForPrimitive`, `writeAfterCopyClonesRootOnlyForInlineString`, `writeAfterCopyDoesNotClonePointerSerializersForInnerPrimitive`) — renamed and simplified to value-correctness.
- [x] 5.5 `:repro:issue289Run` — completed 5000 iterations (fd/mmap stress) cleanly. `:repro:issue350Run` — 107358 ticks processed cleanly.

## 6. Benchmarks

One end-of-change bench run only; narrower scope than a full perf audit (per user direction). `FlatWriteBench`, `FlatCopyBench`, and `MutationTraceBench` are not re-run for this change — their expected directions (write ↑, copy ↓, trace unchanged) follow mechanically from the deletions; RESULTS.md will note this.

- [x] 6.1 Bench ran at `bench-results/2026-04-16_170748_next-513a8e4/`. Results: FLAT 1619.1 ms (was 1646, **-1.6%**) / 8.86 GB alloc (was 9.11, **-2.7%**); NESTED_ARRAY 1734.1 ms (was 1730, +0.2% noise); TREE_MAP 2055.6 ms (was 2057, -0.1% noise).
- [x] 6.2 **Gate passed**: FLAT improved on both axes; NESTED_ARRAY and TREE_MAP within noise.

## 7. Consumers

- [x] 7.1 `./gradlew :dev:compileJava` (from clarity-examples) — clean compile.
- [x] 7.2 `./gradlew compileJava` in `clarity-analyzer` — clean compile against modified clarity via composite build.

## 8. Artifacts & validation

- [x] 8.1 `RESULTS.md` written with headline table, alloc delta, scope notes on skipped benches, and acceptance checklist. Also documents the lazy-`freeEntries` bonus simplification applied during implementation review.
- [x] 8.2 `openspec validate strip-entity-state-cow --strict` — passes.
- [x] 8.3 `./gradlew build` — green.
