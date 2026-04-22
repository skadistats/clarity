## ADDED Requirements

### Requirement: FixedPointerField reads only a presence bit
A `FixedPointerField` SHALL consume exactly one bit from the bitstream (the presence boolean) via `FixedPointerDecoder`. No type-index varint SHALL be read.

#### Scenario: Presence bit true
- **WHEN** the bitstream yields `true` for the presence bit
- **THEN** `FixedPointerDecoder.decode()` returns `Boolean.TRUE`

#### Scenario: Presence bit false
- **WHEN** the bitstream yields `false` for the presence bit
- **THEN** `FixedPointerDecoder.decode()` returns `Boolean.FALSE`

### Requirement: FixedPointerField always resolves to its fixed serializer
`FixedPointerField` SHALL store the serializer as a direct field reference and return it from `resolveSerializer()` without consulting `S2EntityState`. The resolved serializer SHALL be the same regardless of the presence bit value.

#### Scenario: Child navigation
- **WHEN** `getChild(state, idx)` is called on a `FixedPointerField`
- **THEN** it returns the child field from the fixed serializer at the given index, without reading from `state.pointerSerializers`

### Requirement: FixedPointerField produces no state mutation
`FixedPointerField.createMutation()` SHALL return `null`. `FixedPointerField.prepareForWrite()` SHALL return `null`. No `pointerSerializers` slot SHALL be allocated in `S2EntityState` for a fixed pointer field.

#### Scenario: Mutation in materialized read path
- **WHEN** `createMutation()` is called with any decoded value
- **THEN** it returns `null`, and no `StateMutation.SwitchPointer` is produced

#### Scenario: Write in fast read path
- **WHEN** `prepareForWrite()` is called with any decoded value
- **THEN** it returns `null`

### Requirement: FixedPointerField is not assigned a pointerId
`FieldGenerator` SHALL NOT increment `pointerCount` for a pointer field with exactly one serializer. Such fields SHALL be constructed as `FixedPointerField` instances.

#### Scenario: Single-serializer pointer field construction
- **WHEN** `FieldGenerator` encounters a pointer field whose `polymorphicTypes` array resolves to exactly one serializer
- **THEN** a `FixedPointerField` is created and `pointerCount` is not incremented

#### Scenario: Multi-serializer pointer field construction
- **WHEN** `FieldGenerator` encounters a pointer field whose `polymorphicTypes` array resolves to more than one serializer
- **THEN** a `PolymorphicPointerField` is created and `pointerCount` is incremented
