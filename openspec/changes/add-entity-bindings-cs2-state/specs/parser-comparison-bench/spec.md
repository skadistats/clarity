## ADDED Requirements

### Requirement: Benchmark harness location and task

A benchmark SHALL live under `bench/src/main/java/skadistats/clarity/examples/bench/cs2statebench/` and SHALL produce a Gradle `cs2stateBenchRun` task (via the existing `examples-convention` plugin) that runs the harness with a CS2 demo path as its first argument.

#### Scenario: Task exists and runs

- **WHEN** a user runs `./gradlew :bench:cs2stateBenchRun --args="replays/csgo/s2/3dmax-vs-falcons-m1-anubis.dem"`
- **THEN** the benchmark parses the demo using the `cs2state` example's `EntityBindings` wiring and prints a best-of-3 wall-clock result in milliseconds

### Requirement: Methodology consistent with PARSER-COMPARISON.md

The benchmark SHALL time the parse inside `runner.runWith(...)`, excluding JVM startup and including JIT warmup (consistent with how `entityrun` is measured in the existing clarity repo's PARSER-COMPARISON.md). The harness SHALL run at least three iterations and report the minimum wall-clock.

#### Scenario: Best-of-three reporting

- **WHEN** the benchmark completes
- **THEN** standard output includes the three individual iteration times and a labelled "best: <ms>" line

#### Scenario: JVM startup excluded

- **WHEN** a reader inspects the harness code
- **THEN** the timing brackets wrap only the parse loop (source + runner construction through `runWith` completion), not `main()` entry

### Requirement: Full state wiring active during measurement

The benchmark SHALL enable the complete `cs2state` `EntityBindings` wiring (Bomb, Players, Weapons, GameRules, TeamStates, Grenades, Infernos, Hostages, and synthetic event reconstruction) for the duration of the measured run. User handlers for synthetic events MAY be empty, but the dispatch path SHALL be exercised so that the measurement reflects equivalent-scope work to demoinfocs-golang's default ST configuration.

#### Scenario: All registrations present

- **WHEN** a reader inspects the benchmark's setup
- **THEN** every `forClass(...).register()` chain present in the `cs2state` example's wiring code is also active in the benchmark run

#### Scenario: Synthetic event dispatch exercised

- **WHEN** the benchmark runs
- **THEN** at least one no-op handler is subscribed to each synthetic event type, so that dispatch-path work is included in the timing even though the handler body is empty

### Requirement: Comparability artifact

The benchmark output SHALL be captured into a short artifact (e.g., `bench/cs2state-bench-results.md` or logged output committed alongside the benchmark) stating the measured number and the demo used, so the result can be compared against demoinfocs-golang's documented 5.17s ST baseline on the same anubis demo without re-running.

#### Scenario: Result file exists post-run

- **WHEN** the contributor completes the first full run after implementation
- **THEN** a results file or README update records: demo path, hostname/CPU if relevant, JVM version, measured best-of-3 wall-clock, and a one-line comparison against demoinfocs's 5.17s

### Requirement: Downstream update note

The change SHALL include a note (in the benchmark results artifact and/or the `cs2state` README) identifying the follow-up action to update `PARSER-COMPARISON.md` in the clarity core repo with the measured number. This update is explicitly out of scope for this clarity-examples change, but SHALL be documented as the expected next step.

#### Scenario: Follow-up documented

- **WHEN** a reader opens the results artifact or example README
- **THEN** they find a pointer stating "update PARSER-COMPARISON.md in the clarity core repo with this measurement" and the relevant section/lines of that doc
