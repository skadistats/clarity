## ADDED Requirements

### Requirement: NestedEntityState interface
The `NestedEntityState` interface SHALL provide nested index-based state access with the following methods: `get(int idx)`, `set(int idx, Object value)`, `has(int idx)`, `clear(int idx)`, `sub(int idx)`, `isSub(int idx)`, `length()`, `capacity(int wantedSize, boolean shrinkIfNeeded)`, `capacity(int wantedSize)`. It SHALL replace the former `ArrayEntityState` interface.

#### Scenario: Interface replaces ArrayEntityState
- **WHEN** code references the nested state access interface
- **THEN** the type is `NestedEntityState` and provides all methods formerly on `ArrayEntityState`

### Requirement: S2EntityState abstract base class
`S2EntityState` SHALL be an abstract class that implements both `EntityState` and `NestedEntityState`. It SHALL hold a `SerializerField rootField`. It SHALL contain the shared traversal logic for `setValueForFieldPath` and `getValueForFieldPath` that walks the field hierarchy using `NestedEntityState` navigation methods.

#### Scenario: Shared traversal for setValueForFieldPath
- **WHEN** `setValueForFieldPath(fp, value)` is called on any S2EntityState subclass
- **THEN** the base class traversal iterates through the field path segments, calling `field.ensureCapacity(node, ...)`, `field.getChild(idx)`, `field.setValue(node, ...)`, and `node.sub(idx)` on the `NestedEntityState` implementation

#### Scenario: Shared traversal for getValueForFieldPath
- **WHEN** `getValueForFieldPath(fp)` is called on any S2EntityState subclass
- **THEN** the base class traversal navigates through fields using `field.getChild(idx)`, `field.getValue(node, idx)`, `node.isSub(idx)`, and `node.sub(idx)`

### Requirement: NestedArrayEntityState extends S2EntityState
`NestedArrayEntityState` SHALL extend `S2EntityState` and implement `NestedEntityState` methods via its existing `Entry`-based nested array storage. The `Entry` inner class SHALL implement `NestedEntityState`.

#### Scenario: Existing behavior preserved
- **WHEN** `NestedArrayEntityState` is used as the S2 entity state
- **THEN** all field operations (value get/set, vector resize, pointer switching, capacity management) produce identical results to the current implementation

### Requirement: Field method renaming
Field methods SHALL be renamed: `getArrayEntityState` → `getValue`, `setArrayEntityState` → `setValue`, `ensureArrayEntityStateCapacity` → `ensureCapacity`. The parameter type SHALL be `NestedEntityState` instead of `ArrayEntityState`.

#### Scenario: ValueField uses renamed methods
- **WHEN** a ValueField reads or writes state
- **THEN** it calls `getValue(NestedEntityState, int)` and `setValue(NestedEntityState, int, int, Object)`

#### Scenario: VectorField uses renamed methods
- **WHEN** a VectorField resizes a vector
- **THEN** it calls `setValue(NestedEntityState, int, int, Object)` which internally uses `state.sub(idx).capacity(count, true)`

#### Scenario: PointerField uses renamed methods
- **WHEN** a PointerField switches pointer type
- **THEN** it calls `setValue(NestedEntityState, int, int, Object)` which internally uses `state.has(idx)`, `state.clear(idx)`, `state.sub(idx)`
