## MODIFIED Requirements

### Requirement: Category assignment

Every example directory SHALL live in exactly one of the four content subprojects (`examples`, `repro`, `dev`, `bench`). The `shared` subproject SHALL host reusable components consumed by the content subprojects; it SHALL NOT itself contain per-example directories.

#### Scenario: Docs examples land in examples/

- **WHEN** a contributor looks up a teaching example such as `allchat`, `combatlog`, `cooldowns`, `cs2state`, `dumpmana`, `gameevent`, `header`, `info`, `lifestate`, `livesource`, `matchend`, `metadata`, `modifiers`, `particles`, `position`, `propertychange`, `resources`, `seek`, `s1tempentities`, `s2dotatempentities`, `s2effectdispatch`, `spawngroups`, or `tick`
- **THEN** it is located under `examples/src/main/java/skadistats/clarity/examples/<name>/`

#### Scenario: Issue reproducers land in repro/

- **WHEN** a contributor looks up an issue reproducer such as `issue289` or `issue350`
- **THEN** it is located under `repro/src/main/java/skadistats/clarity/examples/repro/<name>/`

#### Scenario: Diagnostic tools land in dev/

- **WHEN** a contributor looks up a maintainer diagnostic tool such as `csgo2test`, `dtinspector`, `dump`, `dumpbaselines`, `entityrun`, `fullpacketcount`, `ntsemantics`, `packetentitiesmatch`, `packetentitiesprobe`, `serializers`, `stringtabledump`, or `test`
- **THEN** it is located under `dev/src/main/java/skadistats/clarity/examples/dev/<name>/`

#### Scenario: Benchmarks land in bench/

- **WHEN** a contributor looks up a throughput benchmark such as `cs2statebench`, `entitybaseline`, `eventdispatchbench`, or `propertychangebench`
- **THEN** it is located under `bench/src/main/java/skadistats/clarity/examples/bench/<name>/`

#### Scenario: Shared subproject hosts reusable components

- **WHEN** a contributor opens `shared/src/main/java/skadistats/clarity/examples/shared/`
- **THEN** it contains components reused by the content subprojects (for example, `ReplayChooser`, `bindings/EntityBindings`) rather than per-example directories
