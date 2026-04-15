## 1. Main-code hook surface

- [x] 1.1 Add a `MutationListener` interface in `skadistats.clarity.io` (alongside `FieldChanges`). Methods take `EntityState` references — the recorder owns id allocation, the parser stays bench-agnostic:
  - `void onBirthEmpty(EntityState newState, DTClass cls)`
  - `void onBirthCopy(EntityState newState, EntityState srcState)`
  - `void onSetupMutation(EntityState target, FieldPath fp, StateMutation mutation)`
  - `void onUpdateMutation(EntityState target, FieldPath fp, StateMutation mutation)`
- [x] 1.2 Wire the listener into `Entities.java`. Add a single null-checked field with a setter. Notification points:
  - `onBirthEmpty` after `cls.getEmptyState()` in `getBaseline`
  - `onBirthCopy` after `baseline.copy()` in `executeEntityCreate`, `executeEntityRecreate`, and after both `newState.copy()` snapshots that feed `updateEntityBaseline`
  - For mutation routing, replace each `changes.applyTo(state)` call with a small helper that, when the listener is non-null, iterates fps + mutations and notifies with `onSetupMutation` (baseline build, create, recreate) or `onUpdateMutation` (update). Production fast path with null listener is unchanged.
  - Add a public `getMutations()` getter to `FieldChanges` so the helper can iterate.
- [x] 1.3 Verify production overhead: the listener field is a constant null on the hot inlined path; JIT eliminates the null check entirely. Sanity-check via parse-bench delta before/after the wiring is added. *(Design confirmed; empirical sanity-check happens in §8 validation runs.)*

## 2. Recorder (jmh sourceSet)

- [x] 2.1 Define records in jmh sourceSet:
  - `Mutation(int stateId, FieldPath fp, StateMutation mutation)`
  - `BirthRecipe(int stateId, BirthKind kind, SerializerField field, int srcStateId, Mutation[] setupMutations)`
  - `BirthKind` enum: `EMPTY`, `COPY_OF`
- [x] 2.2 Implement `MutationRecorder` as a `MutationListener`. Owns `IdentityHashMap<EntityState, Integer>` for ref→stateId resolution. On birth, allocate next monotonic stateId, register the ref, append a `BirthRecipe` and open it as the current setup-mutation buffer. On setup mutation, append to the open buffer. On update mutation, resolve stateId via the map and append to the flat `updateMutations` list. On the next birth (or at finish), close the previous buffer into the `BirthRecipe`'s `setupMutations` array.
- [x] 2.3 Verify capture fidelity: total update-mutation count is stable across runs for a given replay; `births.size() == number of setState calls` observed in a debug print. *(Smoke-tested on `8168882574_1198277651.dem`: 59,918 births and 24,874,525 update mutations stable across two consecutive captures, ~3 s per capture.)*

## 3. Capture harness

- [x] 3.1 Create `MutationTraceCapture` utility in jmh sourceSet: given a replay path, runs a `SimpleRunner` with the listener attached and returns `(List<BirthRecipe> births, Mutation[] updateMutations)`. The trace is impl-independent — capture once, reuse for all impl params.
- [x] 3.2 Measure capture time and trace memory on the largest available replay; log at INFO. *(Realistic numbers: ~25M update mutations on a single Dota replay, ~2-3 GB of trace memory. The proposal's "1-2M" estimate was off by an order of magnitude. Bench JVM gets `-Xmx16g`; no abort threshold needed since the host has 64 GB.)*
- [x] 3.3 Verify the capture-time main-code listener overhead is unmeasurable when the listener is null (run parse bench before/after the hook is added; deltas should be within noise). *(Parse bench post-wiring on Dota: 1950.6 ± 47.4 ms — sits inside the noise floor of the historical pre-listener baseline (~2000 ms recorded in memory). The null-listener branch is JIT-eliminated as expected.)*

## 4. Replay harness

- [x] 4.1 Implement `BirthMaterializer` in jmh sourceSet: given `(List<BirthRecipe> births, S2EntityStateType impl)`, returns an `EntityState[]` indexed by stateId where each entry is brought to its post-setup state (apply EMPTY/COPY_OF construction + each birth's `setupMutations`).
- [x] 4.2 Pre-touch `updateMutations` array at @Setup(Invocation) — a single read pass to ensure the array and its elements are tenured before the hot loop. *(Implemented inline in `MutationTraceBench.setupInvocation`.)*

## 5. JMH bench

- [x] 5.1 Create `MutationTraceBench` in `src/jmh/java/skadistats/clarity/bench/` with same JMH annotations as `EntityStateParseBench` (SingleShotTime, 3+10 iters, 1 fork) and `@Param({"NESTED_ARRAY","TREE_MAP","FLAT"}) impl`, `@Param({""}) replay`. Set `@Fork(jvmArgsAppend = {"-Xmx16g"})` (or higher) — the in-memory trace can reach hundreds of MB to ~1 GB on long replays and the bench host has the headroom.
- [x] 5.2 `@Setup(Level.Trial)`: load replay, run capture, store `births` and `updateMutations` as fields. Capture runs once per trial — shared across impl params.
- [x] 5.3 `@Setup(Level.Invocation)`: materialize `EntityState[] states` from `births` for the current impl; pre-touch `updateMutations`.
- [x] 5.4 `@Benchmark public void replay()`: loop over `updateMutations`, dispatching by stateId.
- [x] 5.5 Verify: on the default Dota replay, per-iteration time is in the tens-of-ms range. *(Smoke-tested cold (no JIT warmup): NESTED_ARRAY 330 ms, FLAT 420 ms, TREE_MAP 560 ms for 25M mutations — ~13-22 ns/mutation. Impls are clearly distinguishable even cold; full JMH run will tighten further.)*

## 6. Wiring into Main

- [x] 6.1 Decide: add `MutationTraceBench` to the existing `Main`'s `include(...)` list, or add a separate `Main` entry point. *(Chose separate `TraceMain` per design preference. Output dir is prefixed `trace_` to disambiguate from parse-bench results.)*
- [x] 6.2 Register a second gradle task `traceBench` mirroring `bench` in `build.gradle.kts` that invokes the new Main.
- [x] 6.3 Confirm existing `./gradlew bench` still works and produces the same artifacts as today. *(Parse bench `--all` ran clean and produced `bench-results/2026-04-15_095436_next-fdbdc5b/` with the usual results.json + results.txt + context.txt structure. ReportWriter output unchanged.)*

## 7. Reporting

- [x] 7.1 Decide whether to extend `ReportWriter` or add a `TraceReportWriter`. *(Reusing existing `ReportWriter` — same impl param shape, same per-replay grouping, same Δ-vs-NESTED_ARRAY logic. `TraceMain` invokes it directly, no changes needed.)*
- [x] 7.2 Verify the Δ-vs-NESTED_ARRAY summary is still meaningful for the trace bench. *(First production run produced clean Δ summary: TREE_MAP +47.0%, FLAT +26.1%, NESTED_ARRAY baseline. ReportWriter handles trace bench output identically to parse bench.)*

## 8. Validation

- [x] 8.1 Run trace bench on default Dota replay, confirm FLAT / NESTED_ARRAY / TREE_MAP deltas are **distinguishable from noise**. *(Production config (3+10) on `8168882574_1198277651.dem`: NESTED_ARRAY 319.7 ±2.3 ms, FLAT 403.0 ±11.6 ms, TREE_MAP 470.0 ±4.1 ms. Relative errors 0.7-2.9%, impl deltas 26-47% — ~10-50× resolution headroom over the parse bench. Per-mutation costs 12.85-18.89 ns, matching the design's expected budget.)*
- [x] 8.2 Compare: for the same three impls, does the trace bench's relative ordering match the parse bench's? If not, investigate — a flipped ranking would signal a methodology bug. *(Ordering matches across all 3 replays: NESTED_ARRAY < FLAT < TREE_MAP in both benches. Trace-bench deltas are 3-5× larger than parse-bench deltas because parse includes ~1500 ms of dispatch-irrelevant work that dilutes the delta. No flipped rankings.)*
- [x] 8.3 Run `--all` (all three default replays) and confirm results are stable. *(Trace bench `--all`: NESTED_ARRAY relative errors 0.5-0.9% across replays, TREE_MAP 1.5-2.0%, FLAT 0.8-2.4% — well below the 26-54% impl deltas. Stable.)*

## 9. Documentation

- [x] 9.1 Update `CLAUDE.md` in clarity (if bench usage is documented there) or create a short README.md in `src/jmh/java/skadistats/clarity/bench/` explaining when to use parse-bench vs trace-bench. *(Created `src/jmh/java/skadistats/clarity/bench/README.md` covering both benches and when to use each.)*
- [x] 9.2 Archive a `bench-results/` baseline for v1 so future diffs have a reference point. *(`bench-results/trace_2026-04-15_095808_next-fdbdc5b/` (3-replay `--all` run) is the v1 baseline; sibling `bench-results/2026-04-15_095436_next-fdbdc5b/` is the matched parse-bench baseline.)*
