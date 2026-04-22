## Why

Writing a stateful CS2 or Dota 2 replay analyzer on top of clarity currently requires boilerplate: a `Map<EntityId, MyStateClass>`, three `@OnEntityCreated`/`@OnEntityPropertyChanged`/`@OnEntityDeleted` handlers per entity class, manual slot/array bookkeeping, and manual handle-following for the common controller→pawn composition in CS2. demoinfocs-golang demonstrates an ergonomically superior model: declarative entity bindings that maintain a unified POJO game-state tree with per-instance captured state, plus synthetic Source 1 event reconstruction. We want the same pattern available to clarity users, validated by a faithful port of demoinfocs's CS2 wiring.

Secondarily, PARSER-COMPARISON.md currently attributes "~3.5s of demoinfocs's 5.17s ST runtime" to CS2 state-machine work versus bare decode, but explicitly caveats this as a rough inference rather than a measurement. A clarity port of demoinfocs's `bindEntities` gives us the equivalent-scope measurement.

## What Changes

- Add a game-agnostic **EntityBindings** framework to the `shared` subproject: fluent DSL for declaring per-entity-class state mirrors. Primitives: `forClass(pattern).withState(factory).bind(property, setter).follow(handleProperty).onDestroy(cleanup)`. Handle-follow is first-class for controller→pawn composition. Eager push semantics: setters fire synchronously on property update, consumer reads are plain Java field access against live POJOs.
- Add a new **cs2state** example under `examples/` that uses EntityBindings to reconstruct a unified `GameState` object tree (Bomb, Players[10], GameRules, TeamStates, Weapons, GrenadeProjectiles, Infernos, Hostages) from CS2 entity streams. The example ports demoinfocs-golang's `datatables.go` wiring to clarity's world, including synthetic Source 1 event reconstruction (`BombPickup`/`BombDropped`/`WeaponFire` etc.) via old-vs-new transition detection.
- Add a **cs2state-bench** benchmark under `bench/` that runs the CS2 example against the anubis demo and records wall-clock, to be compared against demoinfocs's 5.17s ST baseline. Result fed back into PARSER-COMPARISON.md to convert the "~3.5s rough inference" caveat into a measurement.

No breaking changes. Pure additions.

## Capabilities

### New Capabilities
- `entity-bindings`: Game-agnostic reactive POJO-mirror framework in the shared subproject. Fluent DSL for declaring per-entity-class state mirrors, with handle-follow as a first-class cross-entity composition primitive.
- `cs2-game-state`: CS2-specific example application that uses entity-bindings to reconstruct a unified GameState object tree, plus synthetic Source 1 event reconstruction.
- `parser-comparison-bench`: Benchmark harness that compares clarity-with-full-state wall-clock against demoinfocs's sequential ST baseline on the anubis CS2 demo, feeding a measured number back into PARSER-COMPARISON.md.

### Modified Capabilities
- `project-structure`: The shared subproject gains a new public component (`EntityBindings` and supporting types); a new example subdirectory under `examples/` is added; a new benchmark subdirectory under `bench/` is added.

## Impact

- `shared/src/main/java/` — new `skadistats/clarity/examples/shared/bindings/` package containing the EntityBindings framework.
- `examples/src/main/java/skadistats/clarity/examples/cs2state/` — new example directory with Main, the GameState POJO tree, CS2 wiring code, synthetic-event logic, and supporting domain types.
- `bench/src/main/java/skadistats/clarity/examples/bench/cs2state/` — new benchmark directory. Existing bench `examples-convention` task generation picks it up automatically.
- `PARSER-COMPARISON.md` (in the clarity repo, not clarity-examples) — will be updated with the measured number once the benchmark runs. Out of scope for this change but noted as downstream follow-up.
- No changes to clarity core. Pure clarity-examples addition.
- Maintenance surface: the CS2 wiring will drift as CS2 updates change entity schemas. Breakage-tolerant as an example; not a library commitment.
