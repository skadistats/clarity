## Chunking / commit cadence

This change is multi-session. Land in these chunks — each leaves the tree in a sane state and is independently committable:

| Chunk | Groups | What lands | Why it's a stop point |
|---|---|---|---|
| **A. Foundations** | 1, 2 | Test utilities (`BitstreamBuilder`, `DecoderTestBase`) + BitStream `skipVarUInt`/`skipVarULong` | No API surface, no behavior change |
| **B. Decoder skip methods** | 3 | ~30 static `skip` methods + curated branch-coverage tests | Build enforcement still OFF (4.2 not landed), so missing skips don't break the build. Skip methods exist but nothing dispatches to them yet. Largest chunk by volume — subdivide by category (3.1, 3.2, …) if smaller landings are preferred |
| **C. Dispatch + enforcement** | 4 | Generator emits `DecoderDispatch.skip`; annotation processor fails build if any decoder is missing `skip` | One-way firewall: after this, every decoder MUST have skip forever |
| **D. FieldReader** | 5 | `FieldReader.skipFields` abstract + S2/S1 impls | Skip path is fully callable but no consumer-visible API; parser still full-decodes |
| **E. Consumer wiring** | 6, 7 | `BitSet skippedIds`, Entities CREATE/UPDATE/DELETE handling, reset clear, `withEntityFilter` on runner | Feature works end-to-end |
| **F. Verification** | 8, 9 | End-to-end reject-everything parity test, then bench measurement | Pure proof, no implementation |

## 1. Test infrastructure (depth, built first)

- [x] 1.1 Create `BitstreamBuilder` test utility in `src/test/java/skadistats/clarity/io/decoder/` — declarative wire-format builder (`bitstream().skip(n).add(value, bits).addVarUInt(n).addStringZ(s).build()`)
- [x] 1.2 Create `DecoderTestBase` scaffold with `bitstream()` helper. `assertSkipParity` deferred to chunk B (depends on per-decoder skip methods)
- [x] 1.3 Add a single smoke test using the builder to verify byte-aligned and bit-misaligned construction round-trip

## 2. BitStream skip-varint primitive

- [x] 2.1 Add `BitStream.skipVarUInt()` — SWAR peek-64 + NTZ + pos update (no value extraction, no Long.compress)
- [x] 2.2 Add `BitStream.skipVarULong()` — same shape; 9/10-byte tail uses `peekBit` + arithmetic (no `read*` calls)
- [x] 2.3 Unit-test both helpers with 1-byte / multi-byte / max-byte patterns at byte-aligned and bit-misaligned start positions, comparing against `readVarUInt`/`readVarULong` pos delta

## 3. Per-decoder skip methods (category-by-category)

- [x] 3.1 Category 1 (bit-count) — Bool, FixedPointer, IntSigned/Unsigned, LongSigned/Unsigned, FloatNoScale, FloatDefault, FloatCellCoord, FloatNormal, QAngleBitCount, QAngleNoScale, QAnglePitchYawOnly. (Note: design's category 1 list misclassified FloatQuantized — actually cat 4; FloatNormal and QAnglePitchYawOnly — actually cat 1.)
- [x] 3.2 Category 2 (length-then-skip) — CUtlBinaryBlock; StringLen uses readUBitInt(9) prefix (not readVarUInt as design described) + `skip(n*8)`
- [x] 3.3 Category 3 (recursive composite) — Vector, VectorXY, VectorNormal, VectorDefault, Array (use generated `DecoderDispatch.skip` for components)
- [x] 3.4 Category 4 (conditional) — PolymorphicPointer, FloatCoord, FloatCoordMp, FloatQuantized, QAngleNoBitCount, QAnglePrecise. Read methods that have skip logic (readBitCoord, readCellCoord, readCoordMp, readBitNormal, read3BitNormal, readString, readUBitVar) have paired `skip*` helpers on `BitStream`.
- [x] 3.5 Category 5 (walking) — IntVar*, LongVar*, IntMinusOne use new `BitStream.skipVarUInt/skipVarULong`; StringZeroTerminated uses `BitStream.skipString`. PolymorphicPointer's UBitVar tail uses `BitStream.skipUBitVar`.
- [x] 3.6 Skip-parity tests — single `DecoderSkipParityTest` file with @Test per decoder; random-byte parity sweeps with bit-misaligned starts (0/1/3/7/13/31/63/65). Curated `BitstreamBuilder` inputs for decoders where random bytes produce pathological cases (StringLen, CUtlBinaryBlock, PolymorphicPointer).

> Side benefit of chunk B: extended the annotation processor (`DecoderAnnotationProcessor`) to emit `DecoderDispatch.skip` alongside `decode`/`decodeInto`. Composite skips need dispatch to component skip, so this lands here rather than in chunk C. Build-time enforcement (fail if a decoder lacks `skip`) is still deferred to chunk C.

## 4. Decoder dispatch generator

- [x] 4.1 Extend `@RegisterDecoder` annotation processor to emit `DecoderDispatch.skip(BitStream, Decoder)` as int-tableswitch on `decoder.id`, mirroring existing `decode` / `decodeInto`
- [x] 4.2 Build-time enforcement — annotation processor SHALL fail the build if any `@RegisterDecoder` class is missing a static `skip` method (or a `decode`)
- [x] 4.3 Default-case behavior — generated `skip` throws `IllegalArgumentException` on unknown id, matching `decode`

## 5. FieldReader skip plumbing

- [x] 5.1 Add abstract `skipFields(BitStream, DTClass)` to the `FieldReader` interface (no `EntityState` param, no default impl)
- [x] 5.2 Implement `S2FieldReader.skipFields` — stripped `readFieldsFast`: walks FieldOps, resolves the decoder per field, calls `DecoderDispatch.skip`
- [x] 5.3 Implement `S1FieldReader.skipFields` in the abstract base — uses the existing engine-specific `readIndices` plus `DecoderDispatch.skip` per index; CsgoFieldReader and DotaS1FieldReader inherit

## 6. Entities processor changes

- [ ] 6.1 Add `BitSet skippedIds` field; clear in `@OnReset` CLEAR phase alongside `entities` / `baselineRegistry` / `deferredMessages`
- [ ] 6.2 On entity CREATE: consult the runner's filter (if set); on reject, mark `skippedIds.set(eIdx)`, skip-decode the CREATE body via `skipFields`, do NOT materialize an `Entity`, do NOT fire create/enter events, do NOT call `baselineRegistry.updateEntityBaseline`
- [ ] 6.3 On entity UPDATE: if `skippedIds.get(eIdx)`, call `FieldReader.skipFields` and discard; do NOT fire updated/property events
- [ ] 6.4 On entity DELETE: clear the bit (`skippedIds.clear(eIdx)`); do NOT fire deleted event for filtered ids
- [ ] 6.5 Verify `entities.getById(skippedId)` returns `null` and collection-walking APIs (`getByPredicate`) exclude filtered ids — should fall out of "no Entity created" but assert it

## 7. Runner API

- [ ] 7.1 Add `withEntityFilter(Predicate<DTClass>)` to `AbstractFileRunner` (and any sibling runner used by the public API)
- [ ] 7.2 Pipe the filter into the Entities processor at processor-init time
- [ ] 7.3 Throw `IllegalStateException` if `withEntityFilter` is called after parse has started
- [ ] 7.4 Filter exceptions: do NOT catch — let them propagate and terminate the parse

## 8. End-to-end parity test

- [ ] 8.1 Add reject-everything parity test in `src/test/java/skadistats/clarity/processor/entities/`: parse with no filter, parse with `dt -> false`, assert bitstream pos sequence identical at every `CSVCMsg_PacketEntities` boundary
- [ ] 8.2 Run against bench corpus replays: `dota/s2/normal/1560289528.dem`, `dota/s1/normal/271145478.dem` (mandatory); optionally `csgo/s2/issue-345/liquid-vs-betboom-m1-mirage.dem` and `deadlock/newer/19206063.dem`

## 9. Bench observation

- [ ] 9.1 Run the bench harness with a representative filter (e.g., player + gamerules) against the pinned replays; record observed speedup vs no-filter baseline
- [ ] 9.2 Report numbers — no acceptance threshold; document the observed delta and any deviation from the design's paper estimate
