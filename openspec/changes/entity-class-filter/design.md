## Why per-class only (not per-property or per-event)

Per-class is the natural granularity of the wire format: the entity CREATE message carries the class, and that class is what downstream consumers reason about. Per-property would require parsing the FieldOps to know which field is coming and only THEN deciding to skip — that work overlaps with what would be saved, killing the win. Per-event-type ("creates yes, updates no") is occasionally useful (entity counting) but solvable in the listener with a flag check; not worth API surface.

Per-class also matches the existing listener-annotation vocabulary (`@OnEntityCreated(classPattern=...)` etc.), so consumers already think in these terms.

## Why filtered entities are not in the entity collection

Three alternatives were considered:

| Option | Behavior on `entities.getById(skippedId)` | Mental model |
|---|---|---|
| 1. Not in collection at all | returns null (as if never transmitted) | clean — filter is "tell me what exists" |
| 2. Entity present with stale state | returns entity with create-time state only | "frozen in time" — silent bug magnet |
| 3. Entity present, throws on access | returns entity, throws on `getProperty` | loud failure, but breaks iteration code |

Option 1 wins on simplicity: a filtered entity is *as if it were never transmitted*. No sentinel object, no stale-state pitfall, no diverging code paths in the entity-access API. The collection is "things the consumer asked for"; everything else does not exist from the consumer's perspective.

Cost: a consumer who passes a too-narrow filter and later realizes they wanted the data sees `null` rather than getting partial info. This is a documented contract, not a bug — and it's symmetric with the existing behavior of "an entity id that was never transmitted returns null."

## Tracking skipped ids: BitSet

The parser must remember which entity ids were skipped, because subsequent UPDATE messages reference the id without re-stating the class. Options:

```
BitSet                         O(1) get/set, MAX_ENTITIES/8 bytes (~1 KB)
boolean[]                      O(1) get/set, MAX_ENTITIES bytes  (~8 KB)
Set<Integer>                   O(1) avg but boxing per put + hash, ~16 B/entry
Sentinel in entity slot array  O(1), zero extra memory, but pollutes
                               the existing slot-typed-as-Entity invariant
```

`BitSet` is the right answer: smallest, fastest, doesn't touch the existing entity-array invariant. Clear-on-delete to handle id reuse.

## Skip-parity invariant

The single load-bearing correctness property: for every decoder, `skip(bs)` advances `bs.pos` by exactly the same delta as `decode(bs)` would. Any drift corrupts the next field's decode — and since decoder errors are not local (the bitstream is a single shared cursor), drift in one decoder breaks every downstream field of every entity for the rest of the packet.

The contract is about pos-delta equivalence, NOT operation equivalence. Skip implementations are free to use entirely different code paths than decode (e.g., the SWAR varint skip path takes the same bit count as the byte-loop decode, but reaches it via different arithmetic). Parity is asserted on `bs.pos` before and after, not on operation sequences.

Long-term: a code generator that derives `skip` from `decode` declaratively would eliminate drift by construction. Out of scope here — would require rewriting decoders against a DSL. Capture for follow-up.

## Test infrastructure

The current state of decoder testing is thin: `DecoderDecodeIntoParityTest` (random-bytes fuzz parity between `decode` and `decodeInto`), `S2DecoderFactoryTest`, and that's it — no per-decoder unit tests, no `FieldReader` tests, no end-to-end parser tests. The skip-parity invariant requires decoder-level rigor we don't currently have. Test investment for this change concentrates at the decoder boundary, where the wire-format complexity lives.

**Scope note: only valid wire-format inputs are in scope.** Decoders only ever see bits that Valve's encoder produced. Robustness testing against arbitrary random bytes (the "fuzz" framing) is not in scope: if a decoder crashes on a byte sequence Valve's encoder cannot produce, that's acceptable — no real replay reaches that input. Coverage is built from real-distribution inputs (real replays) plus curated wire-format-valid bit patterns (hand-crafted to exercise every decoder branch). Random bytes neither augment nor substitute for either source.

Three layers:

### Layer 1: Per-decoder branch coverage (depth)

For every concrete decoder, exercise every internal branch / code path with curated hand-crafted bit patterns. This is the layer that catches "I forgot the path where flag X is set" bugs that random fuzz won't reliably hit. Each curated test:

- Constructs a known bit pattern using a `BitstreamBuilder` utility.
- Calls `decode`, asserts the returned value is what the wire bits should produce.
- Calls `skip` on the same bit pattern, asserts the pos delta matches `decode`.

**The `BitstreamBuilder` utility (test-only, ~50–100 lines).** Test author should not have to hand-pack bits into a `byte[]`. The builder is a declarative DSL over the wire format:

```java
var bs = bitstream()
    .skip(3)                        // start at pos 3 (bit-misaligned)
    .add(0xA5, 8)                   // 8 bits, value 0xA5
    .addVarUInt(300)                // varint encoding of 300
    .addStringZ("hello")            // UTF-8 + NUL
    .build();
```

This builder is the upfront infrastructure investment; once it exists, curated tests across all decoders become readable 5–15 line methods.

**Branch coverage per decoder, by category:**

| Category | Branch axes to cover |
|---|---|
| **1. Bit-count** (Bool, IntSigned, IntUnsigned, LongSigned, LongUnsigned, FloatNoScale, FloatDefault, FloatCellCoord, FloatQuantized, FixedPointer) | Typical / zero / max value × byte-aligned / bit-misaligned start. For decoders with constructor-configurable bit widths: 1-bit, mid-range, max-bit-width (catches `<<` of 0 / 32 / 64 edge cases). |
| **2. Length-then-skip** (StringLen, CUtlBinaryBlock) | Zero-length blob / typical-length / max-length. Varint length prefix variations. |
| **3. Recursive composite** (Vector, VectorXY, VectorNormal, VectorDefault, Array) | 0 elements / 1 element / many elements. Component coverage inherits from the component's own test. |
| **4. Conditional** (PolymorphicPointer, FloatCoord, FloatCoordMp, FloatNormal, QAngleNoBitCount, QAnglePitchYawOnly) | Every flag combination — for FloatCoord with 3 independent flags that's up to 8 cases (some unreachable when both flags are clear, document why). |
| **5. Walking** (IntVarUnsigned, IntVarSigned, IntMinusOne, LongVarUnsigned, LongVarSigned, StringZeroTerminated) | 1-byte / 2-byte / 3-byte / max-byte varint patterns. Empty / short / max-length zero-terminated strings. Boundary cases at bit-misaligned starts. |

Rough estimate: 60–90 distinct branch cases across ~30 decoders. At 5–15 LoC per test, ~1500 LoC of curated test code. Real investment, defensible given the parity invariant is load-bearing AND the existing per-decoder coverage gap is filled at the same time.

**Test file organization.** One test file per concrete decoder: `IntSignedDecoderTest`, `FloatCoordDecoderTest`, etc. Each file contains that decoder's curated branch-coverage tests. A shared `DecoderTestBase` provides the `bitstream(...)` builder and `assertSkipParity(...)` helpers (the latter wraps "decode and skip on the same bit pattern, assert pos delta matches").

### Layer 2: End-to-end reject-everything parity test

For each engine family represented in the test corpus (at minimum: one S1 replay, one S2 replay):

- Parse with no filter; record bitstream pos at every per-packet `CSVCMsg_PacketEntities` boundary.
- Parse again with a filter that rejects every class; record same pos sequence.
- Assert the two sequences are identical.

This catches drift bugs that pass all decoder unit tests but fail on the actual cross-product of decoder × FieldOp × class shape that real replays exercise. It also covers the `FieldReader.skipFields` plumbing end-to-end, which is why dedicated `S2FieldReader` / `S1FieldReader` unit tests are not in scope — they would require constructing or capturing realistic `DTClass` + wire-fragment fixtures, infrastructure that this end-to-end test makes unnecessary.

Slow (full parse twice per replay) but irreplaceable.

### Layer 3 (test-only utility): runtime parity verifier

A `-Dclarity.test.skipParity=true` flag flips `DecoderDispatch.decode` to a verifying wrapper that:

1. Records `bs.pos` before the call.
2. Performs the normal `decode`, records `p_decode`.
3. Rewinds bs to the pre-call pos, calls `skip`, records `p_skip`.
4. Asserts `p_decode == p_skip`; if not, throws with the decoder id and observed bit deltas.
5. Rewinds bs to the pre-call pos one more time, calls `decode` again to leave the bitstream in the correct post-call state.

Costs ~2-3× CPU per call but only when enabled. Useful for catching drift in any test that runs through `DecoderDispatch` — including non-decoder tests that happen to exercise wire-format parsing. **Most valuable when enabled alongside Layer 2's end-to-end parse**: every decoder invocation in the real-replay parse becomes an automatic skip-parity assertion at decoder granularity, effectively exercising the real-replay input distribution against the skip path with maximum precision (parity violations are reported per-decoder-call rather than only at packet boundaries). Build-time the flag is off; CI can run with it on.

### Coverage philosophy

The goal is "make drift bugs loud" on inputs that can actually occur. Layer 1 (curated branch coverage) covers every internal decoder code path on hand-crafted wire-format-valid inputs. Layer 2 (end-to-end real replays) covers the cross-product of decoder × FieldOp × class on the real input distribution. Layer 3 (runtime verifier on Layer 2) closes the gap: every decoder invocation in real-replay parsing is automatically parity-checked, so anything Layer 1 missed surfaces as a per-call mismatch in the integration test.

Random-bytes input is explicitly NOT a coverage source. Decoders only see Valve's wire format; correctness on bit patterns Valve cannot produce is irrelevant. If garbage input causes a decoder to crash, that's acceptable.

**Decoder testing is where test investment concentrates** because that's where wire-format complexity lives. The layer above it (FieldReader) is plumbing that fans bits out to decoders — its correctness is verified transitively via Layer 2 on real replays. Spending separate test investment at the FieldReader level would mostly re-test what the decoder tests already cover, plus a thin layer of FieldOp / readIndices interaction logic that the end-to-end test exercises naturally.

Explicitly NOT in scope: a captured-bits-per-decoder conformance corpus, version-tagged regression fixtures, property-based testing framework adoption, random-bytes robustness testing, FieldReader-level unit tests, or any encoder code. Those are good ideas for follow-up changes; this change brings testing from "thin" to "adequate for the parity invariant" by concentrating depth at the decoder boundary on inputs that actually occur.

## Annotation interaction (deferred)

Open: if a registered processor declares `@OnEntityCreated("CCSProjectile")` and the manual filter excludes `CCSProjectile`, the listener silently never fires. Three options:

- A: Filter wins silently → silent bug.
- B: Filter is union of (manual filter ∪ classes mentioned in registered listener annotations). Auto-derivation sneaks in from day one.
- C: Registration throws at startup if a listener references a filtered class.

v1 ships with **option A** (filter wins) and a documented warning. Option B is the natural v2 — and turns the manual filter into a *narrower-than-needed* override rather than the source of truth. Worth tackling as a follow-up change once the basic mechanism lands and we can see how consumers actually use it.

## Filter signature

`Predicate<DTClass>` over `Predicate<String>` (dtName). Both are simple; DTClass exposes more (network name, parent class, etc.) so the consumer can write smarter filters without us having to extend the API later. Performance is irrelevant — filter runs once per CREATE, which is not the hot path.

## Decoder skip categorization

A naive "skip = decode and discard the result" framing is wrong: it lumps together decoders whose skip paths differ by orders of magnitude in cost. The codebase's ~30 concrete decoders sort into five structural categories, each with its own optimal skip strategy.

| Category | Decoder examples | Skip strategy | Cost vs decode |
|---|---|---|---|
| **1. Bit-count** | Bool, IntUnsigned, IntSigned, LongUnsigned, LongSigned, FloatNoScale, FloatDefault, FloatCellCoord, FloatQuantized, FixedPointer | `bs.skip(n)` where `n` is the statically-known bit width | ≈0 — single pos increment |
| **2. Length-then-skip** | StringLen, CUtlBinaryBlock | `var n = bs.readVarUInt(); bs.skip(n * 8);` — read the length prefix, advance past the body without copying bytes | very low — avoids the byte-by-byte copy + String/byte[] allocation that decode performs |
| **3. Recursive composite** | Vector, VectorXY, VectorNormal, VectorDefault, Array | Call the component decoder's `skip` N times | inherits component cost; avoids per-component result wrappers and the composite container alloc |
| **4. Conditional** | PolymorphicPointer, FloatCoord, FloatCoordMp, FloatNormal, QAngleNoBitCount, QAnglePitchYawOnly | Read the flag bit(s), then either `bs.skip(n)` or no-op per flag | roughly 1/3 to 1/2 of decode cost — the flag reads happen either way, but float arithmetic, sign handling, and wrapper allocation do not |
| **5. Walking** | IntVarUnsigned, IntVarSigned, IntMinusOne, LongVarUnsigned, LongVarSigned, StringZeroTerminated | Must consume bits until a data-dependent terminator. For varints: `bs.skipVarUInt()` (SWAR pos-only path, see below). For zero-terminated strings: `while (bs.readUBitInt(8) != 0) {}` | ≈ decode minus the value extraction and boxing |

**Categories 1–4 cover roughly 22 of the ~30 decoders.** All four are "fundamentally less work" than full decode, not "decode minus an allocation." Category 5 (~5–6 decoders) is the only case where skip cost is close to decode cost.

This matters for the savings estimate. A naive accounting would assume "skip ≈ decode allocations," giving the proposed 2–2.5× per-entity win. The categorization above means category 2 and 3 decoders (length-prefixed blobs and composites) save *much* more than just the wrapper allocation — they save the per-byte work and the per-component computation. Entity classes dominated by string/vector/array fields (which describe most of the projectile, decoration, and ambient-world classes a consumer typically filters out) benefit disproportionately.

## SWAR varint skip

A new `BitStream.skipVarUInt()` / `skipVarULong()` is added specifically for category 5. The implementation:

```
peek 64 bits at current pos (handles bit-misalignment via existing two-word OR-shift)
contMask = ~word & 0x8080_8080_8080_8080
bytes    = (Long.numberOfTrailingZeros(contMask) >>> 3) + 1
pos     += bytes << 3
```

For `skipVarUInt`, u32 varints are at most 5 bytes — terminator is guaranteed within the 8-byte peek window, no slow path needed. For `skipVarULong`, the 9–10 byte case requires a slow-path fallback.

This is the SWAR shape we tried earlier for `readVarUInt` decode — which was bench-flat because the `Long.compress` value-extraction cost on Zen 5 cancelled the savings. **For skip we omit the value extraction entirely**, so the SWAR path is just `peek + NTZ + pos update` — a genuine net win over the byte-loop alternative.

The varint skip path is hit not only by the varint-typed decoders themselves but also by the length-prefix readVarUInt in category 2 (StringLen, CUtlBinaryBlock). So this single BitStream helper accelerates skips across categories 2 and 5.

## Engine coverage: S1 and S2

Both engines are in scope. The shared decoder layer means per-decoder `skip` methods accelerate both engines without duplication — `DecoderDispatch.skip` covers every registered decoder regardless of which engine uses it. The two `FieldReader` implementations diverge:

| Aspect | S2 | S1 |
|---|---|---|
| Per-update bookkeeping the skip path can't avoid | FieldOp Huffman + FieldPath manipulation + `resolveField` per field | `readIndices` (subclass-specific: CSGO vs DotaS1) + `receiveProps[idx].getSendProp().getDecoder()` per field |
| Eliminable per-entity work fraction (rough) | 25–45% of full-decode cost | even lower than S2 — no FieldOp walk to amortize against, so the skipped portion dominates the surviving overhead |

In practice S1 skip is *structurally cheaper relative to its own decode* than S2 skip is relative to its own decode, because S1 has less unavoidable wire-format bookkeeping. This means S1 consumers with narrow filters benefit more, proportionally, from the feature.

## What the skip path can't avoid

Even with category-appropriate skip per decoder, an entity update still costs:

- Entity header read (id, flags, optionally class on create).
- **S2 only:** every FieldOp in the update — Huffman decode (`readFieldOpId`) plus FieldPath manipulation. The wire format does not provide per-entity bit-length prefixes; FieldOps decide which fields are coming, and FieldOp execution depends on prior FieldOp state.
- **S2 only:** `resolveField` per field — walks the serializer tree to map FieldPath → field metadata → decoder.
- **S1 only:** `readIndices` — reads the bit-encoded list of changed-prop indices for this entity. Cheaper than S2's FieldOp walk per-update, but still scales with the number of changed properties.

What is eliminated, by decoder category:

| eliminated cost | by which category |
|---|---|
| Decoded primitive boxing (Integer, Long, Float wrappers) | all |
| String / byte[] allocation and content copy | 2 |
| Component-value computation (float arithmetic, sign extension, scaling) | 1, 4 |
| Vector / array result allocation | 3 |
| State writes (`decodeInto` polymorphic FieldLayout switch — the second-largest branch-miss hotspot in the parser per the 2026-05-11 profile) | all |
| Property-change dispatch and any registered diff evaluation | all |
| Entity object allocation, FieldPath snapshot retention | all (consumers don't see the entity) |

Rough back-of-envelope: with category-appropriate skip, the per-entity skip-path cost is somewhere in the 25–45% of full-decode cost range (down from the 50–60% the naive read-and-discard framing would have produced). Combined with 70–90% of entity volume being filtered out on typical narrow consumers, the workload-level win lands in the 1.7–2.5× range. That's a meaningful step-change relative to single-digit-percent micro-optimization, and the gap is wider than the original proposal suggested.

**These are paper estimates.** The implementation MUST be benched against a real consumer workload (e.g., the bench harness with a filter equivalent to "only player + gamerules") before the proposal is considered to have delivered. If the win is materially smaller than the estimate, that's signal worth understanding — most likely it would mean the eliminated work was smaller than the model assumed, or the FieldOp-walk + resolveField overhead is bigger than expected.

## Out-of-scope alternatives

- **Two parser modes (full vs filtered) selected per-run.** Would let a "filtered" mode skip even more (e.g., maintain no entity collection at all). Saves more, but creates two divergent code paths to maintain. Filter-as-additive-layer keeps a single code path.
- **Lazy decode.** Defer the per-field `decode` until the consumer asks for the property. Different optimization shape (saves work for unused *properties*, not unused *entities*). Possibly complementary, not substitutable. Separate change if pursued.
- **Cache-friendly entity layout reorganization.** L1 hit rate is already 97%; not the bottleneck. No payoff.
