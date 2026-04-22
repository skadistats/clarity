# Spec: context-construction-api

## Purpose

`Context` serves as the single construction entry point for entity states and field readers within a run. Run configuration (entity state types, field path type) is captured as immutable constructor parameters. Parse-time values (build number, millis-per-tick, game version, pointer count) are stored via set-once setters. Legacy indirection through `EntityStateFactory`, `ContextData`, and `DTClass` construction methods is eliminated.

## Requirements

### Requirement: Context owns entity state and field reader construction

`Context` SHALL expose `newEntityState(DTClass cls)` and `newFieldReader()` as the sole public construction entry points for entity states and field readers within a run. No processor SHALL construct an `EntityState` or `FieldReader` without going through `Context`.

`newEntityState` SHALL dispatch based on the concrete DTClass type:
- For `S2DTClass`: delegate to `s2EntityStateType.createState(s2.getField(), pointerCount, layoutBuilder)`
- For `S1DTClass`: delegate to `s1EntityStateType.createState(s1)`

`newFieldReader` SHALL delegate to `engineType.getNewFieldReader(s2FieldPathType)`.

#### Scenario: S2 entity state created via Context

- **WHEN** `context.newEntityState(s2DtClass)` is called after `setPointerCount` has been called
- **THEN** a fresh `S2EntityState` of the runner-configured type is returned
- **AND** the state is empty

#### Scenario: S1 entity state created via Context

- **WHEN** `context.newEntityState(s1DtClass)` is called
- **THEN** a fresh `S1EntityState` of the runner-configured type is returned
- **AND** the state is empty

#### Scenario: FieldReader uses configured path type

- **WHEN** `context.newFieldReader()` is called for an S2 engine
- **THEN** an `S2FieldReader` configured with the runner's `S2FieldPathType` is returned

#### Scenario: S1 FieldReader unaffected by S2FieldPathType

- **WHEN** `context.newFieldReader()` is called for an S1 engine
- **THEN** an S1 `FieldReader` appropriate for the engine is returned

---

### Requirement: Context holds run configuration as structural fields

`Context` SHALL hold `s1EntityStateType`, `s2EntityStateType`, and `s2FieldPathType` as constructor parameters. These values SHALL be immutable after construction. `FieldLayoutBuilder` SHALL be instantiated privately in the constructor and never exposed publicly.

#### Scenario: Configuration is fixed after construction

- **WHEN** `Context` is constructed with a given `S2EntityStateType`
- **THEN** `context.newEntityState(s2DtClass)` always produces states of that type for the lifetime of the run

---

### Requirement: Context provides uniform set-once parse-time initialization

`Context` SHALL expose set-once setters for `buildNumber`, `millisPerTick`, `gameVersion`, and `pointerCount`. Each setter SHALL throw `IllegalStateException` if called more than once. These replace the `ContextData` shared-mutable-reference pattern and the engine type's `getContextData()` method.

Engine types SHALL call `ctx.setBuildNumber`, `ctx.setMillisPerTick`, and `ctx.setGameVersion` directly (they already inject `ctx`). `S2DTClassEmitter` SHALL call `ctx.setPointerCount` directly.

#### Scenario: First call to any set-once setter succeeds

- **WHEN** a set-once setter on `Context` is called for the first time during a run
- **THEN** the value is stored and subsequently returned by the corresponding getter

#### Scenario: Second call to any set-once setter throws

- **WHEN** a set-once setter on `Context` is called a second time during the same run
- **THEN** `IllegalStateException` is thrown
- **AND** the previously stored value is unchanged

---

### Requirement: EntityStateFactory and ContextData are deleted

`EntityStateFactory` SHALL be deleted. `ContextData` SHALL be deleted. `EngineType.getContextData()` SHALL be removed. No processor SHALL reference either class.

#### Scenario: EntityStateFactory is not present in the codebase

- **WHEN** the codebase is compiled after this change
- **THEN** no class named `EntityStateFactory` exists
- **AND** no `@Import` or `@Insert` of `EntityStateFactory` exists anywhere

---

### Requirement: DTClass no longer participates in entity state construction

`getEmptyState()`, `getEmptyState(EntityStateFactory)`, and `setEntityStateFactory()` SHALL be removed from the `DTClass` interface and from `S1DTClass` and `S2DTClass`. Neither implementation SHALL hold a reference to any factory or construct entity states independently.

#### Scenario: DTClass has no construction methods

- **WHEN** the compiler resolves the `DTClass` interface
- **THEN** no method named `getEmptyState` or `setEntityStateFactory` exists on the interface

---

### Requirement: Infrastructure processors are separated via hook

`AbstractRunner` SHALL expose a protected `infraProcessors()` hook returning `List<Object>`, defaulting to `List.of(this)`. `AbstractFileRunner` SHALL override it to return `[this, engineType, engineType.getPacketReader(), source]`. The recursive `Object[]` flattening in `addProcessorsToModel` SHALL be retained to preserve support for nested arrays in the public `runWith(Object... processors)` API.

`source` SHALL remain in the processor list to preserve injectability for downstream consumers that may declare `@Insert Source`.

#### Scenario: Infrastructure processors are contributed by the correct layer

- **WHEN** a `SimpleRunner` initializes
- **THEN** `infraProcessors()` returns the runner, engine type, packet reader, and source
- **AND** user processors are appended after infrastructure processors in the execution model
