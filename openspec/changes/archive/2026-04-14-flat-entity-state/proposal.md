## Why

The S2 `NestedArrayEntityState` stores all entity field values as boxed `Object` references in a tree of `Entry` objects with `Object[]` arrays. This causes excessive heap allocations (hundreds of objects per entity), poor cache locality due to pointer chasing, and expensive copy-on-write operations that copy entire `Object[]` arrays. For a typical Hero entity with ~189 fields, this means ~200+ heap objects where a single compact buffer would suffice.

## What Changes

- New `FlatEntityState` extending `AbstractS2EntityState` that stores primitive field values in a `byte[]` accessed via `VarHandle`, and reference types (Strings, sub-states for VectorField/PointerField) in a small `Object[]` sidecar
- Per-Serializer layout computation (`FieldLayoutBuilder`) that maps each `FieldPath` to a typed offset (VarHandle + byte offset) or reference index, computed once and cached per Serializer
- `S2EntityStateType.FLAT` variant added to the existing enum, allowing parallel operation and benchmarking via the already-existing `withS2EntityState()` runner configuration
- No changes to the public `EntityState` interface — `applyMutation(FieldPath, StateMutation)` and `getValueForFieldPath(FieldPath)` remain as-is (Phase 1 still boxes at the interface boundary)

## Dependencies

- **decouple-field-from-state** (COMPLETE) — introduced `StateMutation` sealed interface, `EntityState.applyMutation()`, and decoupled Fields from EntityState

## Capabilities

### New Capabilities
- `flat-entity-state`: Flat byte[]-backed EntityState implementation with VarHandle-based typed access and pre-computed per-Serializer field layouts

### Modified Capabilities

## Impact

- `skadistats.clarity.model.state` — new `PrimitiveType`, `FieldLayout`, `FieldLayoutBuilder`, `FlatEntityState` classes; `S2EntityStateType` gains `FLAT` variant
- `skadistats.clarity.io.decoder` — `Decoder` gains `getPrimitiveType()` method, all concrete decoders override with their `PrimitiveType`
- `skadistats.clarity.io.s2.field` — `ArrayField`, `VectorField`, `PointerField` gain accessor methods for `FieldLayoutBuilder`
- No changes to runner infrastructure (`AbstractFileRunner.withS2EntityState()` already exists)
- No breaking API changes for users not using `withS2EntityState(FLAT)` — default behavior unchanged
