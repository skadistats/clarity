## Purpose

Typed state mutation interface (`StateMutation`) and `Field.createMutation` for polymorphic creation, replacing the untyped `Object` value in the write chain with explicit operation types.

## Requirements

### Requirement: StateMutation sealed interface models typed state operations

The system SHALL provide a `StateMutation` sealed interface in the `skadistats.clarity.model.state` package with three record variants:

```java
public sealed interface StateMutation {
    record WriteValue(Object value) implements StateMutation {}
    record ResizeVector(int count) implements StateMutation {}
    record SwitchPointer(Serializer newSerializer) implements StateMutation {}
}
```

Every field change in the write chain SHALL be represented as a `StateMutation`. `FieldChanges` SHALL carry `StateMutation[]` instead of `Object[]`.

#### Scenario: WriteValue for normal field values

- **WHEN** a ValueField (int, float, string, etc.) is decoded from the bitstream
- **THEN** the resulting StateMutation is `WriteValue(decodedValue)`

#### Scenario: ResizeVector for variable-length arrays

- **WHEN** a VectorField's length is decoded from the bitstream
- **THEN** the resulting StateMutation is `ResizeVector(count)` with the validated count

#### Scenario: SwitchPointer for polymorphic sub-serializers

- **WHEN** a PointerField's value is decoded from the bitstream
- **THEN** the resulting StateMutation is `SwitchPointer(newSerializer)` with the resolved Serializer (or null if typeIndex is null)

### Requirement: Field.createMutation produces StateMutation from decoded values

Each Field subclass SHALL provide a `createMutation(Object decodedValue)` method that creates the appropriate `StateMutation`. This method SHALL replace `setValue`, `getValue`, `ensureCapacity`, and `isHiddenFieldPath`.

#### Scenario: Default implementation on Field

- **WHEN** `createMutation(decodedValue)` is called on a Field without an override (e.g., ValueField)
- **THEN** it returns `new StateMutation.WriteValue(decodedValue)`

#### Scenario: VectorField creates ResizeVector with validation

- **WHEN** `createMutation(decodedValue)` is called on a VectorField
- **THEN** the count is extracted from the decoded Integer value
- **AND** the count is validated against the structural maximum for the field's depth
- **AND** a `ResizeVector(count)` is returned
- **WHEN** the count is negative or exceeds the structural maximum
- **THEN** a ClarityException is thrown

#### Scenario: PointerField creates SwitchPointer with serializer resolution

- **WHEN** `createMutation(decodedValue)` is called on a PointerField
- **THEN** the Pointer value is extracted and the typeIndex is resolved to a Serializer from the PointerField's serializer array
- **AND** a `SwitchPointer(newSerializer)` is returned
- **WHEN** the Pointer's typeIndex is null
- **THEN** `SwitchPointer(null)` is returned

### Requirement: S2FieldReader uses Field.createMutation

`S2FieldReader` SHALL obtain the leaf Field via `dtClass.getFieldForFieldPath(fp)`, use `field.getDecoder()` to decode the value, and call `field.createMutation(decoded)` to produce the StateMutation. The decoder SHALL be obtained from the Field directly, eliminating the separate `dtClass.getDecoderForFieldPath()` call.

#### Scenario: FieldReader produces StateMutations

- **WHEN** `S2FieldReader.readFields` decodes field changes from the bitstream
- **THEN** for each FieldPath it obtains the leaf Field, decodes the value, and creates a StateMutation via `field.createMutation(decoded)`
- **AND** the StateMutation is stored in `FieldChanges`

#### Scenario: S1FieldReader wraps all values in WriteValue

- **WHEN** `S1FieldReader.readFields` decodes field changes
- **THEN** all decoded values are wrapped in `StateMutation.WriteValue`

### Requirement: FieldChanges carries StateMutation array

`FieldChanges` SHALL store `StateMutation[]` instead of `Object[]`. The `applyTo(EntityState)` method SHALL call `state.applyMutation(fp, mutation)` for each entry.

#### Scenario: FieldChanges applies mutations to state

- **WHEN** `fieldChanges.applyTo(state)` is called
- **THEN** for each entry it calls `state.applyMutation(fieldPaths[i], mutations[i])`
- **AND** returns true if any mutation reported a capacity change

### Requirement: EntityState.applyMutation replaces setValueForFieldPath

The `EntityState` interface SHALL provide `applyMutation(FieldPath, StateMutation)` instead of `setValueForFieldPath(FieldPath, Object)`. The method SHALL return `true` if the mutation caused a capacity change. `getValueForFieldPath` SHALL remain unchanged.

#### Scenario: applyMutation on EntityState

- **WHEN** `applyMutation(fp, mutation)` is called on any EntityState implementation
- **THEN** the state applies the mutation according to its own storage model
- **AND** returns true if the mutation caused a structural capacity change

### Requirement: Field classes lose state-manipulation methods

The methods `setValue(NestedEntityState, int, int, Object)`, `getValue(NestedEntityState, int)`, `ensureCapacity(NestedEntityState, int)`, and `isHiddenFieldPath()` SHALL be removed from the `Field` class and all subclasses. `createMutation(Object)` SHALL be the only state-related method on Field.

#### Scenario: Field has no NestedEntityState dependency

- **WHEN** the Field class is inspected after this change
- **THEN** it has no import of or reference to `NestedEntityState`
- **AND** the methods `setValue`, `getValue`, `ensureCapacity`, `isHiddenFieldPath` do not exist
- **AND** `createMutation(Object)` is the only method that relates to state operations
