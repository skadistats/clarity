## ADDED Requirements

### Requirement: Fluent class-scoped registration API

The `EntityBindings` processor SHALL expose a fluent DSL for declaring per-entity-class state mirrors. The DSL SHALL start with `forClass(String dtClassPattern)`, continue with `withState(Supplier<S> factory)` to pin the state type, and chain `bind(String propertyName, BiConsumer<S, T> setter)` calls for each property-to-field mirror. The chain SHALL terminate with `register()`, which finalizes the registration. Calls to `bind`, `follow`, `onDestroy`, etc. made after `register()` SHALL have no effect on that registration.

#### Scenario: Minimal singleton class declaration

- **WHEN** a user writes `bindings.forClass("CC4").withState(Bomb::new).bind("m_vecOrigin", Bomb::setPos).register();`
- **THEN** at each `CC4` entity creation, the binder SHALL instantiate a new `Bomb`, seed it with the current `m_vecOrigin` value, and subsequently invoke `Bomb::setPos` on every `m_vecOrigin` change for that entity

#### Scenario: Multiple binds in a single chain

- **WHEN** a user writes `forClass(...).withState(X::new).bind("a", X::setA).bind("b", X::setB).bind("c", X::setC).register();`
- **THEN** all three property-to-field mirrors SHALL be active for every matching entity

#### Scenario: Missing register silently skips

- **WHEN** a user writes the chain but omits the terminal `.register()` call
- **THEN** no bindings are installed and no state objects are created (documented behavior, not an error)

### Requirement: Handle-follow as a first-class operator

The DSL SHALL support `.follow(String handleProperty)` as a chainable operator that scopes subsequent `.bind()` calls to the entity referenced by the handle. When the handle value changes, the binder SHALL unsubscribe from the old target entity's properties, re-subscribe to the new target entity's properties, and seed the state with the new target's current values.

#### Scenario: Controller-to-pawn composition

- **WHEN** a user writes `forClass("CCSPlayerController").withState(Player::new).bind("m_iszPlayerName", Player::setName).follow("m_hPawn").bind("m_iHealth", Player::setHealth).bind("m_vecOrigin", Player::setPos).register();`
- **THEN** the Player state receives `m_iszPlayerName` updates from the controller and `m_iHealth`/`m_vecOrigin` updates from the pawn referenced by `m_hPawn`

#### Scenario: Respawn replaces the followed entity

- **WHEN** the controller's `m_hPawn` handle changes from pawn A to pawn B (e.g., because pawn A was destroyed on respawn)
- **THEN** the binder unregisters its handlers on pawn A, registers equivalent handlers on pawn B, and immediately seeds the Player state with pawn B's current `m_iHealth` and `m_vecOrigin` values

#### Scenario: Handle resolves to no entity

- **WHEN** the handle value does not resolve to a live entity (e.g., controller exists, pawn has not yet been created, or pawn was destroyed without replacement)
- **THEN** no setters are invoked for followed properties; the state retains its previous values for those fields until a valid pawn appears

### Requirement: State factory and per-instance capture

Each matching entity SHALL receive its own state object instance, created by calling the `Supplier<S>` passed to `withState`. The per-instance state SHALL be captured by the binding closures, such that property updates for entity A write to the state object for entity A only.

#### Scenario: Two instances of the same class get separate state

- **WHEN** two `CBombsite` entities exist in the same match
- **THEN** the binder creates two separate `BombSite` state objects, and property updates to one bombsite entity do not affect the other bombsite's state

#### Scenario: State survives handle-follow re-subscription

- **WHEN** a `.follow("m_hPawn")` re-subscription occurs
- **THEN** the same state object instance continues to be used (only the source of followed property updates changes); no new state object is created

### Requirement: Destruction hook

The DSL SHALL support `.onDestroy(Consumer<S> cleanup)` for running user code when the entity is destroyed, before the binder drops its references to the state object.

#### Scenario: Cleanup runs on entity deletion

- **WHEN** a registered entity is deleted (`@OnEntityDeleted` fires for it)
- **THEN** the binder invokes the registered `onDestroy` consumer with the state object, and afterwards releases all internal references to the state object

#### Scenario: Multiple onDestroy consumers

- **WHEN** a user chains `.onDestroy(a).onDestroy(b).register();`
- **THEN** both `a` and `b` are invoked in registration order on entity destruction

### Requirement: State enumeration accessor

The binder SHALL expose `states(Class<S> stateType)` returning a live `Collection<S>` view of all currently-alive state objects of the requested type across all registered entities.

#### Scenario: Consumer enumerates all live players

- **WHEN** a consumer calls `bindings.states(Player.class)` at any tick
- **THEN** the returned collection contains one `Player` object for each currently-alive entity registered with a `PlayerState`-typed state supplier

#### Scenario: Enumeration reflects creation and destruction

- **WHEN** an entity is created, then later destroyed
- **THEN** the state object appears in the collection between its creation and destruction and is absent before/after

### Requirement: Lookup by entity

The binder SHALL expose `stateFor(Entity entity, Class<S> stateType)` returning the state object bound to the given entity for the given state type, or `null` if none.

#### Scenario: State retrievable for a known entity

- **WHEN** a consumer holds an `Entity` reference and calls `bindings.stateFor(e, Player.class)`
- **THEN** it receives the same `Player` instance the binder created for that entity at `@OnEntityCreated` time, or `null` if `e` has no `Player` binding

### Requirement: Eager push dispatch semantics

Property updates SHALL invoke registered setters synchronously during clarity's `@OnEntityPropertyChanged` dispatch. Consumer reads of state-object fields SHALL return live values without triggering any re-evaluation or computation.

#### Scenario: Field read after update sees new value

- **WHEN** a property update fires and the bound setter writes to a state field, and immediately thereafter a consumer reads that field
- **THEN** the read returns the freshly-written value

#### Scenario: Field read without preceding update returns last value

- **WHEN** no update has fired for a property since the last write
- **THEN** reading the state field returns the last-written value (no lazy recomputation, no sentinel)

### Requirement: Package location

The `EntityBindings` processor and its fluent builder classes SHALL live under the `shared` subproject at `skadistats.clarity.examples.shared.bindings`. The binder SHALL be reachable from any content subproject (`examples`, `repro`, `dev`, `bench`) via the existing `implementation(project(":shared"))` dependency.

#### Scenario: Import from an example

- **WHEN** a developer writes an example under `examples/src/main/java/.../Main.java` and imports `skadistats.clarity.examples.shared.bindings.EntityBindings`
- **THEN** the import resolves and the class is visible, because `examples` depends on `:shared`
