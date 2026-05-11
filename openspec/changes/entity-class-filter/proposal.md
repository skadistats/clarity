## Why

Profile evidence from 2026-05-11 confirms the parser is branch-prediction bound on inherently-unpredictable wire-format input: IPC 2.11, L1 hit rate ~97%, branch-miss rate 2.29% → roughly 13–18% of cycles spent on pipeline flushes in code where every branch outcome is a fresh function of the wire stream (FieldOp dispatch, FieldLayout typeSwitch, varint termination, Huffman tail walks). Micro-optimization has plateaued — a SWAR varint rewrite this session moved the needle ~1% on one replay and was flat on another. The existing data-structure work (flat byte buffer state, int-tableswitch Decoder dispatch, bulk-strided bit reads) has taken the easy and medium wins.

The next lever isn't micro-opt — it's doing less work. A typical consumer cares about a small fraction of transmitted entity classes (e.g., players + gamerules + a few projectile types out of dozens). The wire-format cost of decoding the ignored classes — bitstream reads, FieldOp Huffman, polymorphic field-layout dispatch, primitive allocation, state writes, change dispatch — is paid every tick whether anyone consumes it or not. For workloads that care about ~20% of entity classes, skipping the rest could halve total parse cost. That dwarfs anything micro-optimization can recover and is the right shape for the next significant performance change.

This proposal introduces an opt-in per-class entity filter. Filtered entities never enter the entity collection (no Java-side representation, no state, no dispatch). Wire-stream bytes for their CREATEs and UPDATEs are still consumed (the bit-aligned, variable-width format leaves no choice), but every downstream cost — allocation, state mutation, property dispatch — is eliminated.

## What Changes

- **New: per-decoder `skip(BitStream[, decoder])` static method on every concrete `Decoder` subclass.** Consumes the same bit count as `decode` but allocates nothing and writes nothing. The implementation strategy SHALL match the decoder's structural category — five categories cover the codebase; see `design.md` for the full mapping:

  - **bit-count skip** (`bs.skip(n)`) for fixed-width primitives (Bool, IntUnsigned, FloatNoScale, FloatQuantized, …).
  - **length-then-skip** (`var n = bs.readVarUInt(); bs.skip(n * 8);`) for length-prefixed blobs (StringLen, CUtlBinaryBlock). Avoids the byte-copy of `decode`.
  - **recursive composite skip** (call component decoders' `skip` N times) for Array, Vector, VectorXY, VectorNormal, VectorDefault. Avoids per-component result wrappers.
  - **conditional skip** (read flag bits, then `bs.skip(n)` or no-op per flag) for PolymorphicPointer, FloatCoord, FloatCoordMp, FloatNormal, QAngleNoBitCount, QAnglePitchYawOnly. Avoids float arithmetic and wrapper allocations.
  - **walking skip** (read bit-by-bit or byte-by-byte until a data-dependent terminator) for varint family and zero-terminated strings. Cheaper than `decode` only by the cost of the value extraction; varint walking benefits from a new SWAR-based BitStream helper (see below).

- **New: generated `DecoderDispatch.skip(BitStream, Decoder)`.** Mirrors the existing `DecoderDispatch.decode` / `DecoderDispatch.decodeInto` int-tableswitch on `decoder.id`. Emitted by the same `@RegisterDecoder` annotation processor.

- **New: `BitStream.skipVarUInt()` and `BitStream.skipVarULong()`.** SWAR-based varint position update: peek 64 bits, find first byte without the continuation bit via `~word & 0x80…80` + `numberOfTrailingZeros`, advance `pos` by `(bytes * 8)`. No value extraction, no `Long.compress`, no masking. Earlier SWAR experiments on `readVarUInt` were flat because `Long.compress` cost on Zen 5 negated the savings; `skipVarUInt` does none of that work, so the SWAR is a genuine win for the walking-skip varint decoders (IntVarUnsigned, IntVarSigned, IntMinusOne, LongVarUnsigned, LongVarSigned, plus the length prefix in CUtlBinaryBlock and StringLen).

- **New: `FieldReader.skipFields(BitStream, DTClass, EntityState)`.** Concrete implementations on both S2 and S1 field readers:
  - **S2:** structurally a stripped `readFieldsFast` — walks FieldOps + resolves the decoder per field, then calls `DecoderDispatch.skip` instead of `decode`.
  - **S1:** reuses the existing `readIndices` (which is engine-subclass-specific — CSGO vs DotaS1) to populate the changed-prop-index list, then calls `DecoderDispatch.skip` per index. S1 has no FieldOp Huffman walk, so its skip path is structurally cheaper than its decode path by an even larger margin than S2's.

  No `FieldChanges` returned (no state mutated).

- **New: `AbstractFileRunner.withEntityFilter(Predicate<DTClass>)`.** Pre-parse setter; filter is immutable after parse starts. Default behavior with no filter set is unchanged.

- **`Entities` processor changes (additive):** consult the filter on CREATE; if rejected, mark the entity id in a `BitSet` of skipped ids and do not create a Java-side `Entity`. On UPDATE: if the id is marked skipped, call `FieldReader.skipFields` and discard. On DELETE: clear the bit; no consumer-side dispatch.

- **New: decoder test infrastructure.** The skip-parity invariant is load-bearing — bit drift corrupts the rest of the entity stream — and the existing decoder test coverage is sparse (only `DecoderDecodeIntoParityTest` and `S2DecoderFactoryTest`, no per-decoder unit tests, no FieldReader-level tests). The test investment for this change concentrates at the decoder boundary on inputs that *can actually occur in real wire data* — decoders only ever see Valve's encoding, so robustness-testing against arbitrary random bytes is not in scope. If a corrupted byte stream causes a decoder to crash, that's acceptable behavior; no real replay produces that input.

  - **Per-decoder branch coverage (curated valid inputs).** For every concrete decoder, exercise every internal branch / code path with hand-crafted bit patterns that represent inputs Valve's encoder could actually produce. Drives off a `BitstreamBuilder` test utility (~50–100 lines of test code, reused across all curated tests) that produces wire-format-valid bit patterns from a declarative builder. Each test verifies both that `decode` returns the right value AND that `skip` advances by the same bit count. Coverage axes per category:
    - bit-count decoders: typical / zero / max value at byte-aligned and bit-misaligned start positions
    - varint-family: 1-byte / multi-byte / max-byte continuation patterns
    - flag-conditional decoders (FloatCoord, QAngleNoBitCount, PolymorphicPointer, …): every flag-combination path
    - length-prefixed (StringLen, CUtlBinaryBlock): zero-length / typical / max-length
    - composite (Vector, Array): 0 / 1 / many elements
  - **End-to-end reject-everything parity test.** Parse representative S1 (CSGO or Dota S1) and S2 (Dota S2 or CS2) replays from the corpus twice: once with no filter, once with a filter that rejects every class. Assert bitstream consumption is identical at every per-packet tick boundary. Catches drift bugs that pass unit tests but fail on real wire-format combinations, and verifies the `FieldReader.skipFields` plumbing end-to-end without needing dedicated FieldReader-level tests. Exercises real-distribution inputs across the full decoder × FieldOp × class cross-product.
  - **Runtime parity verifier.** A `-Dclarity.test.skipParity=true` flag that, when enabled, wraps every `DecoderDispatch.decode` call with a skip-then-rewind-then-decode-and-compare check. Run alongside the end-to-end parity test on real replays, this turns every decoder invocation in the parse into an automatic parity assertion — effectively exercising the real-replay input distribution against the skip path at decoder granularity. Catches anything the curated branch-coverage tests miss.
  - **Build-time enforcement.** The `@RegisterDecoder` annotation processor SHALL fail the build if a decoder is missing either `decode` or `skip`. Makes "I forgot to add the skip method" impossible to merge.

## Capabilities

### New Capabilities
- `entity-class-filter`: opt-in per-class entity-filtering API that prevents both Java-side entity creation and downstream wire-format costs (allocation, state writes, dispatch) for entity classes the consumer declares uninteresting.

## Impact

- `src/main/java/skadistats/clarity/io/decoder/*Decoder.java` — add a static `skip` method to each of ~30 concrete decoder classes, using the category-appropriate strategy (bit-count, length-then-skip, recursive, conditional, or walking).
- `src/main/java/skadistats/clarity/io/decoder/factory/` — annotation processor emits `DecoderDispatch.skip` alongside existing `decode` / `decodeInto`; build fails if a decoder is missing a `skip` static method.
- `src/main/java/skadistats/clarity/io/bitstream/BitStream.java` — new `skipVarUInt` and `skipVarULong` methods.
- `src/main/java/skadistats/clarity/io/FieldReader.java` — new `skipFields` method with a default delegating to `readFields` for safety (kept as a safety net for any future `FieldReader` implementation).
- `src/main/java/skadistats/clarity/io/s2/S2FieldReader.java` — concrete S2 `skipFields` implementation.
- `src/main/java/skadistats/clarity/io/s1/S1FieldReader.java` — concrete S1 `skipFields` implementation in the base class (relies on `readIndices`, which the engine-specific subclasses already implement); `CsgoFieldReader` and `DotaS1FieldReader` inherit it.
- `src/main/java/skadistats/clarity/processor/entities/Entities.java` — `BitSet skippedIds`, filter consultation on CREATE, skip dispatch on UPDATE, bit-clear on DELETE.
- `src/main/java/skadistats/clarity/processor/runner/AbstractFileRunner.java` — `withEntityFilter(Predicate<DTClass>)` builder.
- `src/test/java/skadistats/clarity/io/decoder/` — extend with per-decoder fuzz parity tests + per-decoder branch-coverage curated tests (one test file per concrete decoder, e.g. `IntSignedDecoderTest`, `FloatCoordDecoderTest`); plus a shared `BitstreamBuilder` test utility for constructing bit patterns declaratively.
- `src/test/java/skadistats/clarity/processor/entities/` (new) — end-to-end reject-everything parity tests against one S1 and one S2 replay.
- Test-only utility for the `-Dclarity.test.skipParity=true` runtime verifier wrapper.

No breaking changes. Default behavior (no filter set) is byte-identical to current behavior. Filter is purely opt-in.

## Non-goals (explicit)

- **Per-property or per-field filtering.** Only per-class. Sub-class filtering (e.g., "I want this class but only its position fields") is harder, of narrower value, and is not in scope. Revisit if demand materializes.
- **Per-event-type filtering.** Filter is binary per class (in or out); there is no "I want creates but not updates" knob. The wire-format-skip path handles updates uniformly.
- **Automatic filter derivation from `@OnEntityCreated` / `@OnEntityProperty` annotations.** A future change could make the filter implicit by walking registered processors, but v1 is explicit-filter-only to keep semantics simple. Open question: how to handle a registered listener whose class isn't in the manual filter — left to a follow-up.
- **Skipping the FieldOp walk itself.** The wire format does not provide per-entity bit-length prefixes; FieldOps decide which fields are coming and what their decoder is, so they must execute. Only the decode + state-write costs are eliminable.
- **A SWAR `skipString()` for zero-terminated strings.** A SWAR scan-for-zero-byte over the 64-bit peek window would speed up category-5 string skipping, but strings on entity-update hot paths are rare (most live in stringtables, which use independent code paths). The plain `while (bs.readUBitInt(8) != 0) {}` skip body is adequate for v1. Revisit if a profile shows otherwise.
- **A declarative decoder DSL that generates both `decode` and `skip` from a single description.** Would eliminate skip-parity drift by construction, but requires a substantial rewrite of the decoder layer. Out of scope; capture as future work.
- **A comprehensive wire-format conformance corpus.** This change brings decoder testing from "very thin" to "covers the parity invariant and exercises every internal branch of every decoder," not to "every wire-format edge case Valve has ever produced." A conformance suite (captured-bits corpus per decoder type, version-tagged, regression-tracked) is a worthwhile follow-up but would multiply test surface beyond what this change needs.
- **FieldReader-level unit tests.** The reject-everything end-to-end parity test exercises both `S2FieldReader.skipFields` and `S1FieldReader.skipFields` against real replays — that's sufficient coverage for the FieldReader plumbing. Dedicated `S2FieldReader` / `S1FieldReader` test fixtures would require constructing or capturing realistic `DTClass` instances + entity-update bit fragments, which is significant infrastructure for a layer the end-to-end test already covers. Decoder-level tests are where the wire-format complexity lives and where test investment is best concentrated.
- **Property-based testing framework adoption** (jqwik / similar). The random-bytes fuzzing in `DecoderDecodeIntoParityTest` already approximates property-based testing for the parity invariant; a full PBT framework is a tool choice for a future change if the fuzz coverage proves insufficient.
- **Encoder / writer code.** Clarity is a read-only parser; we won't add a wire-format encoder just to make round-trip tests prettier. Curated decoder tests use hand-crafted bit patterns, not encoder output.
- **Filter changes mid-parse.** Filter is set once before `runWith` and immutable thereafter.
