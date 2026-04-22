## 1. EntityBindings framework (shared subproject)

- [ ] 1.1 Create `shared/src/main/java/skadistats/clarity/examples/shared/bindings/` package.
- [ ] 1.2 Define public fluent builder types: `EntityBindings` (top-level processor), `Registration0` (post-`forClass`), `Registration<S>` (post-`withState`), `FollowingRegistration<S>` (post-`follow`). Include the generic `bind(String, BiConsumer<S, T>)`, `follow(String)`, `onDestroy(Consumer<S>)`, and `register()` methods.
- [ ] 1.3 Implement registration storage: `IdentityHashMap<DTClass, List<ClassBinding<?>>>` (resolved lazily on first matching entity) + `Int2ObjectMap<List<EntityBindingInstance>>` keyed by entity index.
- [ ] 1.4 Wire `@OnEntityCreated` dispatch: match entity's `DTClass` against registered class patterns, instantiate state via `Supplier<S>`, seed values by reading current property values through `entity.getPropertyForFieldPath(...)`, store the instance record keyed by entity index.
- [ ] 1.5 Wire `@OnEntityPropertyChanged` dispatch: look up entity's binding list by index, find matching property setters (directly-bound or followed), invoke each with the new value.
- [ ] 1.6 Implement handle-follow: on update to a followed handle property, resolve new handle → entity via the engine type's handle-to-index mapping, unsubscribe followed setters from old target, subscribe to new target, seed state with new target's current values.
- [ ] 1.7 Wire `@OnEntityDeleted` dispatch: invoke registered `onDestroy` consumers in registration order, release all references to the state object, drop the entry from the per-entity map. If a delete arrives for an entity that is currently a follow-target for another binding, unregister the follower's followed setters on that target (state object of the follower is preserved).
- [ ] 1.8 Implement `states(Class<S>)` returning a live `Collection<S>` view over currently-registered instances of that state type.
- [ ] 1.9 Implement `stateFor(Entity, Class<S>)` returning the bound state object or `null`.
- [ ] 1.10 Verify the binder compiles and runs under Java 21 (project CLAUDE.md states Java 21).
- [ ] 1.11 Manual smoke test: write a tiny throwaway Main under `shared/` (or in a temporary test fixture) that binds `CCSPlayerController.m_iszPlayerName` to a single field and confirms it populates when run against a known CS2 demo.

## 2. CS2 GameState POJOs (examples subproject)

- [ ] 2.1 Create `examples/src/main/java/skadistats/clarity/examples/cs2state/` directory.
- [ ] 2.2 Create `model/` sub-package with POJO classes: `GameState`, `Bomb`, `Player`, `PlayerTeam` (enum), `Weapon`, `WeaponClass` (enum), `GameRules`, `TeamState`, `GrenadeProjectile`, `Inferno`, `Hostage`, `Vector`, `Handle`.
- [ ] 2.3 Provide plain field accessors/setters on each POJO consistent with method-reference usage (e.g., `Player::setHealth`, `Bomb::setPos`).
- [ ] 2.4 Include a minimal `toString()` on each POJO for debugging snapshot output.

## 3. CS2 entity wiring (examples subproject)

- [ ] 3.1 Create `wiring/CS2StateWiring.java` — a clarity processor that depends on `EntityBindings` via `ctx.getProcessor(EntityBindings.class)` and performs all `forClass(...).register()` calls for CS2 during `@OnDTClassesComplete`.
- [ ] 3.2 Port `bindBomb` (CS2 datatables.go:29-252): CC4 carrier, position, ticking, plant-begin detection; CPlantedC4 position, defuser, defuse-state, ticking.
- [ ] 3.3 Port `bindPlayers`: CCSPlayerController (name, team, score, connected); `follow("m_hPawn")` → CCSPlayerPawn (health, armor, position, isAlive, active weapon handle, flash duration, scoped/ducking flags).
- [ ] 3.4 Port `bindWeapons`: weapon class detection, owner handle, clip, reserve ammo.
- [ ] 3.5 Port `bindTeamStates`: T and CT team score, clan name, from `CCSTeam`.
- [ ] 3.6 Port `bindGameRules`: round time, freezetime, warmup, game phase, match started, overtime count, total rounds played, from `CCSGameRulesProxy.m_pGameRules`.
- [ ] 3.7 Port grenade projectiles (CBaseCSGrenadeProjectile, CMolotovProjectile, CSmokeGrenadeProjectile, etc.): thrower, position, trajectory lifecycle.
- [ ] 3.8 Port `bindHostages`: state, rescuer, position.
- [ ] 3.9 Port `bindBombSites`: bombsite A and B centers and mins/maxs from the trigger entities.

## 4. Synthetic Source 1 events (examples subproject)

- [ ] 4.1 Create `events/` sub-package with event classes: `OnBombPickup`, `OnBombDropped`, `OnBombPlanted`, `OnBombDefused`, `OnWeaponFire` (annotation + Event class pattern, following `lifestate` example).
- [ ] 4.2 Create `events/CS2SyntheticEvents.java` — a processor with `@Provides({...})` that watches old-vs-new transitions on bound state fields and raises the synthetic events. Use the `EntityBindings` state via `states(Bomb.class)` / `states(Player.class)` / `states(Weapon.class)` to detect transitions rather than re-listening to raw property changes.
- [ ] 4.3 Implement `BombPickup` / `BombDropped` detection via `Bomb.carrier` transitions.
- [ ] 4.4 Implement `BombPlanted` detection via CPlantedC4 entity appearance.
- [ ] 4.5 Implement `BombDefused` detection via `Bomb.isDefused` becoming true.
- [ ] 4.6 Implement `WeaponFire` detection via weapon clip-count decrement.

## 5. Demo application (examples subproject)

- [ ] 5.1 Create `Main.java` using `ReplayChooser` for path resolution, `SimpleRunner`, and processors `EntityBindings` + `CS2StateWiring` + `CS2SyntheticEvents`.
- [ ] 5.2 Add an event-driven scenario: subscribe to `OnBombPickup`, `OnBombPlanted`, and any native `@OnGameEvent` for kills; print one line per event including player names resolved from `GameState`.
- [ ] 5.3 Add a tick-end snapshot scenario: on round-end detection, print alive-player count and score from the live `GameState`.
- [ ] 5.4 Add `cs2state/README.md` documenting: what the example demonstrates, known-good demo path used for validation, schema-drift expectation, pointer to the benchmark in `bench/`.

## 6. Benchmark harness (bench subproject)

- [ ] 6.1 Create `bench/src/main/java/skadistats/clarity/examples/bench/cs2statebench/` directory.
- [ ] 6.2 Create `Main.java` that instantiates the same processor graph as `cs2state` Main (EntityBindings + CS2StateWiring + CS2SyntheticEvents), subscribes no-op handlers to all synthetic event types, and times three iterations inside `runner.runWith(...)`.
- [ ] 6.3 Print each iteration's wall-clock in ms and a "best: <ms>" line at the end.
- [ ] 6.4 Confirm `:bench:cs2stateBenchRun` and `:bench:cs2stateBenchPackage` tasks are generated by the existing `examples-convention` plugin without extra configuration.

## 7. Validation and measurement

- [ ] 7.1 Run the example Main against a known-good CS2 demo; spot-check at least two ground-truth values (e.g., final score, one player's kill count) against expected values.
- [ ] 7.2 Run the benchmark against the 3dmax-vs-falcons-m1-anubis.dem demo (same demo as PARSER-COMPARISON.md); record the best-of-3 wall-clock.
- [ ] 7.3 Create `bench/cs2state-bench-results.md` with the measured number, demo path, JVM version, host summary, and the comparison against demoinfocs's 5.17s ST baseline.
- [ ] 7.4 Add a follow-up note (in the results file or the example README) identifying the need to update `PARSER-COMPARISON.md` in the clarity core repo.

## 8. Documentation updates

- [ ] 8.1 Update `CLAUDE.md` in clarity-examples to mention the new `cs2state` example under the examples list and the `EntityBindings` component under `shared/`.
- [ ] 8.2 Update the repo `README.md` to mention `cs2state` under the examples list.
- [ ] 8.3 Verify no other example or doc accidentally references `cs2state` or `EntityBindings` incorrectly.
