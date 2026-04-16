## RENAMED Requirements

- FROM: `### Requirement: AbstractS2EntityState provides field navigation`
- TO: `### Requirement: S2AbstractEntityState provides field navigation`

## MODIFIED Requirements

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
