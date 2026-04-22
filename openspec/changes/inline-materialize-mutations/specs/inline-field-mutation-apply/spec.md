## ADDED Requirements

### Requirement: Materialize path applies mutations inline
`S2FieldReader.readFieldsMaterialized` SHALL apply each `StateMutation` to the entity state immediately after decoding, before resolving the next field path in the same packet.

#### Scenario: Polymorphic pointer switch followed by child field in same packet
- **WHEN** a packet contains a `PolymorphicPointerField` switch at path X followed by a field path under that pointer
- **THEN** the child field SHALL be resolved against the updated state (reflecting the new serializer), without consulting a separate override array

#### Scenario: MutationListener receives typed mutation
- **WHEN** a `MutationListener` is registered and a field is decoded in the materialize path
- **THEN** the listener SHALL receive the `StateMutation` object created for that field, with the entity state already reflecting the applied mutation

### Requirement: Single field resolver for all read paths
`S2FieldReader` SHALL use one field resolver implementation (`resolveField`) for both fast and materialize/debug paths. No separate `resolveFieldDebug` method or `pointerOverrides` array SHALL exist.

#### Scenario: Fast path unchanged
- **WHEN** no `MutationListener` is registered
- **THEN** field resolution SHALL behave identically to before this change

#### Scenario: Materialize path uses same resolver
- **WHEN** a `MutationListener` is registered
- **THEN** field paths SHALL be resolved using the same logic as the fast path

### Requirement: Materialize path tracks capacityChanged
The `FieldChanges` returned by `readFieldsMaterialized` SHALL carry the correct `capacityChanged` flag, accumulated from inline state writes.

#### Scenario: Vector resize causes capacity change
- **WHEN** a decoded field causes the entity state to resize an array
- **THEN** `FieldChanges.capacityChanged()` SHALL return `true`
