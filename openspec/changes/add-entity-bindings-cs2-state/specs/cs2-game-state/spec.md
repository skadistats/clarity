## ADDED Requirements

### Requirement: GameState object tree

The `cs2state` example SHALL provide a `GameState` root object whose fields mirror the top-level structure of demoinfocs-golang's `GameState` interface, exposing live references to `Bomb`, `Player[10]`, `GameRules`, `TeamState` (T and CT), and collections for `Weapon`, `GrenadeProjectile`, `Inferno`, and `Hostage` entities.

#### Scenario: Consumer reads current bomb carrier at any tick

- **WHEN** a consumer calls `gameState.getBomb().getCarrier()` during a tick where a player holds the C4
- **THEN** the call returns the `Player` reference (or an identifier resolvable to one) for the current carrier, with no need for manual entity lookup

#### Scenario: Consumer iterates live players

- **WHEN** a consumer iterates `gameState.getPlayers()` during an active round
- **THEN** each non-null slot contains a `Player` with populated `name`, `team`, `health`, `armor`, `position`, `activeWeapon`, and other documented fields consistent with the current replay tick

### Requirement: Player composition via controller-and-pawn follow

The Player bindings SHALL use `EntityBindings.follow("m_hPawn")` to compose data from `CCSPlayerController` (name, team, connection state, score) with data from the pawn it references (`CCSPlayerPawn`: position, health, armor, active weapon, flash duration, scoped/ducking/walking flags).

#### Scenario: Respawn preserves Player identity

- **WHEN** a round starts and a player's pawn entity is replaced
- **THEN** the same `Player` state object continues to represent the player; the controller-sourced fields are stable, the pawn-sourced fields reflect the new pawn's current values

#### Scenario: Player disconnects

- **WHEN** a `CCSPlayerController` entity is deleted (player disconnect)
- **THEN** the corresponding slot in `gameState.getPlayers()` becomes `null` (or is marked disconnected, implementation choice) and subsequent iteration skips it

### Requirement: Bomb composition across pre-plant and post-plant classes

The Bomb bindings SHALL support two source classes: `CC4` (pre-plant, held or dropped) and `CPlantedC4` (post-plant, armed on the ground). Both SHALL write into the same `Bomb` state object, with the currently-active source determining which fields receive updates.

#### Scenario: Plant transitions source entity

- **WHEN** a `CPlantedC4` entity appears (plant event)
- **THEN** the `Bomb` state's `isPlanted` flag becomes true, `carrier` becomes null, and subsequent position and defuser updates come from the `CPlantedC4` entity rather than the `CC4` entity

#### Scenario: Defuse updates defuser reference

- **WHEN** `CPlantedC4.m_hBombDefuser` changes to a non-empty handle
- **THEN** the `Bomb` state's `defuser` field is updated to reference the corresponding Player

### Requirement: Synthetic Source 1 event reconstruction

The example SHALL include a processor that raises synthetic clarity events reconstructing demoinfocs-style Source 1 semantics on top of S2 entity updates. Scope for this change: `BombPickup`, `BombDropped`, `BombPlanted`, `BombDefused`, `WeaponFire`. Event types SHALL be declared via `@Provides({...})` and user code SHALL subscribe via `@On<EventName>` annotations consistent with clarity's existing custom-event pattern (see the `lifestate` example).

#### Scenario: Bomb pickup detected from ownership transition

- **WHEN** a `CC4.m_hOwnerEntity` changes from an invalid handle (no carrier) to a valid handle pointing at a player
- **THEN** a `BombPickup` event is raised with the picking-up Player

#### Scenario: Bomb drop detected from ownership transition

- **WHEN** a `CC4.m_hOwnerEntity` changes from a valid carrier handle to an invalid handle (no carrier)
- **THEN** a `BombDropped` event is raised with the Player who was previously carrying

#### Scenario: Weapon fire detected from ammo decrement

- **WHEN** a weapon entity's fired-ammo property (implementation-selected, e.g., `m_iClip1`) decrements
- **THEN** a `WeaponFire` event is raised with the weapon and its current owner

### Requirement: Example location and packaging

The `cs2state` example SHALL live under `examples/src/main/java/skadistats/clarity/examples/cs2state/`. It SHALL provide a `Main.java` entry point that accepts a CS2 demo path as its first argument (via `ReplayChooser`) and uses `SimpleRunner` to parse the demo, maintaining the `GameState` tree throughout.

#### Scenario: Unqualified Gradle task runs the example

- **WHEN** a user runs `./gradlew cs2stateRun --args="replays/csgo/s2/..."`
- **THEN** the example parses the demo and prints a minimal summary (e.g., player names, final score, total kills) derived from the live `GameState`

#### Scenario: Packaged jar is self-contained

- **WHEN** a user runs `./gradlew cs2statePackage` and `java -jar examples/build/libs/cs2state.jar <demo>`
- **THEN** the jar runs without requiring external classpath entries, consistent with other example packaging

### Requirement: Demo scenarios in Main

`Main.java` SHALL demonstrate two consumption patterns: (a) an event-subscription scenario that prints synthetic events as they occur (e.g., `BombPickup`, `Kill`, `WeaponFire`), and (b) a tick-end snapshot scenario that prints a round summary at each `RoundEnd`-equivalent moment using plain Java access to the `GameState` tree.

#### Scenario: Event-driven output

- **WHEN** `Main` runs against a typical competitive CS2 demo
- **THEN** standard output includes at least one `BombPickup` line and one `BombPlanted` line with player names derived from the bound `GameState`

#### Scenario: Snapshot-driven output

- **WHEN** a round completes
- **THEN** a summary line is printed listing alive players (and a count) derived from enumerating `gameState.getPlayers()` and checking a boolean `isAlive` or `health > 0` field

### Requirement: Breakage tolerance

The `cs2state` example is declared breakage-tolerant with respect to CS2 game updates. Its README SHALL state that the entity schema it binds against is subject to drift as Valve ships CS2 updates, and that maintaining the example against current demos is a manual effort, not a library guarantee.

#### Scenario: README documents drift expectation

- **WHEN** a contributor opens `examples/src/main/java/.../cs2state/README.md` (or equivalent doc location)
- **THEN** the README explicitly notes the schema-drift risk and names a known-good demo path used for validation
