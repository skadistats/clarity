# Results — `accelerate-flat-entity-state`

All measurements captured on JDK 21.0.10, Linux, single-fork JMH unless stated otherwise. Dota 2 replay used for parse benches: `replays/dota/s2/340/8168882574_1198277651.dem`. CP-0 baseline = commit `fccf74a` (pre-change master). Post-change HEAD = `ebe8d2f` (end of CP-6). All per-checkpoint gates passed (or are documented as partial-pass with rationale).

## Headline: end-to-end parse wall-clock vs CP-0

From `EntityStateParseBench` (full-replay parse, 3 warmup + 10 measurement iterations, single-shot timing):

| Impl         | CP-0 (ms) | Post (ms) | Δ        |
|--------------|-----------|-----------|----------|
| FLAT         | 1988      | **1646**  | **-17.2%** |
| NESTED_ARRAY | 1916      | **1730**  | **-9.7%**  |
| TREE_MAP     | 2200      | **2057**  | **-6.5%**  |

FLAT is now the fastest impl across CS2, Deadlock, and Dota 2 benches, overtaking NESTED_ARRAY. §6.21–6.23 gates.

## Allocation delta (FLAT parse, `-prof gc`)

§6.24 gate. Per-invocation alloc 9.11 GB post vs 10.12 GB pre (**-10%**). No `Integer` / `Float` / `Long` boxing and no `WriteValue` records in the top allocation sites. Dominant remaining allocs are `BitStream.<init>` (13%) and baseline `copy()` for CREATE/RECREATE paths — both outside the change's scope.

## Per-operation micros

### FlatEntityState.copy() (§1.14 gate, `FlatCopyBench`)
- **FLAT**: CP-0 crashes on trace-captured states (`markSubEntriesNonModifiable` bug on shared-slot index). Post-CP-1 copy allocates exactly one `FlatEntityState` wrapper per copy (57 B) and nothing else. Sub-Entry COW confirmed by `FlatEntityStateCowTest.deepNestedWriteClonesOnlyTouchedPath`. Effective speedup vs working tree-walk baseline ≈ order-of-magnitude (NESTED_ARRAY analogue below shows 13.2×).

### NestedArrayEntityState.copy() (§2.13 gate, `FlatCopyBench @Param(impl=NESTED_ARRAY)`)
- **13.2× faster**: 4458.2 → 337.0 µs/op.
- **8.4× less alloc**: 20.1 MB → 2.4 MB per full-replay snapshot pass. Per-state ≈ 48 B = `NestedArrayEntityState` wrapper header + fields; zero allocation beyond the wrapper.

### Decoder.decodeInto (§3.10 gate, `DecodeIntoBench`)
- Zero bytes allocated per decode on the new path; `decodeThenWrite` boxing path allocates 16 B (int/float box) to 96 B (Vector + `float[3]` + 3× Float box).
- ns/op speedup: **1.24–1.38× for scalars**, **1.68× for VECTOR**. Per-decode savings ~0.6–0.9 ns scalar, ~4.6 ns vector.

### FlatEntityState.decodeInto vs applyMutation(WriteValue) (§4.11 gate, `FlatWriteBench`)
- **INT_SIGNED 23.6% faster**, **FLOAT_DEFAULT 18.7% faster**, **VECTOR 37.7% faster** (1021 vs 1336 ns / 1090 vs 1341 ns / 2279 vs 3658 ns per 256-write invocation).
- **Zero write-path allocation** on the decodeInto path. `applyMutation` retains 32 B/write (INT/FLOAT: `Integer` or `Float` box + `WriteValue` record) or 112 B/write (VECTOR: `Vector` + 3× `Float` + `WriteValue`) — escape analysis does not eliminate these.
- Partial-pass note: scalars land below the 30% gate because the baseline per-write cost (~4 ns) is already small; the absolute 1.2 ns alloc-avoidance win doesn't clear 30% of that baseline. The end-to-end signal on `EntityStateParseBench` (17.2% FLAT wall-clock) is the real test.

### Inline-string roundtrip (§5.13 gate, `InlineStringBench`)
- `decodeIntoThenRead`: **6,537 ns/op**, **53,944 B/op**.
- `decodeAndWriteThenRead`: **18,316 ns/op**, **80,570 B/op**.
- **2.80× faster**, **26.6 KB less alloc/invocation** (≈104 B/iter: interned `String` + `WriteValue` + UTF-8 encode scratch eliminated).
- Per-read alloc on the inline path is dominated by the expected `new String(data, off, len, UTF_8)` — one String + its backing array, no hidden costs.

## Inline-string byte[] memory footprint (§5.12 gate)
- Worst-case per-serializer inline reservation: **~2 KB** (4-unbounded-string game-mode serializers with uniform 512 B reservation = 4 × 514 B).
- Median ~1.3 KB; most entity types <1 KB. Accepted at design time; no runtime budget check needed because `FieldLayoutBuilder.totalBytes` is strictly bounded by `Σ (3 + maxLength)` over string leaves.

## Acceptance checklist

- [x] `./gradlew build` — all tests green (clarity, full suite).
- [x] `openspec validate accelerate-flat-entity-state --strict` — passes.
- [x] `:dev:dtinspectorRun` — parses sample replay cleanly; compiles against modified clarity sources.
- [x] Downstream `clarity-analyzer` (JavaFX consumer, `next` branch, composite-build) — compiles and runs cleanly against modified clarity (user-verified, §8.6).
- [x] `MutationTraceBench` — materialize + replay round-trips traced packets on all 3 impls (§8.5, via `SmokeTraceMain`: 12.7 MB Dota replay → 2433 births + 1.95M updates, no exceptions).
- [x] `:repro:issue289Run`, `:repro:issue350Run` — see §9.4 below.

## Follow-ups scoped to successor changes

- **`lightspeed-eager-copy`** — strip owner-pointer COW machinery (`Entry.owner`, `refsOwner`, `pointerSerializersOwner`, `entriesOwner`, `ensureRefsOwned`, `rootEntryWritable`, `makeWritable`, `Consumer<Entry>` slot-setter plumbing). The CP-6 pivot showed COW is no longer necessary — state mutates eagerly and throw aborts the replay run.
- **`accelerate-s1-flat-state`** — port `ObjectArrayEntityState` / `S1FieldReader` to inline-strings + decodeInto built directly on the eager-copy model. S1 `ArrayDecoder` stays on the refs-slab path per §0.6 audit (11 props, dominated by 458 STRING props moved inline by the follow-up).
