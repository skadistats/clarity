## Purpose

Per-entity pointer state tracking, moving mutable serializer state from shared PointerField objects to per-EntityState arrays. This decouples field definitions (immutable, shared across all entities of a DTClass) from runtime pointer resolution (per-entity, varies as SwitchPointer mutations arrive).

## Requirements

### Requirement: Each PointerField gets a globally unique pointerId

Each `PointerField` created by `FieldGenerator` SHALL be assigned a sequential `pointerId` (0, 1, 2, ...). The total count (`pointerCount`) SHALL be available to `EntityStateFactory` for allocating per-EntityState pointer arrays. `PointerField` SHALL be immutable — no mutable `serializer` field. It retains `pointerId`, `defaultSerializer`, `serializers[]`, `decoder`, `serializerProperties`.

#### Scenario: PointerField IDs are sequential
- **WHEN** `FieldGenerator.createFields()` creates N unique PointerField objects
- **THEN** they receive pointerId values 0 through N-1
- **AND** `pointerCount` equals N

#### Scenario: PointerField is immutable
- **WHEN** a PointerField is created
- **THEN** it has no mutable `serializer` field
- **AND** `activateSerializer()` and `resetSerializer()` do not exist

### Requirement: Each S2 EntityState holds a Serializer[] for pointer state

Each S2 `EntityState` implementation SHALL hold a `Serializer[pointerCount]` array tracking the active serializer for each pointer. The array SHALL be initialized with null values (defaultSerializer is used as fallback during navigation). `copy()` SHALL clone the pointer array.

#### Scenario: Two entities of same DTClass have independent pointer state
- **WHEN** entity A and entity B share the same DTClass
- **AND** entity A receives `SwitchPointer(CBodyComponentBaseAnimGraph)` for pointerId 0
- **AND** entity B receives `SwitchPointer(CBodyComponentPoint)` for pointerId 0
- **THEN** entity A's `pointerSerializers[0]` is `CBodyComponentBaseAnimGraph`
- **AND** entity B's `pointerSerializers[0]` is `CBodyComponentPoint`

#### Scenario: copy() clones pointer state
- **WHEN** an S2 EntityState is copied via `copy()`
- **THEN** the copy has its own `pointerSerializers` array
- **AND** modifying one does not affect the other

#### Scenario: Fresh state has null pointers (defaults used)
- **WHEN** a new S2 EntityState is created
- **THEN** all entries in `pointerSerializers` are null
- **AND** navigation falls back to `PointerField.getDefaultSerializer()`

### Requirement: Navigation resolves pointers through EntityState's pointer array

All field-tree traversal on S2 EntityState SHALL use a `resolveChild(Field, int)` helper that checks if the field is a PointerField and resolves the active serializer from the EntityState's `pointerSerializers` array, falling back to `defaultSerializer`.

#### Scenario: Navigation through pointer with active serializer
- **WHEN** `resolveChild(field, idx)` is called where `field` is a PointerField with pointerId P
- **AND** `pointerSerializers[P]` is `SerializerX`
- **THEN** the method returns `SerializerX.getField(idx)`

#### Scenario: Navigation through unset pointer uses default
- **WHEN** `resolveChild(field, idx)` is called where `field` is a PointerField with pointerId P
- **AND** `pointerSerializers[P]` is null
- **AND** `defaultSerializer` is `SerializerY`
- **THEN** the method returns `SerializerY.getField(idx)`

#### Scenario: Navigation through unset multi-variant pointer returns null
- **WHEN** `resolveChild(field, idx)` is called where `field` is a PointerField with pointerId P
- **AND** `pointerSerializers[P]` is null
- **AND** `defaultSerializer` is null (multi-variant, no default)
- **THEN** the method returns null

#### Scenario: Navigation through nested pointer
- **WHEN** `resolveChild` is called for a PointerField inside another pointer's serializer
- **THEN** the same `pointerSerializers` array is used (flat lookup by pointerId)
- **AND** nesting depth does not affect the resolution

#### Scenario: Name resolution for non-pointer path
- **WHEN** `getNameForFieldPath` is called with a path that does not go through any PointerField
- **THEN** the result is identical to the previous S2DTClass implementation

### Requirement: SwitchPointer updates EntityState's pointer array

When `applyMutation` encounters a `SwitchPointer`, it SHALL update `pointerSerializers[pf.getPointerId()]` with the new serializer. No Field objects are mutated.

#### Scenario: SwitchPointer sets pointer state
- **WHEN** `applyMutation` receives a `SwitchPointer(SerializerX)` at a PointerField with pointerId P
- **THEN** `pointerSerializers[P]` is set to `SerializerX`

#### Scenario: SwitchPointer to null clears pointer state
- **WHEN** `applyMutation` receives a `SwitchPointer(null)` at a PointerField with pointerId P
- **THEN** `pointerSerializers[P]` is set to null

### Requirement: S2AbstractEntityState provides field navigation

A new abstract class `S2AbstractEntityState` SHALL implement `EntityState` and provide:
- `resolveChild(Field, int)` — resolves through pointer array or delegates to `field.getChild()`
- `getFieldForFieldPath(S2FieldPath)` — navigates using `resolveChild`
- `getNameForFieldPath(FieldPath)` — navigates using `resolveChild`
- `getFieldPathForName(String)` — navigates using `resolveChild`
- `getTypeForFieldPath(S2FieldPath)` — delegates to getFieldForFieldPath
- `getDecoderForFieldPath(S2FieldPath)` — delegates to getFieldForFieldPath
- `getPointerSerializer(int pointerId)` — returns `pointerSerializers[pointerId]`
- `getPointerCount()` — returns `pointerSerializers.length`

Both `S2NestedArrayEntityState` and `S2TreeMapEntityState` SHALL extend `S2AbstractEntityState`.

#### Scenario: Concrete S2 state classes inherit navigation from S2AbstractEntityState

- **WHEN** field-path navigation is invoked on a `S2NestedArrayEntityState` or `S2TreeMapEntityState` instance
- **THEN** the call resolves through methods defined on `S2AbstractEntityState`
- **AND** `resolveChild(Field, int)` is used for each traversal step

### Requirement: S2DTClass loses navigation methods

`S2DTClass` SHALL NOT provide `getFieldForFieldPath`, `getNameForFieldPath`, `getFieldPathForName`, `getTypeForFieldPath`, or `getDecoderForFieldPath`.

### Requirement: DTClass interface loses navigation methods

`DTClass` interface SHALL NOT declare `getNameForFieldPath` or `getFieldPathForName`. `S1DTClass` SHALL keep these as concrete (non-override) methods.

### Requirement: Entity provides unified navigation API

`Entity` SHALL provide:
- `getNameForFieldPath(FieldPath)` — dispatches to S1DTClass or S2AbstractEntityState
- `getFieldPathForName(String)` — dispatches to S1DTClass or S2AbstractEntityState
- `getFieldForFieldPath(FieldPath)` — dispatches to S2AbstractEntityState (S2 only)

`Entity.hasProperty(String)` SHALL use `this.getFieldPathForName()`.
`Entity.getProperty(String)` SHALL use `this.getFieldPathForName()`.
`Entity.toString()` SHALL use `this::getNameForFieldPath` as name resolver.

#### Scenario: Entity dispatches to the correct engine-specific navigator

- **WHEN** `entity.getNameForFieldPath(fp)` is called on an S2 entity
- **THEN** the call is routed to `S2AbstractEntityState.getNameForFieldPath`
- **WHEN** `entity.getNameForFieldPath(fp)` is called on an S1 entity
- **THEN** the call is routed to `S1DTClass.getNameForFieldPath`

### Requirement: S2FieldReader uses EntityState + batch overrides for field resolution

`readFields` SHALL accept an `EntityState` parameter. The FieldReader SHALL resolve fields using `resolveField()` which checks a local `batchPointerOverrides` array first, then the EntityState's pointer array, then the PointerField's default.

The `batchPointerOverrides` array SHALL be allocated lazily on first use (sized from `state.getPointerCount()`) and reused across batches. At batch end, set entries SHALL be cleared.

When a SwitchPointer mutation is created during decode, the new serializer SHALL be stored in `batchPointerOverrides[pf.getPointerId()]`.

#### Scenario: Intra-batch SwitchPointer followed by child fields
- **WHEN** a decode batch contains a SwitchPointer for pointerId P followed by child field paths through that pointer
- **THEN** the SwitchPointer stores the new serializer in `batchPointerOverrides[P]`
- **AND** subsequent child paths resolve through this local override
- **AND** the EntityState's pointer array is NOT mutated during decode

#### Scenario: Nested pointer switch in batch
- **WHEN** a batch contains SwitchPointer at `[0]` (pointerId 1) then SwitchPointer at `[0, 75]` (pointerId 5)
- **THEN** both are stored in `batchPointerOverrides` by their respective pointerId
- **AND** child fields under `[0, 75, ...]` resolve through pointerId 5's override

### Requirement: OnEntityPropertyChanged uses Entity for name resolution

`OnEntityPropertyChanged.Adapter.propertyMatches` SHALL accept an `Entity` parameter and use `entity.getNameForFieldPath(fp)` for property name resolution.

### Requirement: pointerCount flows through EntityStateFactory

`EntityStateFactory` SHALL receive `pointerCount` (from `FieldGenerator` via `S2DTClassEmitter`/`DTClasses`) and pass it to `S2EntityStateType.createState()` for allocation of the per-state pointer array.
