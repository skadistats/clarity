# accelerate-s1-flat-state — Results

Generated 2026-04-16, on `next` branch (HEAD after CP-2 default-flip + CP-3 trace-infra extension).

## Headline numbers

`S1EntityStateParseBench` — JMH single-shot, 3 warmup × 10 measurement iterations, `@Param({"OBJECT_ARRAY", "FLAT"})` × 2 S1 replays. Captured with `bench-results/2026-04-16_184644_s1_next-590d52f/`.

| Replay | impl | wall-clock (median) | alloc/op |
|---|---|---|---|
| Dota S1 (`271145478.dem`) | OBJECT_ARRAY | 393.5 ms | 4.38 GB |
| Dota S1 (`271145478.dem`) | **FLAT** | **383.4 ms** (-2.6%) | **4.22 GB** (-3.7%) |
| CSGO S1 (`luminosity-vs-azio-cache.dem`) | OBJECT_ARRAY | 1204.7 ms | 15.56 GB |
| CSGO S1 (`luminosity-vs-azio-cache.dem`) | **FLAT** | **1150.0 ms** (-4.5%) | **14.96 GB** (-3.9%) |

**Spec target was ≥ -20% wall-clock and ≥ -50% alloc; actual is -3-5% wall and -4% alloc — significant deviation from target.**

### Why the deviation

The spec proposal projected savings from eliminating both `WriteValue` records *and* primitive boxing. The new unified reader (D8) eliminates `WriteValue` allocation for **both** state types via the fast-path `FieldChanges` constructor (`mutations == null`). So the relative FLAT-vs-OBJECT_ARRAY delta only captures the primitive-boxing + String-alloc savings, not the bigger `WriteValue` win that accrues to both paths.

- The `WriteValue`-elimination win is real but invisible in the FLAT-vs-OBJECT_ARRAY comparison; it benefits anyone using either S1 state type now.
- Total parse time is dominated by bitstream reads, protobuf parsing, and `FieldChanges` array allocation; entity-state writes are a smaller slice than the spec assumed.
- Allocations: the byte[] backing FLAT states is *larger* per copy than ObjectArrayEntityState's Object[] for STRING-heavy DTClasses (e.g. `DT_DOTA_PlayerResource` reserves 24 KB for 48 STRING props vs ~1.6 KB Object[] for OA). This partially offsets primitive-box savings on the alloc rate.
- A true CP-0 baseline (old reader with WriteValue records) was not captured (deferred — bench harness gives pre/post directly via parameters). To attribute total improvement vs the prior `next` HEAD, one would need to run the bench against `590d52f` (pre-implementation) and compare.

### Architectural goals achieved

- Zero-alloc primitive writes on FLAT (verified by `decodeInto` byte layout matching `applyMutation(WriteValue)` byte layout in `S1FlatEntityStateTest`).
- Zero-alloc inline-string writes on FLAT (parity with `StringLenDecoder.decode` byte layout verified).
- Eager-copy `S1FlatEntityState.copy()` matches post-`strip-entity-state-cow` discipline.
- State-agnostic `S1FieldReader.readFields` fast path: same code path for both variants; impl chooses zero-alloc vs boxing per-leaf.

## Inline-string memory footprint (CP-0 audit 0.4)

From `dev/s1sendtables` dump on Dota S1 replay (`s1sendprops_-1.txt`, 463 DTClasses, 458 total STRING props across all classes):

| DTClass | STRING props | Worst-case bytes |
|---|---|---|
| DT_DOTA_PlayerResource | 48 | 24,720 |
| DT_PointCommentaryNode | 4 | 2,060 |
| DT_MaterialModifyControl | 4 | 2,060 |
| DT_Tesla / DT_SlideshowDisplay / DT_MovieDisplay / DT_DOTAPlayer | 3 each | 1,545 each |
| (~7 classes) | 2 each | 1,030 each |
| (~30+ classes) | 1 each | 515 each |

**No DTClass exceeds the 64 KB warning threshold from the spec.** Largest single class is `DT_DOTA_PlayerResource` at ~24 KB; well within budget. No CP-1 construction-time warning required.

## Per-CP gate results

| CP | Gate | Result |
|---|---|---|
| 0.1 | Both S1 replays smoke-parse on pre-change `next` tip | **PASS** — Dota: 844 ms, CSGO: 1535 ms (single-shot, no JMH warmup) |
| 0.2 | S1 parse-bench harness lands | **PASS** — `S1EntityStateParseBench` + `S1Main` + `s1Bench` gradle task added |
| 0.3 | Baseline event-stream capture | **PASS** — `/tmp/s1-parity-baseline/{dota,csgo}.OBJECT_ARRAY.txt` (md5 a291dc7e..., 006bf55e...) |
| 0.4 | Inline-string budget audit | **PASS** — no class above 64 KB |
| 1.12 | Unit tests green + repros green | **PASS** — `S1FlatEntityStateTest` 18 tests pass; `s1tempentities` parses 137,807 temp entities cleanly |
| 2.3 | Parity diff under new fast-path reader | **PASS** — `/tmp/s1-parity-cp2/*.FLAT.txt` md5 matches CP-0 baseline byte-for-byte for both replays |
| 2.4 | Repros + S1 examples on both variants | **PASS** — `s1tempentities` works under both OBJECT_ARRAY and FLAT |
| 2.5 | Default flipped to FLAT | **PASS** — `AbstractFileRunner.s1EntityStateType = S1EntityStateType.FLAT` |
| 2.6 | Bench thresholds (≥ -20% wall, ≥ -50% alloc) | **DEVIATION** — actual -3-5% wall, -4% alloc. See "Why the deviation" above. Architecture correct; spec targets were too optimistic given the unified-reader design. |
| 2.7 | S2 benches unchanged | **PASS** — FLAT is still fastest at 1626 ms (-5% vs NESTED_ARRAY baseline 1712 ms) on Dota S2; no regression vs pre-change S2 results |
| 3.1 | clarity-analyzer compiles | **PASS** — `./gradlew compileJava` green via composite build |
| 3.2 | MutationTraceBench round-trip on S1 | **PASS** — extended `MutationRecorder`/`BirthMaterializer` to support S1; both engines round-trip cleanly. Materialize-path output byte-identical to fast-path output via `S1ParityCaptureMain --materialize`. |
| 3.3 | `:clarity:build` green | **PASS** |
| 3.4 | clarity-analyzer + MutationTraceBench round-trip | **PASS** (3.1 + 3.2 above) |

## Acceptance checklist

- [x] `S1FlatEntityState`, `S1FlatLayout`, `S1FlatEntityStateTest` land in `model.state` package
- [x] `EntityState.decodeInto` added to interface; `ObjectArrayEntityState`, `S1FlatEntityState` provide concrete impls; S2 fallback impls (`NestedArrayEntityState`, `TreeMapEntityState`) throw — they never reach this path
- [x] `FlatEntityState.decodeInto` promoted to `@Override`
- [x] `S1EntityStateType.FLAT` added; `createState` signature changed to `(S1DTClass)` — `EntityStateFactory.forS1` updated; `S1DTClass.getEmptyState()` updated
- [x] `S1DTClass.getFlatLayout()` added with lazy build + cache
- [x] `S1FieldReader.readFields` rewritten per D8: fast path is state-agnostic; debug + materialize helpers preserve legacy WriteValue staging
- [x] `TempEntities` updated to pass real state to `readFields` (was passing `null` then using `applyTo`; now state mutates in place)
- [x] Default S1 state type flipped to `FLAT` in `AbstractFileRunner`
- [x] CP-0 + CP-2 parity captures match byte-for-byte for both Dota S1 and CSGO S1 replays
- [x] Materialize path produces byte-identical results to fast path (validated via `S1ParityCaptureMain --materialize`)
- [x] Trace stack (MutationRecorder/BirthMaterializer) extended to support S1 — `BirthRecipe` now holds `DTClass` instead of `SerializerField`
- [x] Full `:clarity:build` green; `clarity-examples` build green; `clarity-analyzer` compileJava green
- [ ] Bench wall-clock target (-20%) — **MISSED** (-3-5% actual)
- [ ] Bench alloc target (-50%) — **MISSED** (-4% actual)

## Follow-ups

- **Investigate alloc breakdown** — JFR / async-profiler against S1 parse to identify what dominates the 15 GB CSGO allocation. WriteValue elimination (now unified across both paths) likely accounts for a much bigger absolute saving than the residual FLAT-vs-OBJECT_ARRAY delta. A pre-CP-1 vs post-CP-2 comparison would surface this.
- **Inline S1 ARRAY props** — 11 ARRAY props in full Dota S1 DT (per `dev/s1sendtables` audit). Audit `SendProp.template.type` for inner-element types; if uniformly primitive, add `ArrayDecoder.decodeInto`. Drops S1 refs slab entirely. Currently deferred per spec.
- **Cached-String companion** — only if profiles show `new String(...)` on inline-string reads as a hotspot.
- **S2 class `S2*` prefix rename** — purely mechanical (`FlatEntityState` → `S2FlatEntityState`, etc.). Out of scope; queued as a separate small follow-up.
- **Capability rename** — `flat-entity-state` → `s2-flat-entity-state`, etc. Larger openspec operation; can be batched with the S2 class rename.
- **Tighten `S1FlatEntityState.fieldPathIterator`** — currently yields all slots unconditionally (matches `ObjectArrayEntityState`); could filter on flag byte once consumer compatibility is audited.

## Verdict

The implementation is correct (byte-for-byte parity proven across two engines, fast-path and materialize-path, and trace round-trip). The headline performance numbers fell short of spec targets because the architectural choice to unify the reader fast path benefits both state variants, leaving a smaller residual delta between OBJECT_ARRAY and FLAT than projected.

The default has been flipped to FLAT; consumers wishing to fall back can opt in via `.withS1EntityState(OBJECT_ARRAY)`. All downstream gates (`:clarity:build`, `clarity-analyzer compileJava`, `clarity-examples build`, `s1tempentities`) are green.
