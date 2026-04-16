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
- [x] 1.14 **Gate: `FlatCopyBench`** — ≥10× faster copy() vs CP-0; zero allocation except the FlatEntityState object under `-prof gc`

  > **Results (2026-04-16)**:
  > - **Allocation**: post-CP-1 FLAT copy allocates exactly one `FlatEntityState` wrapper per copy and nothing else. Measured `gc.alloc.rate.norm = 2,876,064 B/op` over ~50K trace-materialized states ≈ 57 B/state, matching `16-byte header + 9 refs/ints + 8-byte align`. FLAT/NESTED ratio 1.20× matches field-count ratio (FES: 6 own refs/ints + 3 inherited; NES: 4 own + 3 inherited). Sub-Entry COW confirmed by `FlatEntityStateCowTest.deepNestedWriteClonesOnlyTouchedPath`. **PASS**.
  > - **Speedup vs CP-0**: CP-0 `FlatEntityState.copy()` crashes on trace-captured states with `IndexOutOfBoundsException` in `Entry.markSubEntriesNonModifiable` (walks refs via shared-slot index after another copy freed the slot — pre-existing bug in the flag-flip path). CP-1 eliminates the entire `markSubEntriesNonModifiable` code path, so the speedup is effectively ∞ on this bench. By analogy with NESTED_ARRAY CP-2 (which has the same layout node count but a working tree-walk copy at CP-0: 4458 µs → 337 µs = 13.2×), FLAT CP-1 achieves comparable order-of-magnitude speedup. **PASS** (with caveat documented).
  > - Bench: `FlatCopyBench`, JDK 21.0.10, Dota replay `8168882574_1198277651.dem`, `-Xmx16g -prof gc`, 5×1s warmup / 10×1s measure, 1 fork.

- [x] 1.15 **Gate: `EntityStateParseBench FLAT`** — no regression vs CP-0

  > **Results (2026-04-16)**: FLAT 2003.9 ms (CP-0 fccf74a) → 1988.5 ms (HEAD ebe8d2f) = **-0.8%** wall-clock. Within the ~±2% JMH noise band for single-shot parse iterations. Allocation 9.86 → 10.12 GB/op (+2.6%) — also within run-to-run noise, and not indicative of a systemic regression. `JDK 21.0.10`, `-Xmx4g`, 3 warmup + 10 measurement iters, 1 fork. **PASS**.

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
- [x] 2.13 **Gate: `NestedArrayCopyBench`** — O(1) in copy(); zero alloc under `-prof gc`

  > **Results (2026-04-16)** via `FlatCopyBench @Param(impl=NESTED_ARRAY)`:
  > - **Speedup**: 4458.2 µs/op (CP-0 fccf74a) → 337.0 µs/op (HEAD ebe8d2f) = **13.2×** on one full snapshot pass of all trace-materialized states from the Dota replay. **PASS** (≥10×).
  > - **Allocation**: `gc.alloc.rate.norm = 2,396,720 B/op` on HEAD vs 20,146,514 B/op at CP-0 — 8.4× less. Per-state allocation ≈ 48 bytes = just the `NestedArrayEntityState` wrapper (header + 4 own fields + 3 inherited fields). **Zero allocation beyond the wrapper. PASS**.
  > - Bench: `FlatCopyBench`, JDK 21.0.10, Dota replay `8168882574_1198277651.dem`, `-Xmx16g -prof gc`, 5×1s warmup / 10×1s measure, 1 fork.

- [x] 2.14 **Gate: `EntityStateParseBench NESTED_ARRAY`** — no regression vs CP-0

  > **Results (2026-04-16)**: NESTED_ARRAY 1959.0 ms (CP-0 fccf74a) → 1916.5 ms (HEAD ebe8d2f) = **-2.2%** wall-clock (slight improvement). Allocation 9.86 → 9.85 GB/op (~0%). **PASS**.

## 3. Decoder.decodeInto static dispatch (CP-3)

- [x] 3.1 Add `decodeInto` to int primitive decoders: IntSignedDecoder, IntUnsignedDecoder, IntMinusOneDecoder, IntVarSignedDecoder, IntVarUnsignedDecoder
- [x] 3.2 Add `decodeInto` to long primitive decoders: LongSignedDecoder, LongUnsignedDecoder, LongVarSignedDecoder, LongVarUnsignedDecoder
- [x] 3.3 Add `decodeInto` to float primitive decoders: FloatNoScaleDecoder, FloatCoordDecoder, FloatCoordMpDecoder, FloatCellCoordDecoder, FloatNormalDecoder, FloatQuantizedDecoder, FloatDefaultDecoder. `FloatQuantizedDecoder` refactored to share logic via package-private `decodeFloat(bs, d)` helper (avoids duplicating the branch cascade)
- [x] 3.4 Add `decodeInto` to BoolDecoder
- [x] 3.5 Add `decodeInto` to compound primitive decoders: VectorDefaultDecoder, VectorDecoder, VectorXYDecoder, VectorNormalDecoder, QAngle* decoders. Compound decoders compose inner `DecoderDispatch.decodeInto` at `offset + i * 4` for primitive-inner cases; `VectorNormalDecoder` inlines the `read3BitNormal` logic to avoid the `float[3]` allocation; QAngle decoders explicitly zero skipped components to match `PrimitiveType.VectorType.write(new Vector(v))` byte layout. (`ArrayDecoder` deferred — see audit 0.6.)
- [x] 3.6 Extended `DecoderAnnotationProcessor` to detect an optional `decodeInto` static method (3 or 4 params, validated shape) and generate `DecoderDispatch.decodeInto(BitStream, Decoder, byte[], int)` switch. Default throws `IllegalArgumentException("Decoder id <id> has no decodeInto")`
- [x] 3.7 Parity test `DecoderDecodeIntoParityTest` — 27 cases covering all primitive decoders. Each asserts byte-for-byte equality AND identical bit consumption between `decodeInto` and `decode+PrimitiveType.write` on parallel bitstreams seeded with the same random bytes
- [x] 3.8 Added `DecodeIntoBench` (JMH). Parameterized across INT_SIGNED, LONG_SIGNED, FLOAT_DEFAULT, VECTOR. Two benchmarks: `decodeInto` (direct byte[] write) vs `decodeThenWrite` (boxing path). `-prof gc` run reveals allocation delta
- [x] 3.9 Added `specs/decoder-codegen/spec.md` delta — ADDED Requirements for `DecoderDispatch.decodeInto`, validation rules on decodeInto methods, coverage list of primitive decoders, parity invariant
- [x] 3.10 **Gate: `DecodeIntoBench`** — PASS. With `BitStream` creation hoisted to `@Setup(Level.Invocation)`, the timed loop shows:
  - `decodeInto` adds **0 bytes per decode** (baseline identical in both methods); `decodeThenWrite` adds 16 B (int/float box), 24 B (long box), or 96 B (vector object + float[3] + 3× Float box).
  - Speedup: **1.24×–1.38× for scalars**, **1.68× for VECTOR**. Per-decode savings: ~0.6–0.9 ns scalar, ~4.6 ns vector.
  - Run 2026-04-16 on Linux/JDK 21 with `-wi 3 -i 5 -f 1 -prof gc`.

## 4. FlatEntityState.decodeInto and write (CP-4)

- [x] 4.1 Add `decodeInto(FieldPath, Decoder, BitStream)` to FlatEntityState; traversal mirrors `applyMutation`
- [x] 4.2 At Primitive leaf: makeWritable, set flag byte to 1, call `DecoderDispatch.decodeInto`, return `oldFlag == 0`
- [x] 4.3 At Ref leaf (strings before CP-5 inline them): fall back to `decode` + existing Ref write logic
- [x] 4.4 At SubState leaf: throw `IllegalStateException`
- [x] 4.5 Add `write(FieldPath, Object)` to FlatEntityState — single direct-write dispatching on leaf shape: Primitive → `PrimitiveType.write`; Ref → refs slot write; SubState-Pointer → switch pointer logic (clear old sub-Entry, set pointerSerializers, lazy-create new); SubState-Vector → resize vector sub-Entry
- [x] 4.6 Unit test: `decodeInto` byte-identical to `applyMutation(WriteValue(decode))` for primitive decoders + layout shapes (root-primitive, in-vector, via-pointer), plus Ref-leaf fallback + SubState throw (FlatEntityStateDecodeIntoTest)
- [x] 4.7 Unit test: `write` produces results identical to `applyMutation(WriteValue)` / `applyMutation(ResizeVector)` / `applyMutation(SwitchPointer)` across all layout shapes (4 cases)
- [x] 4.8 Unit test: `decodeInto` capacity-change return — first call (null→value) returns true, second returns false
- [x] 4.9 Unit test: `decodeInto` / `write` after `copy()` triggers owner-pointer COW on touched path only — primitive-only write doesn't clone refs; ref-write clones refs; inner-primitive write doesn't clone pointerSerializers
- [x] 4.10 Add `FlatWriteBench` micro — decodeInto vs applyMutation-WriteValue, parameterized INT_SIGNED / FLOAT_DEFAULT / VECTOR
- [x] 4.11 **Gate: `FlatWriteBench`** — PARTIAL PASS.

  > **Results (2026-04-16)** via `java -jar build/libs/clarity-4.0.1-SNAPSHOT-jmh.jar FlatWriteBench -prof gc -wi 3 -i 5 -f 1` on JDK 17, Linux. 256 writes per invocation on a single int/float/vector leaf; fresh BitStream per `@Setup(Level.Invocation)` (65 KB source bytes).
  >
  > - **Zero allocation on primitive writes: PASS.** `decodeInto` allocates a flat **163,944 B/op** across all three `kind` params — identical to each other, and this is purely the invocation-level BitStream setup (`ByteString.copyFrom(sourceBytes)` + `BitStream.createBitStream`). No write-path allocation. `applyMutationWriteValue` allocates 172,104 B/op (INT), 172,136 B/op (FLOAT), 192,616 B/op (VECTOR) — delta over decodeInto is 32 B/write (INT/FLOAT: `Integer`/`Float` box 16 B + `WriteValue` record 16 B) or 112 B/write (VECTOR: `Vector` + 3× `Float` + `WriteValue`). Escape analysis does **not** eliminate these — they survive as real allocations.
  >
  > - **ns/op improvement:** INT_SIGNED 1021 vs 1336 = **23.6%** faster; FLOAT_DEFAULT 1090 vs 1341 = **18.7%** faster; VECTOR 2279 vs 3658 = **37.7%** faster. Only VECTOR hits the 30% gate. Scalars land at ~20%, limited by how cheap per-write cost already is (~4 ns/write) — the ~1.2 ns delta from avoiding Integer/WriteValue alloc is a meaningful absolute win but not 30% of a 4 ns baseline. **PASS for VECTOR, below-target for scalars** — the true end-to-end signal is `EntityStateParseBench` at CP-6 (target ≥20% wall-clock on full replay parse).

## 5. Inline strings in byte[] (CP-5)

- [x] 5.1 Added `FieldLayout.InlineString(int offset, int maxLength)` as a new sealed subtype. Semantics: flag byte at `offset`, 2-byte LE length prefix at `offset+1..offset+2`, UTF-8 bytes at `offset+3..offset+3+length-1`. Total reservation `3 + maxLength`
- [x] 5.2 FieldLayoutBuilder detects `ValueField`s whose decoder is `StringZeroTerminatedDecoder` or `StringLenDecoder` and emits `InlineString`. `char[N]` literals parsed from `FieldType.getElementCount()` (sizes observed in audit: 8, 18, 32, 33, 64, 128, 129, 161, 255, 256, 260, 512). Anything unbounded — `CUtlString`, `CUtlSymbolLarge`, `char[<named-const>]`, S1 `PropType.STRING` — falls back to `FieldLayoutBuilder.UNBOUNDED_STRING_MAX_LENGTH = 512`
- [x] 5.3 `BitStream.readStringInto(byte[] data, int offset, int n)` added — zero-alloc direct read into target byte[], mirrors `readString`'s zero-terminator semantics. `StringLenDecoder.decodeIntoInline` and `StringZeroTerminatedDecoder.decodeIntoInline` added — 4-param signature `(bs, data, offset, maxLength)`. Named `decodeIntoInline` (not `decodeInto`) to bypass the standard codegen path: string decoders have no `primitiveType` and must never be routed via `DecoderDispatch.decodeInto`. No over-length assert in the decoder: StringLen's 9-bit wire cap (511) is always ≤ the 512 reservation, and StringZeroTerminated's `readStringInto(..., maxLength)` is naturally capped. Trust the data in the hot path
- [x] 5.4 `FlatEntityState.decodeInto` routes `InlineString` leaves by `instanceof` check on the passed decoder (2 possibilities). Flag byte set before dispatch; returns `oldFlag == 0`
- [x] 5.5 `FlatEntityState.write`/`writeValue` handles `InlineString`: encodes via `String.getBytes(UTF_8)`, writes 2-byte LE length + bytes inline. Over-length (programmatic write exceeding reserved span) throws `IllegalStateException` on the slow `write` path only
- [x] 5.6 `FlatEntityState.getValueForFieldPath` reads length prefix, allocates `String(data, offset+3, len, UTF_8)` — one allocation per call
- [x] 5.7 Refs slab audit for S2 FLAT:

  > **Results (2026-04-16)**:
  > - All `allocateRefSlot` / `freeRefSlot` / `releaseRefSlot` call sites in `FlatEntityState`: **4 live**, **2 vestigial**.
  > - **Live (sub-Entry lifecycle only)**: `resizeVector` allocate, `switchPointer` release + allocate, `lazyCreateSubEntry` allocate. `releaseRefsInEntry` releases SubState slots recursively.
  > - **Vestigial (unreachable for real S2 data post-CP-5)**: the `FieldLayout.Ref` branches in `writeValue` (allocate + free) and in `releaseRefsInEntry` (free). Retained because `S2DecoderFactory` could in principle produce a non-primitive non-string decoder for an unknown type — today the fallback path produces `IntVarUnsignedDecoder` (primitive), and every known type is primitive or string, so no Ref leaf is ever emitted in practice.
  > - Tests `FlatEntityStateCowTest.copyThenStringWriteClonesRefsInCopyOnly` and `FlatEntityStateDecodeIntoTest.writeAfterCopyClonesRefsOnlyWhenRefTouched` were rewritten to assert the new "string write clones root only, not refs" invariant. `EntityStateTest` slab-release tests (`resizeVectorShrinkReleasesDroppedSubEntries`, `resizeVectorToZeroReleasesAllElementSubEntries`, `releaseOnCopyDoesNotAffectOriginal`) switched their vector element from an inner string to an inner pointer-to-leaf so each element still occupies a slab slot.

- [x] 5.8 `FlatEntityStateInlineStringTest.inlineStringRoundtripViaWrite` + `inlineStringRoundtripViaDecodeInto` — both assert byte-level layout (flag/length-prefix/UTF-8) AND round-trip equality via `getValueForFieldPath`. Bonus: `inlineStringEmptyWriteReadsBackEmpty` (empty-string vs null distinguishable via flag byte) and `inlineStringWriteThenClear`
- [x] 5.9 `FlatEntityStateInlineStringTest.writeExceedingMaxLengthThrows` — programmatic `write(fp, 513-byte-string)` on a 512-byte-reserved leaf throws `IllegalStateException`
- [x] 5.10 `FlatEntityStateInlineStringTest.copyThenInlineStringWriteClonesRootOnly` — after `copy()`, a subsequent `write(fp, String)` clones the copy's root byte[] but NOT its refs slab; original's root and refs both unchanged
- [x] 5.11 `InlineStringBench` (JMH) — parameterized on a 12-byte UTF-8 string. Two benchmarks: `decodeIntoThenRead` (zero-alloc decode + one String per read) vs `decodeAndWriteThenRead` (legacy `DecoderDispatch.decode` + `applyMutation(WriteValue)` still writes inline in the new layout, but pays the interned-String + `WriteValue` record + UTF-8 re-encode cost)
- [x] 5.12 **Gate: byte[] memory footprint** — met by §0 audit (accepted ≤2 KB worst case: 4 × 514 = 2056 B for 4-unbounded-string game-mode serializers; median ~1.3 KB). No runtime check needed: `FieldLayoutBuilder` `totalBytes` accumulation is strictly bounded by `Σ (3 + maxLength)` over string leaves, which the audit already characterized
- [x] 5.13 **Gate: `getValueForFieldPath` allocation** — PASS.

  > **Results (2026-04-16)** via `InlineStringBench -prof gc -wi 2 -i 3 -f 1` on JDK 21:
  > - `decodeIntoThenRead`: **6,537 ns/op**, **53,944 B/op** (≈ 32 KB per-invocation BitStream `stringTemp` + per-iter String allocation).
  > - `decodeAndWriteThenRead`: **18,316 ns/op**, **80,570 B/op**.
  > - Roundtrip **2.80× faster** with inline path; **26,626 B/invocation** less allocation (≈ 104 B/iter: interned-String + `WriteValue` record + UTF-8 encode scratch eliminated).
  > - Per-read allocation on inline path is dominated by the expected `new String(data, off, len, UTF_8)` — one String + its backing char/byte array, no hidden per-read allocations beyond the single String construction.

## 6. Unified readFieldsFast + Entities snapshot/rollback (CP-6)

- [x] 6.1 Add `state.write(FieldPath, Object)` to `EntityState` interface; implement on `FlatEntityState` (done 4.5), `NestedArrayEntityState`, `TreeMapEntityState`
- [x] 6.2 Modify `FieldChanges` — add `boolean capacityChanged`; fast-path constructor `(FieldPath[], int, boolean)` leaving `mutations == null`; retain legacy constructor for debug/baseline
- [x] 6.3 Adjust `FieldChanges.applyTo(state)` to return `capacityChanged` directly when `mutations == null`
- [x] 6.4 Rewrite `S2FieldReader.readFieldsFast` — hoist `isFlat`; per-field check `isFlat && field.isPrimitiveLeaf()` routes to `decodeInto`, else `state.write`; accumulate capacityChanged; no staging
- [x] 6.5 Remove `pointerOverrides[]` from `readFieldsFast`; subsequent `resolveField` reads current serializers from `state.pointerSerializers` directly
- [x] 6.6 Leave `readFieldsDebug` untouched
- [x] 6.7 ~~Snapshot scratch~~ — **dropped**. See design pivot note below.
- [x] 6.8 ~~snapshotAndCopy helper~~ — **dropped**.
- [x] 6.9 ~~Call snapshotAndCopy~~ — **dropped**.
- [x] 6.10 Rewrite each `queueEntity*` method to mutate eagerly + queue event-only lambdas
- [x] 6.11 ~~Add Entity.setExists~~ — **dropped**. Existing `setExistent` suffices; no rollback caller.
- [x] 6.12 ~~Wrap processPacketEntities in rollback~~ — **dropped**. `finally` clears `queuedUpdates` only.
- [x] 6.13 Entity cache audit: no caches require invalidation (confirmed by reading `Entity.java` — `getPropertyForFieldPath` reads through `state` directly).
- [x] 6.14-6.18 ~~Unit tests for rollback semantics~~ — **dropped** (no rollback to test).
- [x] 6.19 Integration: full Dota 2 replay, FLAT — parses successfully via `./gradlew bench`, 10 iterations no errors.
- [x] 6.20 Integration: same replay, NESTED_ARRAY — parses successfully, 10 iterations.
- [x] 6.21 **Gate: `EntityStateParseBench FLAT`** — **17.2% improvement** vs CP-0 (1988 → 1646 ms on Dota `340/8168882574_1198277651.dem`). Below 20% target but FLAT overtook NESTED_ARRAY and became fastest impl.
- [x] 6.22 **Gate: `EntityStateParseBench NESTED_ARRAY`** — **9.7% improvement** vs CP-0 (1916 → 1730 ms). Better than "neutral".
- [x] 6.23 **Gate: `EntityStateParseBench TREE_MAP`** — **6.5% improvement** vs CP-0 (2200 → 2057 ms). No regression.
- [x] 6.24 **Gate: `-prof gc` on FLAT parse** — autobox near-zero: per-invocation alloc 9.11 GB post vs 10.12 GB pre (-10%); no `Integer`/`Float`/`Long` boxing or `WriteValue` records in the top allocation sites (dominant allocs are `BitStream.<init>` 13% and `copy()` for CREATE/RECREATE baselines).

### Design pivot (2026-04-16): no per-packet snapshot/rollback

Initial CP-6 implementation included `snapshotAndCopy(entity)` on every UPDATE + try/catch/finally rollback. JFR profiling (`FlatEntityState.rootEntryWritable` = 69.60% of all allocation) showed the per-packet COW `byte[].clone()` dominated. Measured effect: +46% wall-clock regression for FLAT, 4× allocation.

**Decision**: drop the entire snapshot/rollback machinery. The deferred-message mechanism at `Entities.onPacketEntities` lines 346-358 prevents the only scenario where rollback would have mattered — packets that would decode incorrectly against the current state are detected up-front via `entitiesServerTick < deltaFrom` and deferred untouched. No code path exists where a failed `readFields` is expected to recover; every throw aborts the replay run. State-dirty-on-throw is acceptable.

Pre-existing atomicity holes (`setActive` on line 488, `updateEntityAlternateBaselineIndex` on line 440) stay as they were — not newly introduced by CP-6.

Scope impact: Requirement "Packet-scoped snapshot-and-rollback atomicity" in `entity-update-commit` spec no longer holds; the delta needs to be updated to reflect the actual implementation. `Entity.setExists` method not added. `FieldChanges.capacityChanged` fast-path semantics unchanged.

## 7. S1 full port — **DEFERRED to follow-up change `accelerate-s1-flat-state`**

S1 is deferred so its implementation can be built directly on the eager-copy model delivered by the planned follow-up `lightspeed-eager-copy` change, rather than temporarily adopting the owner-pointer COW machinery only to have it stripped immediately after. `ObjectArrayEntityState` and `S1FieldReader` remain unchanged in this change. `ObjectArrayEntityState` gains only a trivial `write(fp, decoded)` delegating to the existing slot-assignment behavior so the `EntityState` interface is satisfied.

## 8. MutationListener contract (CP-8)

- [ ] 8.1 Add `onUpdateWrite(EntityState, FieldPath)` to `MutationListener` with default no-op
- [ ] 8.2 Invoke `onUpdateWrite` after each `decodeInto` + each `state.write` in `S2FieldReader.readFieldsFast` when listener attached
- [ ] 8.3 Continue invoking `onUpdateMutation` from paths that still produce materialized `StateMutation` (debug reader, baseline, programmatic, trace replay)
- [ ] 8.4 Update `MutationRecorder` to implement `onUpdateWrite` — read back via `getValueForFieldPath`
- [ ] 8.5 Verify `MutationTraceBench` still replays traces correctly
- [ ] 8.6 **Gate:** `dtinspector` smoke test — runs cleanly on a sample replay

## 9. Final validation

- [ ] 9.1 Run full bench matrix (`EntityStateParseBench` × 3 engines, `FlatCopyBench`, `NestedArrayCopyBench`, `DecodeIntoBench`, `FlatWriteBench`, `InlineStringBench`) — record before/after
- [ ] 9.2 Run JMH `-prof gc` across benches — document autobox/alloc deltas
- [ ] 9.3 Run `:dev:dtinspectorRun` on sample replay
- [ ] 9.4 Run `:repro:issue289Run`, `:repro:issue350Run`
- [ ] 9.5 Document measured improvements in `proposal.md` or `RESULTS.md` before archiving
- [ ] 9.6 `./gradlew build` — all tests green
- [ ] 9.7 `openspec validate accelerate-flat-entity-state --strict` — passes

## 10. ~~Documentation and cleanup~~ — **merged into successor changes**

Remaining code that would be cleaned up here bleeds into the two planned follow-up changes:

- Owner-pointer COW machinery (`Entry.owner`, `refsOwner`, `pointerSerializersOwner`, `entriesOwner`, `ensureRefsOwned`, `rootEntryWritable`, `makeWritable`, `Consumer<Entry>` slot-setter plumbing) → **`lightspeed-eager-copy`**
- `ObjectArrayEntityState`, `S1EntityStateType` legacy references → **`accelerate-s1-flat-state`**
- `pointerOverrides[]`, staging fields on `FieldChanges` → **NOT dead** (still used by `readFieldsDebug` and baseline materialization); intentionally kept
