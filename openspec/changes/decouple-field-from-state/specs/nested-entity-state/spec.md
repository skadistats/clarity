## MODIFIED Requirements

### Requirement: NestedEntityState interface
The `NestedEntityState` interface SHALL be an internal detail of `NestedArrayEntityState`. It SHALL NOT be used as a common interface for all S2 EntityState implementations. It SHALL continue to provide nested index-based state access with the methods: `get(int idx)`, `set(int idx, Object value)`, `has(int idx)`, `clear(int idx)`, `sub(int idx)`, `isSub(int idx)`, `length()`, `capacity(int wantedSize, boolean shrinkIfNeeded)`.

#### Scenario: Interface is internal to NestedArrayEntityState
- **WHEN** `NestedEntityState` is used in the codebase
- **THEN** it is only referenced by `NestedArrayEntityState` and its inner `Entry` class
- **AND** it is NOT implemented by `TreeMapEntityState` or any other EntityState implementation

### Requirement: S2EntityState abstract base class
`S2EntityState` SHALL be dissolved. There SHALL be no shared abstract base class with traversal logic for S2 EntityState implementations. Each implementation SHALL provide its own `applyMutation(FieldPath, StateMutation)` and `getValueForFieldPath(FieldPath)`.

#### Scenario: No shared S2EntityState
- **WHEN** NestedArrayEntityState or TreeMapEntityState is inspected
- **THEN** neither extends `S2EntityState`
- **AND** each directly implements `EntityState`

### Requirement: NestedArrayEntityState owns its traversal
`NestedArrayEntityState` SHALL implement `EntityState` directly (not via S2EntityState). It SHALL provide its own `applyMutation(FieldPath, StateMutation)` that traverses the Field hierarchy via `field.getChild(idx)` and the Entry hierarchy via `entry.sub(idx)`. At the leaf, it SHALL dispatch on the `StateMutation` type.

#### Scenario: applyMutation with WriteValue
- **WHEN** `applyMutation(fp, WriteValue(value))` is called on NestedArrayEntityState
- **THEN** the traversal navigates to the correct Entry via Field+Entry hierarchy
- **AND** the value is stored via `entry.set(idx, value)`

#### Scenario: applyMutation with ResizeVector
- **WHEN** `applyMutation(fp, ResizeVector(count))` is called on NestedArrayEntityState
- **THEN** the traversal navigates to the correct Entry
- **AND** the sub-entry is resized via `entry.sub(idx).capacity(count, true)`
- **AND** the method returns true (capacity changed)

#### Scenario: applyMutation with SwitchPointer
- **WHEN** `applyMutation(fp, SwitchPointer(newSerializer))` is called on NestedArrayEntityState
- **AND** the new serializer differs from the current one
- **THEN** the existing sub-entry is cleared and a new one is created
- **WHEN** newSerializer is null
- **THEN** the sub-entry is cleared

#### Scenario: getValueForFieldPath traversal
- **WHEN** `getValueForFieldPath(fp)` is called on NestedArrayEntityState
- **THEN** it traverses the Field+Entry hierarchy and returns the value at the leaf
- **AND** it does NOT call `field.getValue(NestedEntityState, int)`

#### Scenario: Capacity ensured from Field structural info
- **WHEN** the traversal encounters a node shorter than the required index
- **THEN** it determines the required capacity from the parent Field type: `SerializerField` → `serializer.getFieldCount()`, `ArrayField` → `length`
- **AND** it ensures the Entry capacity directly, without calling `field.ensureCapacity`

### Requirement: Field method renaming
This requirement is REMOVED — the renamed methods (`setValue`, `getValue`, `ensureCapacity`) are themselves removed in favor of `Field.createMutation`.

#### Scenario: Field methods removed
- **WHEN** the Field class hierarchy is inspected
- **THEN** the methods `setValue(NestedEntityState, ...)`, `getValue(NestedEntityState, ...)`, and `ensureCapacity(NestedEntityState, ...)` do not exist

## REMOVED Requirements

### Requirement: Field method renaming
**Reason**: Replaced by `Field.createMutation(Object)` in the `state-mutation` capability. The methods `setValue`, `getValue`, and `ensureCapacity` are removed entirely, not renamed.
**Migration**: Use `Field.createMutation(Object)` to create StateMutation objects. State implementations handle the mutations directly.
