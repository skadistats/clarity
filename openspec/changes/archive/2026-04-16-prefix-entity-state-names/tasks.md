## 1. Rename S1 state class

- [x] 1.1 Rename `ObjectArrayEntityState` → `S1ObjectArrayEntityState` (file + class + all references) via IDE refactor
- [x] 1.2 Run `./gradlew compileJava` in `clarity` — verify clean compile

## 2. Rename S2 state classes

- [x] 2.1 Rename `AbstractS2EntityState` → `S2AbstractEntityState` via IDE refactor
- [x] 2.2 Rename `FlatEntityState` → `S2FlatEntityState` via IDE refactor
- [x] 2.3 Rename `NestedArrayEntityState` → `S2NestedArrayEntityState` via IDE refactor
- [x] 2.4 Rename `NestedArrayEntityStateIterator` → `S2NestedArrayEntityStateIterator` via IDE refactor
- [x] 2.5 Rename `TreeMapEntityState` → `S2TreeMapEntityState` via IDE refactor
- [x] 2.6 Rename `NestedEntityState` (interface) → `S2NestedEntityState` via IDE refactor
- [x] 2.7 Run `./gradlew compileJava` in `clarity` — verify clean compile after every rename

## 3. Verify tests and comments

- [x] 3.1 Run `./gradlew test` in `clarity` — verify all tests pass
- [x] 3.2 Grep `src/` for any remaining occurrences of the old short names (`FlatEntityState`, `NestedArrayEntityState`, `TreeMapEntityState`, `ObjectArrayEntityState`, `AbstractS2EntityState`, `NestedEntityState`) not preceded by `S1`/`S2`; fix stale javadoc / inline comments
- [x] 3.3 Run `./gradlew jmh` compileOnly or equivalent to confirm JMH harness still compiles

## 4. Verify downstream consumers

- [x] 4.1 Compile `clarity-examples` against the renamed `clarity` — `./gradlew :examples:compileJava :dev:compileJava :repro:compileJava :bench:compileJava`
- [x] 4.2 Compile `clarity-analyzer` at `/home/spheenik/projects/clarity/clarity-analyzer` — verify clean compile (fixed one `AbstractS2EntityState` reference in `ObservableEntity.java`)
- [x] 4.3 Compile `clarity-cp0-bench` — verify clean compile (project not found, skipped)

## 5. Verify spec coherence

- [x] 5.1 Grep `openspec/specs/` for any remaining occurrences of the old short names not preceded by `S1`/`S2` — expect zero matches after delta archive
- [x] 5.2 Run `openspec validate prefix-entity-state-names` — confirm green

## 6. Smoke test

- [x] 6.1 Run a representative example (e.g. `./gradlew :examples:allchatRun --args "<replay>"`) against a known-good Dota 2 replay — verify no runtime breakage from the rename
