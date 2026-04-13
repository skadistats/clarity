## Why

The S2 `NestedArrayEntityState` stores all entity field values as boxed `Object` references in a tree of `Entry` objects with `Object[]` arrays. This causes excessive heap allocations (hundreds of objects per entity), poor cache locality due to pointer chasing, and expensive copy-on-write operations that copy entire `Object[]` arrays. For a typical Hero entity with ~189 fields, this means ~200+ heap objects where a single compact buffer would suffice.

## What Changes

- New `FlatEntityState` implementation of `EntityState` that stores primitive field values in a `byte[]` accessed via `VarHandle`, and reference types (Strings, subsegments for CUtlVector fields) in a small `Object[]` sidecar
- Per-Serializer layout computation that maps each `FieldPath` to a typed offset (VarHandle + byte offset) or reference index, computed once at Serializer construction time
- `EntityStateFactory.forS2()` gains the ability to produce either `NestedArrayEntityState` or `FlatEntityState`, allowing parallel operation and benchmarking
- No changes to the public `EntityState` interface — `setValueForFieldPath(FieldPath, Object)` and `getValueForFieldPath(FieldPath)` remain as-is (Phase 1 still boxes at the interface boundary)

## Capabilities

### New Capabilities
- `flat-entity-state`: Flat byte[]-backed EntityState implementation with VarHandle-based typed access and pre-computed per-Serializer field layouts

### Modified Capabilities

## Impact

- `skadistats.clarity.model.state` — new `FlatEntityState` class, new `EntityStateType` enum, modified `EntityStateFactory`
- `skadistats.clarity.io.s2` — layout computation integrated into Serializer/Field construction pipeline (FieldGenerator or post-construction)
- `skadistats.clarity.io.s2.field` — Field subclasses need to provide size/type information for layout computation
- `skadistats.clarity.processor.runner` — `AbstractRunner` gains `entityStateType` field, `SimpleRunner`/`ControllableRunner` gain `withEntityState()` method, `Context` exposes `EntityStateType` for `@Insert`
- `skadistats.clarity.processor.entities` — `Entities` injects `EntityStateType`, passes to `DTClass.getEmptyState()`
- `skadistats.clarity.model` — `DTClass.getEmptyState()` gains `EntityStateType` parameter
- No breaking API changes for users not using `withEntityState()` — default behavior unchanged
