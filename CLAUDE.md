# CLAUDE.md — clarity

Replay parser for Dota 2, CS:GO, CS2, Deadlock. Source 1 + Source 2.

## Build / test
```bash
./gradlew build
./gradlew test
./gradlew test --tests "skadistats.clarity.TestClassName"
./gradlew publishToMavenLocal
```

Java 21 required — exhaustive switch over sealed types needs
source=target=21. Don't lower it.

## Layout gotchas
- `s1` / `s2` sub-packages are load-bearing: engine-specific code goes
  under the matching `io/`, `model/`, or `processor/` subtree.
- Wire/protobuf definitions live in the separate `clarity-protobuf`
  artifact at `/home/spheenik/projects/clarity/clarity-protobuf`.
  Don't edit generated proto classes here. To update against upstream
  Valve protos, use the tool + runbook at
  `clarity-protobuf/tools/proto-sync/` (RUNBOOK.md).
- `replays/` → `/home/spheenik/projects/replays`.

## Parsing components (orientation)
Bottom-up:

- `runner` — the driver. `SimpleRunner` / `ControllableRunner`.
- Event system — annotation-driven wiring. `ExecutionModel` resolves
  dependencies, instantiates processors, injects everything at startup.
  Backed by in-repo annotation processors in the `src/processor/`
  source set (`ProvidesIndexProcessor`, `ListenerValidationProcessor`,
  `EventGenerationProcessor`, `DecoderAnnotationProcessor`) — they
  index `@Provides`, validate listener signatures, and generate code
  at compile time so runtime avoids classpath scanning.
- `InputSourceProcessor` — sits on top. Reads the replay source and
  routes protobuf messages to subscribers.

Two-tier routing — container messages (CDemoPacket, CDemoSendTables, …)
carry packed embedded messages:
```
container → @OnMessageContainer → processEmbedded() → @OnMessage(Specific)
```

Subsystem processors subscribe to those messages:
- `sendtables` / DTClasses — entity schemas (s1 send tables, s2 flattened serializers)
- `stringtables`           — named lookup tables (instancebaseline, etc.)
- `entities`               — entity lifecycle + state; emits OnEntity{Created,Updated,Deleted}
- `gameevents`             — player_death, item_purchase, …; emits @OnGameEvent
- `modifiers`              — Dota buffs/debuffs
- `tempentities`           — temporary visual effects

## Related repos
All wired together as Gradle **composite builds** (conditional on the
sibling checkout existing) — no publish step needed during local dev.

Upstream:
- clarity-protobuf: `/home/spheenik/projects/clarity/clarity-protobuf`
  — wire defs. Pulled in via `includeBuild("../clarity-protobuf")` in
  our `settings.gradle.kts`. Bumped independently; Java 17 on purpose.

Downstream (API changes here need a compat check against both; both
composite-include this repo):
- clarity-examples: `/home/spheenik/projects/clarity/clarity-examples`
- clarity-analyzer: `/home/spheenik/projects/clarity/clarity-analyzer`
