Implementation order matches the checkpoints in `design.md`. Each section ends with a benchmark/acceptance gate — don't advance until the gate passes.

> **Precursor (separate tiny change, lands first):** `FlatCopyBench` — JMH micro measuring pure `FlatEntityState.copy()` throughput on a realistic Hero-sized state on current master. Captures the CP-0 baseline.

## 0. Prerequisite audits

> **Audit 0.1–0.4 DONE** during design (see the conversation analysis and `clarity-examples/s1sendprops_-1.txt` dump via the `:dev:s1sendtables` example added for this purpose).
>
> **Key results:**
> - S2 `char[N]`: explicit declared max in the type name (sizes observed: 8, 18, 32, 33, 64, 128, 129, 161, 255, 256, 260, 512; mode = 256).
> - S2 `CUtlString`: **no per-prop metadata bound**. Shared `StringLenDecoder` enforces a 9-bit length prefix → wire cap 511 bytes.
> - S1 `PropType.STRING` (458 props in full Dota DT): all have `numBits = 0`, `flags = 0`, `numElements = 0`. **No metadata anywhere.** Same decoder cap.
> - Per-serializer worst-case inline footprint with uniform 512-byte reservation for unbounded strings: max ~2 KB (game modes); median ~1.3 KB; most entity types <1 KB. Accepted.
>
> **Design decision (D7)**: `char[N]` inline at declared N. Unbounded strings (S2 `CUtlString`, S1 STRING) inline at uniform **514 bytes** (2-byte prefix + 512). No threshold, no hybrid, no refs fallback for strings.

- [x] 0.5 **Entity cache audit**: enumerate caches/derived state on `Entity` that reference state-held data (property-name→fieldpath caches, pointer-serializer lookups, event-dispatch caches, dtClass pointer). Document findings; identify any that need invalidation on `setState`

  > **Results (2026-04-16):**
  > - `Entity` holds no state-derived caches of its own. Fields: `index/serial/handle/dtClass` (immutable), `existent/active/spawnGroupHandle` (primitives), `state` (the reference being swapped).
  > - Name/field-path lookups (`getNameForFieldPath`, `getFieldPathForName`, `getFieldForFieldPath`) delegate to `AbstractS2EntityState` or `S1DTClass`. The S2 lookups walk `state.pointerSerializers[]`, so they move with `setState(snapshot)` automatically — no external cache to invalidate.
  > - `S1DTClass.propsByName` is compile-time-initialized, never touched during packet processing.
  > - Event-dispatch caches (`Entities.classPatternMatchers`, `OnEntityPropertyChanged.Event.adaptersByClass`, `Adapter.propertyMatches`) key on `DTClass` (immutable) or `(DTClass, FieldPath)`. Events fire *after* commit under D9, so a failing packet's rollback never populates these caches with mid-packet state.
  > - **Conclusion: no Entity cache invalidation hook needed for CP-6.** Rollback restores `state` + `existent` only (D10). Task 6.13 can be completed as a no-op with a comment noting this audit outcome.

- [x] 0.6 **S1 ARRAY decoder audit**: 11 ARRAY props identified in the dump with `numElements` ∈ {2, 10, 16, 32, 33}. Determine whether `ArrayDecoder` produces an `Object[]` stored at one idx (→ needs a refs slot) or whether the inner primitive type permits inline expansion (`numElements × innerSize` bytes in byte[]). Outcome decides whether S1 keeps any refs slab

  > **Results (2026-04-16):**
  > - `ArrayDecoder.decode` produces a runtime-sized `Object[]` via `DecoderDispatch.decode` on each element. Count is read from the wire (`readUBitInt(nSizeBits)`); `numElements` in the sendprop metadata is only used to size the length prefix. `ArrayDecoder` is S1-only (S2 arrays go through `ArrayField` layout nodes).
  > - **Decision: defer inlining.** `S1FlatEntityState` keeps a small refs slab. The 11 ARRAY props each occupy one refs idx holding an `Object[]` with boxed inner elements, same as today. S1 refs slab is capped at 11 slots per entity class in the worst case.
  > - Rationale: S1 has 458 STRING props vs. 11 ARRAY props — inline-strings dominate the savings, array inlining is marginal. Inlining arrays also pulls unknown scope (inner template types not audited; could include STRING or nested ARRAY, both of which complicate the byte[] encoding).
  > - **Impact on tasks:** 3.5 drops `ArrayDecoder` from the `decodeInto` list. 7.8 ports int/long/float/vector/vectorXY/string `decodeInto` only; `ArrayDecoder` stays on the `DecoderDispatch.decode` → `state.write(fp, Object[])` path.
  > - **Future optimization (tracked in design.md Future follow-ups):** Extend the `s1sendtables` dumper to print `SendProp.template.type`, audit the 11 inner types, then inline primitive-inner arrays if the audit is clean.

## 1. Owner-pointer COW — FlatEntityState (CP-1)

- [x] 1.1 Replace `Entry.modifiable` (boolean) with `Entry.owner` (FlatEntityState reference); update constructor and all call sites
- [x] 1.2 Switch `refs` from `ArrayList<Object>` to `Object[]` + `int refsSize`; adjust allocate/free/get/set sites; grow via `Arrays.copyOf`
- [x] 1.3 Switch `freeSlots` from `Deque<Integer>` to `int[]` + `int freeSlotsTop`
- [x] 1.4 Replace `refsModifiable` with `refsOwner` (FlatEntityState reference); clone `refs` and `freeSlots` together on first mismatched-owner write
- [x] 1.5 Replace `pointerSerializersModifiable` with `pointerSerializersOwner` on AbstractS2EntityState (shared with NESTED_ARRAY)
- [x] 1.6 Replace `Entry.ensureModifiable()` with `makeWritable(Entry e, Consumer<Entry> slotSetter)` on FlatEntityState (used `Consumer<Entry>` over `IntConsumer`; caller passes a lambda that swaps the refs slot)
- [x] 1.7 Delete `Entry.markSubEntriesNonModifiable` and old `Entry.copy()`; delete all call sites
- [x] 1.8 Rewrite `FlatEntityState(FlatEntityState other)`: share everything by reference; invalidate owner pointers on both sides (null); no flag flipping, no tree walk at copy time. Sub-Entry ownership is invalidated lazily on first `ensureRefsOwned` (O(refsSize) iteration, not O(layout-node-count))
- [x] 1.9 Update all write sites in `applyMutation` to use owner-match + clone-on-mismatch
- [x] 1.10 Unit test: `copy()` performs no byte[] clone, no FieldLayout traversal, no Entry allocation (FlatEntityStateCowTest.copyPerformsNoByteClone)
- [x] 1.11 Unit test: deep-nested entity with polymorphic Pointer + Vector sub-Entries; copy; write into sub-entry; verify original unchanged, copy cloned only the touched path (FlatEntityStateCowTest.deepNestedWriteClonesOnlyTouchedPath)
- [x] 1.12 Unit test: copy → write to String (still in refs at this CP) → allocates new refs slot in copy only (FlatEntityStateCowTest.copyThenStringWriteClonesRefsInCopyOnly)
- [x] 1.13 Unit test: copy → SwitchPointer → clones `pointerSerializers` in copy only (FlatEntityStateCowTest.copyThenSwitchPointerClonesPointerSerializersInCopyOnly)
- [ ] 1.14 **Gate: `FlatCopyBench`** — ≥10× faster copy() vs CP-0; zero allocation except the FlatEntityState object under `-prof gc`
- [ ] 1.15 **Gate: `EntityStateParseBench FLAT`** — no regression vs CP-0

## 2. Owner-pointer COW — NestedArrayEntityState (CP-2)

- [x] 2.1 Replace `Entry.modifiable` (boolean) with `Entry.owner` (NestedArrayEntityState); single owner covers the state array too. Entry remains a non-static inner class so `new Entry()` implicitly binds to the current outer — the owner-ptr model leverages this: after `makeWritable`, owner==outer and the Entry's outer references (for `releaseEntryRef` etc.) are guaranteed correct
- [x] 2.2 Add `entriesOwner` field governing the `entries` list and `freeEntries` deque as a pair
- [x] 2.3 Rewrite copy constructor: share `entries` / `freeEntries` / per-Entry refs by reference; set `entriesOwner = null` on both sides; also invalidate `root.owner = null` so the first write on either side trips makeWritable (non-root entries invalidated lazily on first `ensureEntriesOwned`). No Entry wrapper allocations at copy time
- [x] 2.4 Add `ensureEntriesOwned()` helper — clones entries + freeEntries together if owner mismatches; iterates entries once to null sub-Entry owners (O(entriesSize) amortized, not O(layout-node-count))
- [x] 2.5 Add `makeWritable(Entry e, int slot)` — clones Entry wrapper + state array if owner mismatches; new Entry is bound to the cloning FES via inner-class semantics, so its `releaseEntryRef`/`capacityChanged` writes hit the right outer
- [x] 2.6 Update `Entry.set`, `Entry.capacity`, `Entry.subEntry` — removed the internal `modifiable` clone branch. These methods now precondition on `owner == outer this`, enforced by callers via `rootEntryWritable`/`subEntryForWrite`/`makeWritable`
- [x] 2.7 Update `createEntryRef`, `clearEntryRef`, `releaseEntryRef` to call `ensureEntriesOwned` before mutating. `markFree` was merged into `clearEntryRef` (no standalone caller survived the refactor)
- [x] 2.8 Audit `AbstractS2EntityState` — eager `pointerSerializers.clone()` already removed in CP-1 as part of 1.5. Confirmed: copy ctor shares by reference, writes gated by `ensurePointerSerializersOwned`
- [x] 2.9 Unit test: `copy()` zero Entry allocations, zero ArrayList/Deque allocations (NestedArrayEntityStateCowTest.copyZeroContainerAllocations)
- [x] 2.10 Unit test: copy → write to sub-entry at slab index k → clones only entries[k] (NestedArrayEntityStateCowTest.writeToSubEntryClonesOnlyTouchedSlab)
- [x] 2.11 Unit test: parity — applied trace on copy + original produces identical states; original unchanged (NestedArrayEntityStateCowTest.tracedWritesOnCopyMatchApplyingToFresh). Bonus: copyThenSwitchPointerClonesPointerSerializersInCopyOnly
- [x] 2.12 `NestedArrayCopyBench` — covered by `FlatCopyBench` which is parameterized `@Param({"FLAT", "NESTED_ARRAY"})`. One JMH class measures both impls' copy() pass
- [ ] 2.13 **Gate: `NestedArrayCopyBench`** — O(1) in copy(); zero alloc under `-prof gc`
- [ ] 2.14 **Gate: `EntityStateParseBench NESTED_ARRAY`** — no regression vs CP-0

## 3. Decoder.decodeInto static dispatch (CP-3)

- [ ] 3.1 Add `decodeInto` to int primitive decoders: IntSignedDecoder, IntUnsignedDecoder, IntMinusOneDecoder, IntVarSignedDecoder, IntVarUnsignedDecoder
- [ ] 3.2 Add `decodeInto` to long primitive decoders: LongSignedDecoder, LongUnsignedDecoder, LongVarSignedDecoder, LongVarUnsignedDecoder
- [ ] 3.3 Add `decodeInto` to float primitive decoders: FloatNoScaleDecoder, FloatCoordDecoder, FloatCoordMpDecoder, FloatCellCoordDecoder, FloatNormalDecoder, FloatQuantizedDecoder, FloatDefaultDecoder
- [ ] 3.4 Add `decodeInto` to BoolDecoder
- [ ] 3.5 Add `decodeInto` to compound primitive decoders: VectorDefaultDecoder, VectorDecoder, VectorXYDecoder, VectorNormalDecoder, QAngle* decoders — compose inner element `decodeInto` at `offset + i * stride`. (`ArrayDecoder` deferred — see audit 0.6.)
- [ ] 3.6 Extend `decoder-codegen` to generate `DecoderDispatch.decodeInto(BitStream, Decoder, byte[], int)` — only primitive decoders get cases; default throws
- [ ] 3.7 Unit test: for each primitive decoder, `decodeInto` byte-for-byte matches `PrimitiveType.write(..., decode(bs))`
- [ ] 3.8 Add `DecodeIntoBench` micro — per-decoder comparison
- [ ] 3.9 Update `decoder-codegen` spec delta if needed
- [ ] 3.10 **Gate: `DecodeIntoBench`** — zero allocations under `-prof gc`; ns/op ≥ existing path

## 4. FlatEntityState.decodeInto and write (CP-4)

- [ ] 4.1 Add `decodeInto(FieldPath, Decoder, BitStream)` to FlatEntityState; traversal mirrors `applyMutation`
- [ ] 4.2 At Primitive leaf: makeWritable, set flag byte to 1, call `DecoderDispatch.decodeInto`, return `oldFlag == 0`
- [ ] 4.3 At Ref leaf (strings before CP-5 inline them): fall back to `decode` + existing Ref write logic
- [ ] 4.4 At SubState leaf: throw `IllegalStateException`
- [ ] 4.5 Add `write(FieldPath, Object)` to FlatEntityState — single direct-write dispatching on leaf shape: Primitive → `PrimitiveType.write`; Ref → refs slot write; SubState-Pointer → switch pointer logic (clear old sub-Entry, set pointerSerializers, lazy-create new); SubState-Vector → resize vector sub-Entry
- [ ] 4.6 Unit test: `decodeInto` byte-identical to `applyMutation(WriteValue(decode))` for all primitive decoders + layout shapes
- [ ] 4.7 Unit test: `write` produces results identical to `applyMutation(WriteValue)` / `applyMutation(ResizeVector)` / `applyMutation(SwitchPointer)` across all layout shapes
- [ ] 4.8 Unit test: `decodeInto` capacity-change return (null→value only per D5)
- [ ] 4.9 Unit test: `decodeInto` / `write` after `copy()` triggers owner-pointer COW on touched path only
- [ ] 4.10 Add `FlatWriteBench` micro — decodeInto vs applyMutation-WriteValue
- [ ] 4.11 **Gate: `FlatWriteBench`** — zero allocation on primitive writes; ns/op ≥30% improvement vs applyMutation path

## 5. Inline strings in byte[] (CP-5)

- [ ] 5.1 Extend FieldLayout: add inline-string leaf shape (`Primitive.String(offset, prefixBytes, maxLength)` or equivalent encoding on existing `Primitive`)
- [ ] 5.2 Update FieldLayoutBuilder: emit inline-string leaves for all String props. Use declared `N` for `char[N]` (S2); use 512 for unbounded strings (S2 `CUtlString`, all S1 STRING). 2-byte length prefix uniformly
- [ ] 5.3 Add `StringLenDecoder.decodeInto(BitStream, byte[], int offset)` — writes 2-byte length prefix + UTF-8 bytes; asserts decoded length ≤ slot's declared maxLength
- [ ] 5.4 Extend `FlatEntityState.decodeInto` to handle the inline-string leaf shape (dispatch to `StringLenDecoder.decodeInto`)
- [ ] 5.5 Extend `FlatEntityState.write` for inline-string leaf — decoded is a `String`, encode to UTF-8 bytes + length prefix, write inline
- [ ] 5.6 Extend `FlatEntityState.getValueForFieldPath` for inline-string leaf — read length prefix, allocate String from bytes
- [ ] 5.7 Refs slab becomes sub-Entry-only in S2. Audit `allocateRefSlot` / `freeRefSlot` call sites — all should now be sub-Entry lifecycle only
- [ ] 5.8 Unit test: inline-string roundtrip — decode a String, verify byte-level layout, read back via `getValueForFieldPath`
- [ ] 5.9 Unit test: string exceeding declared maxLength throws (schema-violation path)
- [ ] 5.10 Unit test: copy + inline-string write triggers owner-pointer COW on byte[] only, no refs clone
- [ ] 5.11 Add `InlineStringBench` micro — decode+read roundtrip, inline vs pre-change refs path
- [ ] 5.12 **Gate: byte[] memory footprint** — per-entity-type byte[] growth ≤ 2 KB (documented in §0 audit)
- [ ] 5.13 **Gate:** `getValueForFieldPath` for inline-string allocates exactly one String per call; no unexpected per-read allocations beyond that

## 6. Unified readFieldsFast + Entities snapshot/rollback (CP-6)

- [ ] 6.1 Add `state.write(FieldPath, Object)` to `EntityState` interface; implement on `FlatEntityState` (done 4.5), `NestedArrayEntityState`, `TreeMapEntityState`
- [ ] 6.2 Modify `FieldChanges` — add `boolean capacityChanged`; fast-path constructor `(FieldPath[], int, boolean)` leaving `mutations == null`; retain legacy constructor for debug/baseline
- [ ] 6.3 Adjust `FieldChanges.applyTo(state)` to return `capacityChanged` directly when `mutations == null`
- [ ] 6.4 Rewrite `S2FieldReader.readFieldsFast` — hoist `isFlat`; per-field check `isFlat && field.isPrimitiveLeaf()` routes to `decodeInto`, else `state.write`; accumulate capacityChanged; no staging
- [ ] 6.5 Remove `pointerOverrides[]` from `readFieldsFast`; subsequent `resolveField` reads current serializers from `state.pointerSerializers` directly
- [ ] 6.6 Leave `readFieldsDebug` untouched
- [ ] 6.7 Add reusable snapshot scratch on `Entities`: `EntityState[] snapshotScratch`, `boolean[] existsSnapshotScratch`, `int[] dirtyIndices`, `int dirtyTop`; sized to maxEntityIndex; allocated once
- [ ] 6.8 Add `snapshotAndCopy(Entity entity)` helper — idempotent per packet
- [ ] 6.9 Call `snapshotAndCopy` at top of each state-mutating queue method: update, create, recreate, enter, leave, delete
- [ ] 6.10 Rewrite each `queueEntity*` method to mutate eagerly + queue event-only lambdas
- [ ] 6.11 Add `Entity.setExists(boolean)` setter for rollback
- [ ] 6.12 Wrap `processPacketEntities` in try/catch/finally: rollback iterates dirtyIndices restoring state + exists; finally nulls dirty slots, resets dirtyTop, clears queuedUpdates
- [ ] 6.13 Implement `Entity` cache invalidation hooks identified in audit 0.5 (if any)
- [ ] 6.14 Unit test: happy path — all entities commit, events fire, scratch resets
- [ ] 6.15 Unit test: mid-packet failure — all touched entities rolled back; no events fire
- [ ] 6.16 Unit test: same-entity double-update — snapshotted once
- [ ] 6.17 Unit test: `Entity.exists` rollback for queueEntityLeave failure (D10 bug fix)
- [ ] 6.18 Unit test: observer in `@OnEntityUpdated` calling `getByIndex` on another same-packet entity sees committed state
- [ ] 6.19 Integration: full Dota 2 replay, FLAT, bit-identical event stream vs CP-0 baseline
- [ ] 6.20 Integration: same replay, NESTED_ARRAY, bit-identical event stream
- [ ] 6.21 **Gate: `EntityStateParseBench FLAT`** — ≥20% wall-clock improvement vs CP-0 (document actual)
- [ ] 6.22 **Gate: `EntityStateParseBench NESTED_ARRAY`** — neutral or improved
- [ ] 6.23 **Gate: `EntityStateParseBench TREE_MAP`** — regression accepted; measured delta documented
- [ ] 6.24 **Gate: `-prof gc` on FLAT parse** — autobox count near-zero

## 7. S1 full port (CP-7)

- [ ] 7.1 Create `S1FlatEntityState` class — byte[] + optional refs slab + owner pointer(s), single-level static layout
- [ ] 7.2 Create `S1FieldLayout` — per-DTClass immutable table mapping `idx → (leafKind, offset, maxLength)`. Computed once at `S1DTClass` compile time, shared across all instances
- [ ] 7.3 Update `S1DTClass` to precompute and cache the layout
- [ ] 7.4 Implement `S1FlatEntityState.copy()` — shares layout + byte[] + refs by reference; owner pointers null. O(1)
- [ ] 7.5 Implement `S1FlatEntityState.decodeInto(fp, decoder, bs)` — makeWritable + `DecoderDispatch.decodeInto(bs, decoder, data, layout.offsetOf(fp))`
- [ ] 7.6 Implement `S1FlatEntityState.write(fp, decoded)` — dispatch on `layout.kindOf(fp)`: Primitive → `PrimitiveType.write`; inline-string → encode + write; Ref (if any survived audit 0.6) → refs slot write
- [ ] 7.7 Implement `S1FlatEntityState.getValueForFieldPath` — read from byte[] or refs based on leaf kind
- [ ] 7.8 Port S1 decoders (`io/decoder/factory/s1/*`) to add `decodeInto`: int decoders, long decoders, float decoders, vector decoders (3-float inline), VectorXY (2-float inline), StringLenDecoder (inline per task 5.4). `ArrayDecoder` keeps the existing boxing path → `state.write(fp, Object[])` → refs slot (see audit 0.6)
- [ ] 7.9 Extend `DecoderDispatch.decodeInto` to cover S1-specific decoder ids (via codegen)
- [ ] 7.10 Rewrite `S1FieldReader.readFields` to decode-direct: `state.decodeInto` for primitives/inline-strings; `state.write` for refs
- [ ] 7.11 Delete `ObjectArrayEntityState` and `S1EntityStateType` legacy references
- [ ] 7.12 Unit test: `S1FlatEntityState.copy()` is O(1), zero allocation under `-prof gc`
- [ ] 7.13 Unit test: `S1FlatEntityState.decodeInto` byte-for-byte parity with `PrimitiveType.write(..., decode(bs))` for each ported S1 decoder
- [ ] 7.14 Unit test: `write` parity — write(fp, decoded) == old-applyMutation(WriteValue) across all leaf kinds
- [ ] 7.15 Unit test: owner-ptr COW on byte[] and refs (if present)
- [ ] 7.16 Add `S1EntityStateParseBench` (or extend EntityStateParseBench to S1) — measure full S1 parse time
- [ ] 7.17 Integration: full old Dota 2 S1 replay, bit-identical event stream vs CP-0 baseline
- [ ] 7.18 **Gate: `S1EntityStateParseBench`** — substantial improvement vs CP-0 S1 baseline
- [ ] 7.19 **Gate:** `-prof gc` on S1 parse — autobox allocation near-zero

## 8. MutationListener contract (CP-8)

- [ ] 8.1 Add `onUpdateWrite(EntityState, FieldPath)` to `MutationListener` with default no-op
- [ ] 8.2 Invoke `onUpdateWrite` after each `decodeInto` + each `state.write` in `S2FieldReader.readFieldsFast` and `S1FieldReader.readFields` when listener attached
- [ ] 8.3 Continue invoking `onUpdateMutation` from paths that still produce materialized `StateMutation` (debug reader, baseline, programmatic, trace replay)
- [ ] 8.4 Update `MutationRecorder` to implement `onUpdateWrite` — read back via `getValueForFieldPath`
- [ ] 8.5 Verify `MutationTraceBench` still replays traces correctly
- [ ] 8.6 **Gate:** `dtinspector` smoke test — runs cleanly on a sample replay

## 9. Final validation

- [ ] 9.1 Run full bench matrix (`EntityStateParseBench`, `MutationTraceBench`, `FlatCopyBench`, `NestedArrayCopyBench`, `DecodeIntoBench`, `FlatWriteBench`, `InlineStringBench`, `S1EntityStateParseBench`) — record before/after across all impls
- [ ] 9.2 Run JMH `-prof gc` across benches — document autobox/alloc deltas
- [ ] 9.3 Run `:dev:dtinspectorRun` on sample replay
- [ ] 9.4 Run `:repro:issue289Run`, `:repro:issue350Run`
- [ ] 9.5 Document measured improvements in `proposal.md` "Why" or a separate `RESULTS.md` before archiving

## 10. Documentation and cleanup

- [ ] 10.1 Remove dead code: `modifiable` flag machinery (FLAT + NESTED_ARRAY), `markSubEntriesNonModifiable`, unused `Entry.copy()` call sites, staging-buffer fields on FieldChanges for fast path, `pointerOverrides[]`, `ObjectArrayEntityState`
- [ ] 10.2 Verify no static-analysis warnings remain in touched classes
- [ ] 10.3 Update `CLAUDE.md` / `README` if mutation-pipeline shape is documented (likely not)
- [ ] 10.4 `./gradlew build` — all tests green
- [ ] 10.5 `openspec validate accelerate-flat-entity-state --strict` — passes
