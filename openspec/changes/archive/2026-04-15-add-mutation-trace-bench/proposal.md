## Why

The existing `EntityStateParseBench` measures whole-replay parsing throughput, which bundles Snappy decompression, protobuf decoding, bit-reader traversal, serializer-table construction, field-path decoding, and mutation application into a single ~2000 ms iteration. Dispatch-level changes in `EntityState.applyMutation` — where the 5-arm `FieldLayout` chain and the 3-arm `StateMutation` chain live — produce effects on the order of 10-30 ms per iteration, well below the ±40 ms run-to-run noise floor of the parse bench.

Two recent experiments (`polymorphic-field-layout` virtualizing dispatch; a `sealed` → pattern-switch rewrite) could not be validated or falsified against this bench: the first produced a ~5% regression that was plausibly from `Cursor` allocation rather than dispatch shape, and the second produced a wash within noise. Without a bench that can actually resolve dispatch-level changes, we are flying blind on the hottest entity-state code path.

## What Changes

- New JMH bench (`MutationTraceBench`) that measures `EntityState.applyMutation` throughput in isolation
- A recorder component (in-memory only, not serialized to disk in v1) that taps the parser during a `@Setup(Level.Trial)` run and captures every `(FieldPath, StateMutation, targetEntityIndex)` tuple into an ordered list
- The measured `@Benchmark` method creates a fresh `EntityState` per iteration (per impl), then iterates the captured mutation list calling `applyMutation` — no protobuf, no bit-reader, no serializer-table work inside the measurement window
- Parameterized by `impl` (NESTED_ARRAY, TREE_MAP, FLAT — same as existing bench) and `replay` (same replays as existing bench)
- **Full-apply mode only**: the measurement includes descent + leaf write/resize/switch. No separate descent-only mode (explicitly scoped out — real mutations never skip the leaf; measuring descent alone would not reflect any real workload)
- **Whole-replay scope**: capture every mutation for every entity across the full replay, not a single-entity slice (simpler, more faithful to the parse bench's workload, defers the per-entity-class variance discussion to a follow-up)

## Dependencies

- **flat-entity-state** (ARCHIVED) — existing `FLAT` impl and `S2EntityStateType` enum used as a bench param
- Existing JMH harness at `src/jmh/java/skadistats/clarity/bench/` (Main, ReportWriter, ContextWriter, EntityStateParseBench) — new bench lives alongside and reuses Main's run infrastructure

## Capabilities

### New Capabilities

- `mutation-trace-bench`: An isolated JMH benchmark that measures `EntityState.applyMutation` throughput by replaying a real captured mutation stream against a fresh EntityState per iteration, with no parser work inside the measurement window.

### Modified Capabilities

(none)

## Impact

- `src/jmh/java/skadistats/clarity/bench/MutationTraceBench.java` — new JMH `@State` class. Whole-replay trace capture in `@Setup(Trial)`; per-impl state materialization in `@Setup(Invocation)`; pure update-mutation replay loop in `@Benchmark` (Design B — setup mutations are pre-applied, the measured loop contains only `applyMutation` calls). Requires a generous heap (`@Fork(jvmArgsAppend = {"-Xmx16g"})`) — the in-memory trace can reach hundreds of MB to ~1 GB on long replays.
- `src/jmh/java/skadistats/clarity/bench/MutationRecorder.java` — new `MutationListener` implementation in the jmh sourceSet. Assigns monotonic `stateId`s on `setState` boundaries, separates setup mutations (baseline build, entity create/recreate) from update mutations (the measured workload), and stores the captured stream as `(List<BirthRecipe> births, Mutation[] updateMutations)`.
- `src/main/java/skadistats/clarity/io/MutationListener.java` (new) and small wiring in `skadistats.clarity.processor.entities.Entities` — a single null-checked listener field plus four notification points at the existing `applyTo` call sites (`executeEntityCreate`, `executeEntityRecreate`, `executeEntityUpdate`, `getBaseline`). Production cost when listener is null is one branch per `applyTo` call. Confirmed during design that wrapping `EntityState` is not viable: `S2FieldReader` performs hard casts to `AbstractS2EntityState`.
- `src/jmh/java/skadistats/clarity/bench/Main.java` — add `MutationTraceBench` to the `include(...)` list (or split into a second entry point — preferred, since the parse and trace benches answer different questions and should be runnable independently).
- `ReportWriter.java` — extend to produce a comparable report for ops-per-mutation throughput (or a separate writer; TBD during implementation).
- No changes to main runtime code paths beyond the listener wiring.
- No changes to published API surface (the listener interface lives in `io` for proximity to `FieldChanges`; jmh sourceSet is not part of the published jar).

## Explicit non-goals

- **No disk-serialized trace.** The v1 bench requires a replay file on the runner's filesystem, exactly like the current parse bench. Checked-in binary fixtures are a possible future v2 for CI portability — deferred until the v1 proves useful.
- **No descent-only mode.** Only full `applyMutation` is measured.
- **No per-entity-class slicing.** Whole-replay only. If dispatch cost turns out to differ dramatically per entity class, that motivates a follow-up proposal.
- **No synthetic/histogram-based workload.** Real captured mutations only.
