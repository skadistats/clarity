## ADDED Requirements

### Requirement: Each S2 EntityState has a cloned rootField with own PointerField instances

Each S2 `EntityState` implementation SHALL hold a cloned copy of the root `SerializerField` where each `PointerField` in the root Serializer's `Field[]` is replaced with an independent copy. Non-pointer fields SHALL be shared. The cloned rootField SHALL be accessible to subclasses via `AbstractS2EntityState`.

#### Scenario: Two entities of same DTClass have independent pointer state
- **WHEN** entity A and entity B share the same DTClass
- **AND** entity A receives `SwitchPointer(CBodyComponentBaseAnimGraph)`
- **AND** entity B receives `SwitchPointer(CBodyComponentPoint)`
- **THEN** entity A's rootField PointerField has serializer `CBodyComponentBaseAnimGraph`
- **AND** entity B's rootField PointerField has serializer `CBodyComponentPoint`
- **AND** neither affects the other

#### Scenario: copy() uses copy-on-write for rootField
- **WHEN** an S2 EntityState is copied via `copy()`
- **THEN** original and copy share the same rootField
- **AND** both have `rootFieldOwned = false`

#### Scenario: SwitchPointer after copy triggers clone
- **WHEN** an S2 EntityState that shares a rootField receives a SwitchPointer
- **THEN** `ensureOwnRootField()` clones the rootField before mutation
- **AND** the other state sharing the rootField is unaffected

#### Scenario: Fresh state has PointerFields at default
- **WHEN** a new S2 EntityState is created from a shared SerializerField
- **THEN** each PointerField in the cloned rootField has its `serializer` set to `defaultSerializer`
- **AND** `rootFieldOwned` is true

### Requirement: PointerField has a copy constructor and null-safe getChild()

`PointerField` SHALL provide a copy constructor that copies all immutable state (`decoder`, `serializerProperties`, `serializers[]`, `defaultSerializer`) and initializes the mutable `serializer` field from the source's current value.

`PointerField.getChild(idx)` SHALL return null when `serializer` is null (multi-variant pointer before any SwitchPointer), instead of throwing NullPointerException.

#### Scenario: Copy constructor preserves active serializer
- **WHEN** a PointerField with active serializer X is copied
- **THEN** the copy's `getChild()` delegates to serializer X

#### Scenario: getChild() on unset multi-variant pointer
- **WHEN** `getChild(idx)` is called on a PointerField with `serializer == null`
- **THEN** null is returned

#### Scenario: getChild() on single-variant pointer without SwitchPointer
- **WHEN** `getChild(idx)` is called on a PointerField with one variant
- **THEN** the default serializer is used (set at construction)

### Requirement: AbstractS2EntityState provides field navigation

A new abstract class `AbstractS2EntityState` SHALL implement `EntityState` and provide:
- `getFieldForFieldPath(S2FieldPath)` — navigates the cloned rootField tree via `getChild()` chains
- `getNameForFieldPath(FieldPath)` — resolves field names through the cloned rootField tree
- `getFieldPathForName(String)` — resolves a dotted field name to a FieldPath through the cloned rootField tree
- `getTypeForFieldPath(S2FieldPath)` — returns the FieldType for a field path
- `getDecoderForFieldPath(S2FieldPath)` — returns the Decoder for a field path

Both `NestedArrayEntityState` and `TreeMapEntityState` SHALL extend `AbstractS2EntityState`.

#### Scenario: Navigation through pointer with active serializer
- **WHEN** `getFieldForFieldPath` is called with path `[P, C1]` where field at `P` is a PointerField
- **AND** the PointerField's serializer is `SerializerX`
- **THEN** the method returns `SerializerX.getField(C1)` via normal `getChild()` delegation

#### Scenario: Navigation through unset pointer returns null
- **WHEN** `getNameForFieldPath` is called with a path through a multi-variant PointerField
- **AND** no SwitchPointer has been applied
- **THEN** the method returns null (via null-safe getChild())

#### Scenario: Name resolution for non-pointer path
- **WHEN** `getNameForFieldPath` is called with a path that does not go through any PointerField
- **THEN** the result is identical to the current S2DTClass implementation

#### Scenario: getFieldPathForName through pointer
- **WHEN** `getFieldPathForName` is called with a name like `CBodyComponent.m_cellX`
- **AND** the PointerField's active serializer has field `m_cellX`
- **THEN** the correct FieldPath is returned

### Requirement: S2DTClass loses navigation methods

`S2DTClass` SHALL NOT provide `getFieldForFieldPath`, `getNameForFieldPath`, `getFieldPathForName`, `getTypeForFieldPath`, or `getDecoderForFieldPath`. These methods are on `AbstractS2EntityState`.

### Requirement: DTClass interface loses navigation methods

`DTClass` interface SHALL NOT declare `getNameForFieldPath` or `getFieldPathForName`. `S1DTClass` SHALL keep these as concrete (non-override) methods.

### Requirement: Entity provides unified navigation API

`Entity` SHALL provide:
- `getNameForFieldPath(FieldPath)` — dispatches to S1DTClass or AbstractS2EntityState based on engine
- `getFieldPathForName(String)` — dispatches to S1DTClass or AbstractS2EntityState based on engine
- `getFieldForFieldPath(FieldPath)` — dispatches to AbstractS2EntityState (S2 only)

`Entity.hasProperty(String)` SHALL use `this.getFieldPathForName()`.
`Entity.getProperty(String)` SHALL use `this.getFieldPathForName()`.
`Entity.toString()` SHALL use `this::getNameForFieldPath` as name resolver.

#### Scenario: S2 entity navigation uses own state
- **WHEN** `entity.getNameForFieldPath(fp)` is called on an S2 entity
- **THEN** it delegates to the entity's state's `getNameForFieldPath`

#### Scenario: S1 entity navigation uses DTClass
- **WHEN** `entity.getNameForFieldPath(fp)` is called on an S1 entity
- **THEN** it delegates to the entity's S1DTClass `getNameForFieldPath`

#### Scenario: hasProperty through pointer
- **WHEN** `entity.hasProperty("CBodyComponent.m_cellX")` is called
- **AND** the entity's PointerField is set to a variant that has `m_cellX`
- **THEN** the method returns true

### Requirement: S2FieldReader uses EntityState for field resolution without mutating it

`readFields` SHALL accept an `EntityState` parameter (nullable). The reader SHALL use the state's rootField for field resolution during decode. The reader MUST NOT mutate the state or its rootField during decode — all mutations are deferred to `FieldChanges.applyTo()`.

For intra-batch pointer resolution (SwitchPointer followed by child fields in the same batch), the reader SHALL maintain a local `batchPointerOverrides` array indexed by root field position. When a SwitchPointer is decoded, the new serializer is stored in this array. When resolving a field path whose root field is a `PointerField`, the reader SHALL check this array first, falling back to the PointerField's current serializer.

#### Scenario: Entity update — field resolution from existing state
- **WHEN** `readFields` is called with an entity's current state
- **AND** a field path navigates through a pointer that was set in a previous tick
- **THEN** the pointer is resolved using the state's PointerField's active serializer

#### Scenario: Intra-batch SwitchPointer followed by child fields
- **WHEN** a decode batch contains a SwitchPointer at root position P followed by child field paths through P
- **THEN** the SwitchPointer stores the new serializer in the local `batchPointerOverrides[P]`
- **AND** subsequent child paths at P resolve through this local override
- **AND** the state's rootField is NOT mutated

#### Scenario: State is not mutated during decode
- **WHEN** `readFields` decodes a batch containing SwitchPointer mutations
- **THEN** the state's rootField PointerFields are unchanged after `readFields` returns
- **AND** the SwitchPointer mutations are in the returned `FieldChanges` for later application

#### Scenario: Baseline parse
- **WHEN** `readFields` is called for baseline parsing with a fresh empty state
- **THEN** PointerFields start at default serializer
- **AND** SwitchPointer mutations in the batch are tracked in the local override array for subsequent paths

### Requirement: TreeMapEntityState activates PointerField on SwitchPointer

`TreeMapEntityState.applyMutation` SHALL, on `SwitchPointer`, activate the new serializer on its own rootField's PointerField (in addition to existing `clearSubEntries` logic). This ensures navigation through the state's rootField resolves correctly.

#### Scenario: SwitchPointer on TreeMapEntityState
- **WHEN** `applyMutation` receives a `SwitchPointer(SerializerX)` at a PointerField
- **THEN** the PointerField on the state's rootField has serializer `SerializerX`
- **AND** sub-entries under the pointer are cleared

### Requirement: OnEntityPropertyChanged uses Entity for name resolution

`OnEntityPropertyChanged.Adapter.propertyMatches` SHALL accept an `Entity` parameter and use `entity.getNameForFieldPath(fp)` for property name resolution, instead of `dtClass.getNameForFieldPath(fp)`.

#### Scenario: Property matching uses entity's pointer state
- **WHEN** a property change event is raised for an entity with an active pointer
- **THEN** the property name is resolved through the entity's own pointer state

### Requirement: FieldGenerator validates pointer field constraints

`FieldGenerator` SHALL throw `ClarityException` if a `PointerField` is created at a non-root level (inside a sub-serializer or vector element).

#### Scenario: Pointer at root level — valid
- **WHEN** a PointerField is encountered at the root level of a serializer
- **THEN** it is created normally

#### Scenario: Pointer nested in sub-serializer — rejected
- **WHEN** a PointerField would be created inside a sub-serializer
- **THEN** a `ClarityException` is thrown
