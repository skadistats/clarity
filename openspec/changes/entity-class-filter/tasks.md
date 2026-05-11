## Chunking / commit cadence

This change is multi-session. Land in these chunks — each leaves the tree in a sane state and is independently committable:

| Chunk | Groups | What lands | Why it's a stop point |
|---|---|---|---|
| **A. Foundations** | 1, 2 | Test utilities (`BitstreamBuilder`, `DecoderTestBase`) + BitStream `skipVarUInt`/`skipVarULong` | No API surface, no behavior change |
| **B. Decoder skip methods** | 3 | ~30 static `skip` methods + curated branch-coverage tests | Build enforcement still OFF (4.2 not landed), so missing skips don't break the build. Skip methods exist but nothing dispatches to them yet. Largest chunk by volume — subdivide by category (3.1, 3.2, …) if smaller landings are preferred |
| **C. Dispatch + enforcement** | 4 | Generator emits `DecoderDispatch.skip`; annotation processor fails build if any decoder is missing `skip` | One-way firewall: after this, every decoder MUST have skip forever |
| **D. FieldReader + verifier** | 5, 6 | `FieldReader.skipFields` abstract + S2/S1 impls; `-Dclarity.test.skipParity=true` verifier | Skip path is fully callable but no consumer-visible API; parser still full-decodes |
| **E. Consumer wiring** | 7, 8 | `BitSet skippedIds`, Entities CREATE/UPDATE/DELETE handling, reset clear, `withEntityFilter` on runner | Feature works end-to-end |
| **F. Verification** | 9, 10 | End-to-end reject-everything parity test (with and without verifier flag), then bench measurement | Pure proof, no implementation |

## 1. Test infrastructure (depth, built first)

- [ ] 1.1 Create `BitstreamBuilder` test utility in `src/test/java/skadistats/clarity/io/decoder/` — declarative wire-format builder (`bitstream().skip(n).add(value, bits).addVarUInt(n).addStringZ(s).build()`)
- [ ] 1.2 Create `DecoderTestBase` with shared helpers: `bitstream(...)`, `assertSkipParity(decoder, bs)` (decode + rewind + skip, compare pos deltas)
- [ ] 1.3 Add a single smoke test using the builder to verify byte-aligned and bit-misaligned construction round-trip

## 2. BitStream skip-varint primitive

- [ ] 2.1 Add `BitStream.skipVarUInt()` — SWAR peek-64 + NTZ + pos update (no value extraction, no Long.compress)
- [ ] 2.2 Add `BitStream.skipVarULong()` — same shape, with slow-path fallback for the 9–10 byte case
- [ ] 2.3 Unit-test both helpers with 1-byte / multi-byte / max-byte patterns at byte-aligned and bit-misaligned start positions, comparing against `readVarUInt`/`readVarULong` pos delta

## 3. Per-decoder skip methods (category-by-category)

- [ ] 3.1 Category 1 (bit-count) — add `skip` static to Bool, IntUnsigned, IntSigned, LongUnsigned, LongSigned, FloatNoScale, FloatDefault, FloatCellCoord, FloatQuantized, FixedPointer
- [ ] 3.2 Category 2 (length-then-skip) — add `skip` static to StringLen, CUtlBinaryBlock
- [ ] 3.3 Category 3 (recursive composite) — add `skip` static to Vector, VectorXY, VectorNormal, VectorDefault, Array (delegates to component's `skip`)
- [ ] 3.4 Category 4 (conditional) — add `skip` static to PolymorphicPointer, FloatCoord, FloatCoordMp, FloatNormal, QAngleNoBitCount, QAnglePitchYawOnly (read flag bits, conditionally `bs.skip(n)` per flag)
- [ ] 3.5 Category 5 (walking) — add `skip` static to IntVarUnsigned, IntVarSigned, IntMinusOne, LongVarUnsigned, LongVarSigned (use `skipVarUInt`/`skipVarULong`), StringZeroTerminated (`while (bs.readUBitInt(8) != 0) {}`)
- [ ] 3.6 Per-decoder branch-coverage tests — one test file per concrete decoder (`<Name>DecoderTest`), exercising every internal branch per the design's coverage axes table; each test asserts both decode value AND skip parity

## 4. Decoder dispatch generator

- [ ] 4.1 Extend `@RegisterDecoder` annotation processor to emit `DecoderDispatch.skip(BitStream, Decoder)` as int-tableswitch on `decoder.id`, mirroring existing `decode` / `decodeInto`
- [ ] 4.2 Build-time enforcement — annotation processor SHALL fail the build if any `@RegisterDecoder` class is missing a static `skip` method (or a `decode`)
- [ ] 4.3 Default-case behavior — generated `skip` throws `IllegalArgumentException` on unknown id, matching `decode`

## 5. FieldReader skip plumbing

- [ ] 5.1 Add abstract `skipFields(BitStream, DTClass)` to the `FieldReader` interface (no `EntityState` param, no default impl)
- [ ] 5.2 Implement `S2FieldReader.skipFields` — stripped `readFieldsFast`: walks FieldOps, resolves the decoder per field, calls `DecoderDispatch.skip`
- [ ] 5.3 Implement `S1FieldReader.skipFields` in the abstract base — uses the existing engine-specific `readIndices` plus `DecoderDispatch.skip` per index; CsgoFieldReader and DotaS1FieldReader inherit

## 6. Test-only runtime parity verifier

- [ ] 6.1 Add `-Dclarity.test.skipParity=true` flag handling
- [ ] 6.2 Implement verifying wrapper around `DecoderDispatch.decode`: record pos, decode-record-pos, rewind, skip-record-pos, assert equal, rewind, decode-for-real
- [ ] 6.3 Wire flag into the test classpath only (no production cost when disabled)

## 7. Entities processor changes

- [ ] 7.1 Add `BitSet skippedIds` field; clear in `@OnReset` CLEAR phase alongside `entities` / `baselineRegistry` / `deferredMessages`
- [ ] 7.2 On entity CREATE: consult the runner's filter (if set); on reject, mark `skippedIds.set(eIdx)`, skip-decode the CREATE body via `skipFields`, do NOT materialize an `Entity`, do NOT fire create/enter events, do NOT call `baselineRegistry.updateEntityBaseline`
- [ ] 7.3 On entity UPDATE: if `skippedIds.get(eIdx)`, call `FieldReader.skipFields` and discard; do NOT fire updated/property events
- [ ] 7.4 On entity DELETE: clear the bit (`skippedIds.clear(eIdx)`); do NOT fire deleted event for filtered ids
- [ ] 7.5 Verify `entities.getById(skippedId)` returns `null` and collection-walking APIs (`getByPredicate`) exclude filtered ids — should fall out of "no Entity created" but assert it

## 8. Runner API

- [ ] 8.1 Add `withEntityFilter(Predicate<DTClass>)` to `AbstractFileRunner` (and any sibling runner used by the public API)
- [ ] 8.2 Pipe the filter into the Entities processor at processor-init time
- [ ] 8.3 Throw `IllegalStateException` if `withEntityFilter` is called after parse has started
- [ ] 8.4 Filter exceptions: do NOT catch — let them propagate and terminate the parse

## 9. End-to-end parity test

- [ ] 9.1 Add reject-everything parity test in `src/test/java/skadistats/clarity/processor/entities/`: parse with no filter, parse with `dt -> false`, assert bitstream pos sequence identical at every `CSVCMsg_PacketEntities` boundary
- [ ] 9.2 Run against bench corpus replays: `dota/s2/normal/1560289528.dem`, `dota/s1/normal/271145478.dem` (mandatory); optionally `csgo/s2/issue-345/liquid-vs-betboom-m1-mirage.dem` and `deadlock/newer/19206063.dem`
- [ ] 9.3 Run the same test with `-Dclarity.test.skipParity=true` to exercise the Layer 3 runtime verifier on the real-replay input distribution

## 10. Bench observation

- [ ] 10.1 Run the bench harness with a representative filter (e.g., player + gamerules) against the pinned replays; record observed speedup vs no-filter baseline
- [ ] 10.2 Report numbers — no acceptance threshold; document the observed delta and any deviation from the design's paper estimate
