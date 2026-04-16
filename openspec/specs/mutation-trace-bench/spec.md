# mutation-trace-bench Specification

## Purpose

Provide a JMH benchmark that measures `EntityState.applyMutation` throughput in isolation,
against a previously-captured stream of `(FieldPath, StateMutation)` tuples. The whole-replay
parse bench has a ~2-10% noise floor that hides dispatch-level changes (1-3% on `applyMutation`);
this bench exists specifically to resolve those changes by stripping Snappy decompression,
protobuf decoding, bit-reader traversal, serializer-table construction, and field-path decoding
out of the measured window.

## Requirements

### Requirement: Mutation-trace bench measures applyMutation throughput in isolation

The `MutationTraceBench` JMH benchmark SHALL measure `EntityState.applyMutation` throughput
against a previously-captured mutation stream, with no Snappy decompression, protobuf decoding,
bit-reader traversal, serializer-table construction, or field-path decoding inside the measured
window. The bench exists specifically so dispatch-level changes (1-3% on `applyMutation`) can be
resolved — the whole-replay parse bench has a ~2-10% noise floor that hides them.

The bench SHALL be parameterized by `impl` (`NESTED_ARRAY`, `TREE_MAP`, `FLAT`) and `replay`
(a path filled by the `TraceMain` entry point from CLI args).

#### Scenario: Measured window contains only applyMutation dispatch
- **WHEN** the bench's `@Benchmark` method runs
- **THEN** the loop body SHALL only call `EntityState.applyMutation(FieldPath, StateMutation)` via an array-indexed dispatch
- **AND** NO parser, decoder, bit-reader, field-resolution, or state-construction work SHALL occur inside the loop

#### Scenario: Standard bench invocation
- **WHEN** `./gradlew traceBench` runs with a default replay
- **THEN** the bench reports per-impl throughput as `SingleShotTime` in ms/op
- **AND** the output artifacts `results.json`, `results.txt`, and `context.txt` are produced under `bench-results/trace_<timestamp>_<branch-sha>/`

### Requirement: Trace is captured once per trial and shared across impl params

The bench SHALL capture the trace in `@Setup(Level.Trial)` and reuse it across all `impl`
parameter values within the trial. The captured `FieldPath` and `StateMutation` objects SHALL be
impl-independent (they describe what to apply, not how to store it), so a single trace replays
correctly against `NESTED_ARRAY`, `TREE_MAP`, and `FLAT` entity states.

#### Scenario: Same trace drives all three impls
- **WHEN** `@Setup(Level.Trial)` completes
- **THEN** the same captured trace is used by subsequent `@Setup(Level.Invocation)` calls regardless of the current `impl` param value

#### Scenario: Trace capture is deterministic for a given replay
- **WHEN** capture runs twice against the same replay file
- **THEN** the two resulting traces MUST agree on total birth count and total update-mutation count

### Requirement: State identity is tracked by monotonic stateId

The recorder SHALL assign a monotonically increasing `stateId` to every `EntityState` the
parser constructs during capture (class baseline builds, entity creates, entity recreates, and
`updateBaseline` snapshots). Every captured mutation SHALL carry the `stateId` of the state it
targets, and the replay loop SHALL dispatch via a dense `EntityState[]` indexed by `stateId` —
a plain array load, not a map lookup.

This model correctly handles entity-index reuse: when an entity slot is recreated (new handle,
new state), the parser calls `setState(newState)` on a fresh `Entity` object, and the recorder
assigns a new `stateId`, keeping the old and new states cleanly separated.

#### Scenario: Index reuse produces distinct stateIds
- **WHEN** entity index N is destroyed and a new entity is created at index N with a different handle
- **THEN** the new entity's state receives a distinct `stateId` from the old
- **AND** mutations targeting each state go to their own array slot

#### Scenario: Baseline and entity states share the same stateId space
- **WHEN** a class baseline state and an entity state both exist
- **THEN** both carry stateIds from the same monotonic counter
- **AND** an entity-create's `baseline.copy()` references the baseline's stateId as `srcStateId` in the birth recipe

### Requirement: Births are reproduced at @Setup(Invocation) as setup work

For each `BirthRecipe` in capture order, the replay harness SHALL (a) construct an `EntityState`
— either fresh via `S2EntityStateType.createState(SerializerField, ...)` for an `EMPTY` birth, or
via `states[srcStateId].copy()` for a `COPY_OF` birth — and (b) apply the birth's captured
`setupMutations` (the mutations that landed during baseline build / entity create / entity
recreate). All of this SHALL happen in `@Setup(Level.Invocation)`, outside the measured window.

The hot loop SHALL apply ONLY update mutations (those originating from the entity-update code
path), against states that are already in their post-create shape.

#### Scenario: EMPTY birth produces the requested impl
- **WHEN** a BirthRecipe of kind `EMPTY` is materialized with `impl=FLAT`
- **THEN** the resulting `EntityState` is a `S2FlatEntityState` built from the recipe's `SerializerField`

#### Scenario: COPY_OF birth clones its source
- **WHEN** a BirthRecipe of kind `COPY_OF` with `srcStateId=S` is materialized
- **THEN** the resulting `EntityState` is `states[S].copy()` — same impl, independent contents

#### Scenario: Setup mutations are pre-applied, not measured
- **WHEN** the measured loop starts
- **THEN** every state in the `states[]` array holds the logical contents it would have had in production immediately after its `setState(...)` installation

### Requirement: MutationListener surface exists on the parser

The parser SHALL expose a `MutationListener` interface in `skadistats.clarity.io` with methods
`onBirthEmpty`, `onBirthCopy`, `onSetupMutation`, and `onUpdateMutation`. All four take live
`EntityState` references as parameters — the listener is responsible for identity and stateId
allocation, the parser is bench-agnostic.

`Entities` SHALL provide a `setMutationListener(MutationListener)` setter and SHALL invoke the
listener at every `EntityState` construction point (baseline build, entity create, entity
recreate, `updateBaseline` snapshots of newly-created states) and at every `applyMutation` site
that routes through `FieldChanges.applyTo`.

#### Scenario: Listener observes every birth
- **WHEN** the parser constructs an `EntityState` via `cls.getEmptyState()` or via a `.copy()` call
- **THEN** the listener's `onBirthEmpty` or `onBirthCopy` method SHALL be invoked before the state is used

#### Scenario: Setup vs update mutations are distinguishable at the listener level
- **WHEN** a mutation is applied during baseline build, entity create, or entity recreate
- **THEN** the listener receives it via `onSetupMutation`
- **WHEN** a mutation is applied during entity update
- **THEN** the listener receives it via `onUpdateMutation`

### Requirement: Null-listener production overhead is unmeasurable

When `mutationListener == null` (production), the hooks added to `Entities` SHALL be a single
null-checked branch per `applyTo` call and per birth site. JIT inlining SHALL eliminate the
check on hot paths so the parse bench's `EntityStateParseBench` scores remain indistinguishable
from the pre-hook baseline.

#### Scenario: Parse bench score after wiring is within noise of baseline
- **WHEN** the parse bench runs with no listener attached
- **THEN** its score deviation from the pre-wiring baseline is smaller than the bench's own run-to-run standard error

### Requirement: Recorder identity tracking drops after capture

The `MutationRecorder` SHALL use an `IdentityHashMap<EntityState, Integer>` internally to resolve
state references to stateIds during capture. This map holds strong references to every
`EntityState` ever constructed during the parser run. When capture ends, the map SHALL be
cleared by `finish()` so those states become eligible for GC.

#### Scenario: Captured states are released after finish()
- **WHEN** `MutationRecorder.finish()` returns
- **THEN** the recorder no longer holds references to any captured `EntityState`
- **AND** only the resulting `CapturedTrace` (which references `FieldPath`, `StateMutation`, and `SerializerField` objects — not captured states) remains reachable

### Requirement: Trace bench tolerates multi-GB heap footprint

The bench SHALL declare `@Fork(jvmArgsAppend = {"-Xmx16g"})` so forked JMH JVMs have enough
headroom for the captured trace, which can hold tens of millions of update mutations and
consume several gigabytes of heap on a long replay. The `./gradlew traceBench` gradle task
SHALL rely on the `@Fork` annotation as authoritative — it MUST NOT duplicate the `-Xmx` flag
in the command-line arguments.

#### Scenario: Large Dota replay fits in the bench JVM
- **WHEN** the bench runs against a ~25M-update-mutation Dota replay
- **THEN** the forked JVM does not OutOfMemory, and the trace is materialized per iteration without heap exhaustion

### Requirement: Parse bench and trace bench share reporting machinery

`TraceMain` SHALL reuse `ReportWriter`, `ContextWriter`, and the same per-replay / per-impl
grouping logic as `Main`. Output schema (results.json, results.txt) SHALL be identical in shape
to the parse bench, differing only in absolute numbers and output directory prefix (`trace_`).

#### Scenario: Trace bench results render in the standard ReportWriter format
- **WHEN** `TraceMain` completes
- **THEN** `results.txt` contains per-replay tables with `Δ vs NESTED_ARRAY` columns
- **AND** Memory / GC pressure and Per-engine winners sections are produced identically to the parse bench

### Requirement: Trace bench is runnable independently from the parse bench

`MutationTraceBench` and `EntityStateParseBench` SHALL have separate entry points (`TraceMain`
and `Main`) and separate gradle tasks (`traceBench` and `bench`). They answer different
questions (end-to-end parsing perf vs isolated dispatch perf) and SHALL NOT be conflated into a
single run.

#### Scenario: Running one bench does not run the other
- **WHEN** `./gradlew bench` is invoked
- **THEN** only `EntityStateParseBench` runs, and only `bench-results/<timestamp>_<sha>/` (no `trace_` prefix) is produced
- **WHEN** `./gradlew traceBench` is invoked
- **THEN** only `MutationTraceBench` runs, and only `bench-results/trace_<timestamp>_<sha>/` is produced
