## Purpose

NestedArrayEntityState with Entry-based nested array storage and own Field+Entry traversal, dispatching on StateMutation at the leaf. NestedEntityState is an internal interface used only by NestedArrayEntityState.

## Requirements

### Requirement: NestedEntityState interface
The `NestedEntityState` interface SHALL be an internal detail of `NestedArrayEntityState`. It SHALL NOT be used as a common interface for all S2 EntityState implementations. It SHALL continue to provide nested index-based state access with the methods: `get(int idx)`, `set(int idx, Object value)`, `has(int idx)`, `clear(int idx)`, `sub(int idx)`, `isSub(int idx)`, `length()`, `capacity(int wantedSize, boolean shrinkIfNeeded)`.

#### Scenario: Interface is internal to NestedArrayEntityState
- **WHEN** `NestedEntityState` is used in the codebase
- **THEN** it is only referenced by `NestedArrayEntityState` and its inner `Entry` class
- **AND** it is NOT implemented by `TreeMapEntityState` or any other EntityState implementation

### Requirement: NestedArrayEntityState owns its traversal
`NestedArrayEntityState` SHALL implement `EntityState` directly. It SHALL provide its own `applyMutation(FieldPath, StateMutation)` that traverses the Field hierarchy via `field.getChild(idx)` and the Entry hierarchy via `entry.sub(idx)`. At the leaf, it SHALL dispatch on the `StateMutation` type.

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

#### Scenario: Existing behavior preserved
- **WHEN** `NestedArrayEntityState` is used as the S2 entity state
- **THEN** all field operations (value get/set, vector resize, pointer switching, capacity management) produce identical results to the previous implementation
