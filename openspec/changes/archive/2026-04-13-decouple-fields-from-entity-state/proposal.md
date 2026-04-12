## Why

S2 Field types (ValueField, ArrayField, VectorField, PointerField) are tightly coupled to `NestedArrayEntityState` through the `ArrayEntityState` interface. The `TreeMapEntityState` bypasses the field hierarchy entirely with a flat `Map<FieldPath, Object>` approach. This means fields only work with one EntityState implementation, and the TreeMapEntityState cannot participate in field-driven structural operations (vector resize, pointer type switching). Decoupling fields from the concrete state implementation allows both state types to share the same traversal logic and makes the architecture extensible for future state implementations.

## What Changes

- **Rename `ArrayEntityState` to `NestedEntityState`** — the interface describes nested index-based access, not a concrete array structure
- **Rename Field methods**: `getArrayEntityState` → `getValue`, `setArrayEntityState` → `setValue`, `ensureArrayEntityStateCapacity` → `ensureCapacity`
- **Introduce `S2EntityState` abstract base class** that implements both `EntityState` and `NestedEntityState`, holding the shared traversal logic (`setValueForFieldPath`, `getValueForFieldPath`) and `rootField`
- **Make `NestedArrayEntityState` extend `S2EntityState`** instead of directly implementing both interfaces, moving traversal logic to the base class
- **Make `TreeMapEntityState` extend `S2EntityState`** with prefix-view-based `NestedEntityState` implementation using S2 long field path encoding for efficient range operations
- **BREAKING**: `ArrayEntityState` interface renamed to `NestedEntityState`
- **BREAKING**: Field method signatures renamed

## Capabilities

### New Capabilities
- `nested-entity-state`: Common nested state interface and S2 base class with shared field traversal logic
- `treemap-nested-state`: TreeMapEntityState implementation of NestedEntityState using long-encoded prefix views

### Modified Capabilities

## Impact

- `skadistats.clarity.model.state.ArrayEntityState` → renamed to `NestedEntityState`
- `skadistats.clarity.model.state.NestedArrayEntityState` → extends new `S2EntityState` base class
- `skadistats.clarity.model.state.TreeMapEntityState` → extends new `S2EntityState` base class, gains NestedEntityState implementation
- `skadistats.clarity.io.s2.Field` and all subclasses (ValueField, ArrayField, VectorField, PointerField, SerializerField) → method renames
- `skadistats.clarity.model.state.S2EntityStateType` → factory updated for new base class
- No changes to S1 code path (`ObjectArrayEntityState`)
