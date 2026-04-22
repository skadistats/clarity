## Context

Clarity's current entity event model is annotation-driven and processor-scoped: `@OnEntityCreated`, `@OnEntityPropertyChanged(classPattern=..., propertyPattern=...)`, `@OnEntityDeleted`. It is well-suited to "one handler, many entities" processors but forces boilerplate for stateful analyzers that want per-entity captured state: you end up maintaining a `Map<EntityId, MyState>` and a three-handler triad per tracked class.

demoinfocs-golang takes the opposite approach: on entity creation, user code is handed the `Entity` and registers closure-based callbacks per property. The closure captures the per-instance state directly, so the `Map<EntityId, MyState>` goes away. A `BindProperty(name, &var, type)` primitive mirrors a property straight into a field.

We want clarity users to have this ergonomic option as a library-free pattern (built on clarity's existing primitives), validated by a faithful port of demoinfocs's CS2 wiring that produces a unified `GameState` object tree.

Relevant constraints:

- Clarity has two entity-state storage modes (`S2FlatEntityState`, `S2NestedArrayEntityState`). Both dispatch through the same `@OnEntityPropertyChanged` event surface. No core changes required.
- Clarity's flat state always has values (baseline-seeded); there is no "property does not exist yet" condition demoinfocs defensively guards for. Most of demoinfocs's `if val.Any != nil` scaffolding becomes dead code in a direct port.
- Property values arrive boxed (`Integer`, `Float`, `Vector`, etc.) via `Entity.getPropertyForFieldPath(fp)`. No zero-cost unboxing for primitive properties in the common dispatch path.
- The shared subproject is already on every content subproject's classpath (see `project-structure` spec), so placing the binder there makes it reachable from examples, repro, dev, and bench.

## Goals / Non-Goals

**Goals:**

- An `EntityBindings` framework that collapses the `Map<EntityId, MyState>` + three-handler triad into a fluent per-class declaration.
- Handle-follow (`.follow("m_hPawn")`) as a first-class primitive, transparently re-subscribing when a handle changes.
- Per-instance state captured via closures over locals, no user-visible identity tracking.
- A CS2 `GameState` example object tree demonstrating the pattern against a realistic multi-class scenario (Bomb, Players, Weapons, GameRules, Hostages, Infernos, Grenades).
- Synthetic Source 1 event reconstruction (`BombPickup`, `BombDropped`, `WeaponFire`, etc.) demonstrating the "old-vs-new transition detection" idiom on top of the binder.
- A benchmark that produces a comparable wall-clock number against demoinfocs's 5.17s ST baseline on the anubis demo.

**Non-Goals:**

- Production-grade CS2 state tracking. The example is breakage-tolerant: as CS2 updates drift the entity schemas, the example may need manual updates, and that's fine.
- Full parity with demoinfocs's 90+ event types. Synthetic events are scoped to enough to prove the pattern (Bomb transitions + WeaponFire), not exhaustive coverage.
- A general-purpose reactive framework. The binder is deliberately scoped to "mirror a property into a POJO field, with handle-follow as the one cross-entity operator." Richer dependency graphs (derived values, multi-source combinators) are explicitly out of scope; for those, users should fall back to `ObservableEntity`-style approaches like clarity-analyzer uses.
- A Dota 2 equivalent. The pattern generalizes, but only CS2 is in scope for this change.
- Zero-allocation hot path. The binder uses `IdentityHashMap<Class, ...>` and per-entity `ArrayList<Handler>`. If profiling shows allocation pressure, that's a follow-up.

## Decisions

### 1. Place the binder in `shared/`, not clarity core

Alternatives considered:

- **(a) clarity core `Entity` API** — add `entity.onPropertyUpdate(name, handler)` as a native primitive.
- **(b) clarity-examples `shared/` as user-space code** (chosen).
- **(c) A separate module in a new repo.**

(a) imposes a second dispatch path in the core library's hot loop, with cost paid by every consumer even if they don't use it. (c) is overkill for an example-level pattern. (b) keeps the core clean, makes the binder a copyable pattern for users who don't want the dependency, and costs nothing at runtime for consumers who don't use it. `shared/` is already on every content subproject's classpath.

### 2. Fluent generic builder, state type pinned at `withState`

Alternatives considered:

- **(a) Raw `EntityBindings.register(pattern, factory, BiConsumer<Entity, S>)`** — single method, flat API.
- **(b) Fluent `.forClass(...).withState(...).bind(...).bind(...).register()`** (chosen).
- **(c) Kotlin-style DSL receiver.**

(a) forces the user to pack all bindings into one lambda body with manual `entity.onPropertyUpdate(...)` calls — no typing benefit, same boilerplate as the raw annotation approach. (c) requires Kotlin. (b) is idiomatic Java for fluent APIs, lets the compiler infer `S` once at `withState(Supplier<S>)` and pin it across subsequent `bind(String, BiConsumer<S, T>)` calls, so `PlayerState::setHealth` works as a method reference without explicit type witnesses.

### 3. Eager push, not lazy invalidation

Alternatives considered:

- **(a) Lazy pull via JavaFX-style `ObservableValue`** — re-evaluate on read.
- **(b) Eager push: setter fires synchronously on property update** (chosen).

Replay analyzers typically read widely across the state tree each tick; lazy evaluation adds dependency-tracking overhead without saving work. Eager push mirrors demoinfocs, keeps consumer reads to plain Java field access, and runs on top of clarity's existing synchronous `@OnEntityPropertyChanged` dispatch with no change to the core pipeline.

### 4. Handle-follow as a first-class primitive, not a generic combinator

Alternatives considered:

- **(a) Generic `.combine(otherEntity, otherProp, combiner)`** — supports arbitrary cross-entity dependencies.
- **(b) `.follow(handleProperty)` that scopes subsequent `.bind()` calls to the referenced entity** (chosen).
- **(c) No cross-entity support; users fall back to raw annotations for composition.**

(a) lands us in reactive-framework territory and invites every analyzer to build its own ad hoc DAG. (c) blocks the most common CS2 pattern (controller→pawn). (b) covers the controller→pawn case idiomatically while staying scoped. Non-handle cross-entity cases (e.g., Dota's `PlayerResource.m_vecPlayerTeamData[i].m_hSelectedHero`) are deliberately out of scope for this change; users who need them fall back to manual `@OnEntityPropertyChanged` or `ObservableEntity`.

### 5. Boxed values in the binder dispatch path

Alternatives considered:

- **(a) Typed primitive variants (`bindInt`, `bindFloat`, `bindVector`)** — zero-boxing hot path.
- **(b) Generic `bind(String, BiConsumer<S, T>)` with autoboxed values** (chosen).

The primary source values come from `Entity.getPropertyForFieldPath(fp)`, which already returns boxed `Object`. Adding typed variants in the binder doesn't remove boxing — it only moves it. Go with generic now, revisit only if profiling shows allocation pressure.

### 6. Synthetic events modelled as regular clarity events via `@Provides`

Alternatives considered:

- **(a) The binder dispatches events through its own event bus.**
- **(b) Synthetic events piggyback on clarity's existing event system via `@Provides({MyEvent.class})` and `Event<MyEvent>.raise(...)`** (chosen).

(b) means user code subscribes to synthetic events (`@OnBombPickup`, `@OnWeaponFire`) via the same annotation pattern as native events, with zero new infrastructure. The binder stays simple; event creation is a separate processor in the CS2 example.

### 7. Entity-indexed storage, not per-Entity object reference

Alternatives considered:

- **(a) Attach the binder's handler list to the `Entity` object itself** — requires adding a slot to core.
- **(b) `Int2ObjectMap<List<Binding>>` keyed by entity index, owned by the binder** (chosen).

(a) requires core changes. (b) keeps everything in user space at the cost of a map probe per property update. The map can be `IdentityHashMap`-like using the entity index directly, which JVMs optimize well.

### 8. Example location: `examples/cs2state/` (not `dev/` or `bench/`)

`cs2state` is demonstrative user-facing code illustrating an ergonomic pattern. It is not a maintainer diagnostic (`dev/`) or a throughput benchmark (`bench/`). It belongs under `examples/` per the `project-structure` capability's category rules. The benchmark harness (a separate concern) lives under `bench/cs2state-bench/` and consumes the example's wiring.

### 9. Benchmark methodology mirrors PARSER-COMPARISON.md

The benchmark runs against the same `3dmax-vs-falcons-m1-anubis.dem` demo as the existing comparison, takes best-of-3 wall-clock, times inside `runner.runWith(...)` (excluding JVM startup, including JIT warmup), and is invoked via a `:bench:cs2stateBenchRun` Gradle task. The raw number goes into the benchmark's own README or log output; updating PARSER-COMPARISON.md itself is out of scope (that file lives in the clarity core repo, not clarity-examples).

## Risks / Trade-offs

- **Risk**: The ported wiring produces incorrect game state due to a mis-translated property name or handle-follow semantic difference between demoinfocs and clarity.
  - **Mitigation**: Include a sanity-check Main that dumps `GameState` snapshots at selected ticks and manually cross-reference one or two values (e.g., player 0's health at a specific tick) against what the same replay reports via demoinfocs or the in-game observer.

- **Risk**: The benchmark shows clarity-with-state significantly slower than demoinfocs's 5.17s ST, undermining the "clarity is faster" narrative in PARSER-COMPARISON.md.
  - **Mitigation**: That would itself be a valuable finding. The doc would be updated with the honest measurement rather than the current rough inference. The "clarity has faster per-core decode" claim rests on `entityrun`, not on state-maintenance workload.

- **Risk**: Autoboxing on hot fields (position vectors updated every tick per entity) dominates wall-clock, making the binder-based path look unfairly slow.
  - **Mitigation**: Accept for this change; if it's a real problem, it's a follow-up optimization (typed primitive bind variants) and doesn't invalidate the pattern.

- **Risk**: CS2 entity schema drift — Valve renames a property or restructures a class between a replay and today.
  - **Mitigation**: The example is explicitly breakage-tolerant; schema drift is a known maintenance cost and we accept it. Document the expectation in the example's README.

- **Trade-off**: Fluent builder requires the user to write `.register()` at the end; forgetting it silently no-ops. Considered using a lambda-scoped `forClass(pattern, cfg -> cfg.withState(...).bind(...))` pattern to avoid this, but rejected for readability — the linear fluent chain reads better.

- **Trade-off**: Handle-follow is expressible only for single-level indirection (`.follow("m_hPawn")`). Multi-level follows (pawn → weapon services → active weapon) require nested `.follow()` calls, which the generic builder must support. If nesting turns out to be fragile, scope back to single-level and document the limitation.

## Migration Plan

Not applicable — this is a pure addition. No existing clarity-examples code is modified.

## Open Questions

- Should the binder expose a `states(Class<S>)` accessor that returns a live `Collection<S>` of all currently-bound states of a type? Nice-to-have for consumer queries ("all alive players"), but can be deferred. Decision: include in the initial binder to avoid users hand-rolling the equivalent.
- Should synthetic events live in a separate sub-package of the CS2 example, or inline with the state-building processor? Decision: separate sub-package (`events/`) for readability; the example is already going to be ~1000 LOC.
- Should the CS2 example's `Main` demonstrate consuming `GameState` via `@OnTickEnd` (state snapshot per tick) or via direct property-change events? Decision: both, in two small demo scenarios — one shows "dump round summary at round end," the other shows "print kill announcements as they happen."
