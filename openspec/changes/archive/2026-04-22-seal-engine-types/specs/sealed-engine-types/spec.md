## ADDED Requirements

### Requirement: DTClass is a sealed sum type

The `DTClass` interface SHALL be declared `sealed` and SHALL permit exactly `S1DTClass` and `S2DTClass`. The default methods `s1()`, `s2()`, and `evaluate(Function<S1DTClass, V>, Function<S2DTClass, V>)` SHALL be removed. Callers that need engine-specific behaviour SHALL dispatch via `switch` over the sealed permitted types or `instanceof` pattern matching.

#### Scenario: Sealed declaration closes the hierarchy

- **WHEN** the codebase is inspected
- **THEN** `DTClass` is declared `public sealed interface DTClass permits S1DTClass, S2DTClass`
- **AND** no `evaluate(Function, Function)` method exists on `DTClass`
- **AND** no `s1()` or `s2()` default methods exist on `DTClass`

#### Scenario: Exhaustive switch over DTClass

- **WHEN** internal code needs to select behaviour based on the engine of a `DTClass` reference
- **THEN** it uses `switch (dtClass) { case S1DTClass s1 -> …; case S2DTClass s2 -> …; }`
- **AND** the compiler enforces exhaustiveness without requiring a `default` branch
- **AND** a `default` branch SHALL NOT be written (adding one disables exhaustiveness checking)

### Requirement: FieldPath is a sealed sum type

The `FieldPath` interface SHALL be declared `sealed` and SHALL permit exactly `S1FieldPath` and `S2FieldPath`. The default methods `s1()` and `s2()` SHALL be removed. `FieldPath` SHALL remain a valid opaque handle for user-facing code: callers that do not need engine-specific structure SHALL continue to pass `FieldPath` values through `Entity` accessors without casting.

#### Scenario: Sealed declaration closes the hierarchy

- **WHEN** the codebase is inspected
- **THEN** `FieldPath` is declared `public sealed interface FieldPath permits S1FieldPath, S2FieldPath`
- **AND** no `s1()` or `s2()` default methods exist on `FieldPath`

#### Scenario: User-facing opaque-handle contract preserved

- **WHEN** user example code receives a `FieldPath` from `@OnEntityPropertyChanged` or `EntityState.fieldPathIterator()`
- **THEN** it can pass the value to `Entity.getNameForFieldPath(FieldPath)` and `Entity.getPropertyForFieldPath(FieldPath)` without casting
- **AND** no example under `clarity-examples/` requires source modification as a result of this change

### Requirement: EntityState is a sealed sum type

The `EntityState` interface SHALL be declared `sealed` and SHALL permit exactly `S1EntityState` and `S2EntityState`. Shared read operations — `copy()`, `fieldPathIterator()`, `getValueForFieldPath(FieldPath)`, and `dump(String, Function)` — SHALL remain on the base. Engine-specific write operations — `write`, `decodeInto`, `applyMutation` — SHALL move to the sealed sub-interfaces with engine-typed `FieldPath` arguments (see `state-mutation` delta).

`S1EntityState` SHALL be a sealed interface extending `EntityState` and permitting the concrete S1 state implementations (`S1FlatEntityState`, `S1ObjectArrayEntityState`). `S2EntityState` SHALL be a sealed interface extending `EntityState` and permitting the concrete S2 state implementations (`S2FlatEntityState`, `S2NestedArrayEntityState`, `S2TreeMapEntityState`).

#### Scenario: Sealed declaration closes the EntityState hierarchy

- **WHEN** the codebase is inspected
- **THEN** `EntityState` is declared `public sealed interface EntityState permits S1EntityState, S2EntityState`
- **AND** `S1EntityState` is declared `public sealed interface S1EntityState extends EntityState permits …`
- **AND** `S2EntityState` is declared `public sealed interface S2EntityState extends EntityState permits …`

#### Scenario: Shared reads stay on the base

- **WHEN** user code holds an `EntityState` reference returned from `Entity.getState()`
- **THEN** it can call `getValueForFieldPath(FieldPath)`, `fieldPathIterator()`, `copy()`, and `dump()` without casting to a sub-interface

#### Scenario: Engine-typed writes on sub-interfaces

- **WHEN** `S1EntityState` is inspected
- **THEN** it declares `write(S1FieldPath fp, Object decoded)`, `decodeInto(S1FieldPath fp, Decoder d, BitStream bs)`, and `applyMutation(S1FieldPath fp, StateMutation m)`
- **AND** `S2EntityState` declares the same methods with `S2FieldPath` in place of `S1FieldPath`
- **AND** the base `EntityState` interface does NOT declare any `write`, `decodeInto`, or `applyMutation` method

### Requirement: FieldReader narrows engine types at the readFields boundary

`S1FieldReader.readFields(BitStream, DTClass, EntityState, boolean, boolean)` and `S2FieldReader.readFields(BitStream, DTClass, EntityState, boolean, boolean)` SHALL cast the incoming `DTClass` and `EntityState` arguments to their engine-specific types exactly once at the start of the method. All subsequent per-field operations within the method SHALL invoke methods on the engine-typed references without any additional casts on `DTClass`, `EntityState`, or `FieldPath`.

Each `FieldReader` subclass SHALL store its internal `fieldPaths[]` work array typed as the engine-specific `FieldPath` subtype (`S1FieldPath[]` or `S2FieldPath[]`), eliminating per-iteration casts in the read loop.

#### Scenario: S1FieldReader casts once at entry

- **WHEN** `S1FieldReader.readFields` is invoked
- **THEN** the method body begins with `var dtClass = (S1DTClass) dtClassGeneric;` and `var state = (S1EntityState) stateGeneric;`
- **AND** the per-field loop contains no `.s1()` calls and no `(S1FieldPath)` casts
- **AND** `state.write(fp, …)` / `state.decodeInto(fp, …)` resolve statically to `S1EntityState` methods

#### Scenario: S2FieldReader casts once at entry

- **WHEN** `S2FieldReader.readFields` / `readFieldsFast` is invoked
- **THEN** the method body begins with `var dtClass = (S2DTClass) dtClassGeneric;` and `var state = (S2EntityState) stateGeneric;`
- **AND** the per-field loop contains no `.s2()` calls and no `(S2FieldPath)` casts
- **AND** `state.write(fp, …)` / `state.decodeInto(fp, …)` resolve statically to `S2EntityState` methods

#### Scenario: Internal fieldPaths array is engine-typed

- **WHEN** `S1FieldReader` or `S2FieldReader` is inspected
- **THEN** the inherited `fieldPaths[]` array is declared or accessed as `S1FieldPath[]` / `S2FieldPath[]` respectively
- **AND** indexing into the array yields a value assignable to the engine's `FieldPath` subtype without cast

### Requirement: Build toolchain is JDK 21, bytecode release is 17

`clarity/build.gradle.kts` SHALL declare a toolchain of `JavaLanguageVersion.of(21)` and SHALL set `options.release = 17` on Java compile tasks. The published artifacts SHALL remain runnable on Java 17 JVMs. No library APIs introduced after Java 17 SHALL be referenced by `clarity` source.

#### Scenario: Toolchain and release target are configured

- **WHEN** `clarity/build.gradle.kts` is inspected
- **THEN** it contains `java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }`
- **AND** it contains a `tasks.withType<JavaCompile>` block that sets `options.release = 17`

#### Scenario: Output bytecode runs on Java 17

- **WHEN** `./gradlew build` is run with both JDK 17 and JDK 21 installed locally
- **THEN** the build succeeds using JDK 21 as toolchain
- **AND** `javap -v` on any class under `build/classes/` reports `major version: 61` (Java 17 bytecode)
- **AND** `java -jar` against the produced jar on a JDK 17 JVM executes without `UnsupportedClassVersionError`

### Requirement: Downstream clarity-analyzer compatibility

clarity-analyzer (at `/home/spheenik/projects/clarity/clarity-analyzer`) SHALL compile against the sealed hierarchies without change to its public API. The single use of `DTClass.evaluate(Function, Function)` in `ObservableEntity.java` and the three cast-to-`S1DTClass` sites SHALL be migrated to exhaustive `switch` / `instanceof` patterns as part of this change.

#### Scenario: clarity-analyzer compiles

- **WHEN** clarity-analyzer is compiled against the updated clarity artifact
- **THEN** `./gradlew build` in clarity-analyzer succeeds with no source changes beyond `ObservableEntity.java`
- **AND** `ObservableEntity.java` contains no call to `DTClass.evaluate`
- **AND** `ObservableEntity.java` contains no `FieldPath.s1()` or `.s2()` call

### Requirement: User-facing examples compile unchanged

The `clarity-examples` repository SHALL compile against the updated clarity artifact without any source modification. No example under `examples/`, `repro/`, `dev/`, `bench/`, or `shared/` subprojects SHALL require editing solely to accommodate the sealed hierarchies.

#### Scenario: Examples build clean

- **WHEN** clarity-examples is compiled against the updated clarity artifact
- **THEN** `./gradlew build` succeeds with zero source edits required
- **AND** no warning regarding sealed types, casts, or removed methods is emitted from example source
