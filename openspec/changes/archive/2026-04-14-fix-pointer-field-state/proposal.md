## Why

`PointerField` stores the active serializer as mutable state on a shared Field object. All entities of the same DTClass share the same Field hierarchy, but each entity can have a different active serializer for its pointer fields (e.g., `CBodyComponent` → `CBodyComponentPoint` vs. `CBodyComponentBaseAnimGraph`). When entity A's `SwitchPointer` sets the serializer on the shared `PointerField`, entity B's subsequent traversal through that pointer uses the wrong serializer — leading to silent data corruption or wrong decoders. This was confirmed in Deadlock replays where entity-index recycling causes pointer variant changes across entities of the same DTClass.

Analysis across Dota 2, CS2, and Deadlock replays confirms CBodyComponent has 12–17 variants across 3 fundamental types with **0 common fields** in Deadlock/Dota 2. There is no meaningful "schema view" without per-entity state.

## What Changes

- Each `PointerField` gets a globally unique `pointerId` assigned by `FieldGenerator`. PointerField becomes **immutable** — no mutable `serializer` field
- Each S2 `EntityState` holds a `Serializer[pointerCount]` array tracking the active serializer for each pointer. `copy()` clones this array
- New `AbstractS2EntityState` abstract class holds the pointer array and all **field navigation methods** (moved from `S2DTClass`): `getFieldForFieldPath`, `getNameForFieldPath`, `getFieldPathForName`, `getTypeForFieldPath`, `getDecoderForFieldPath`. Navigation uses `resolveChild()` which looks up the active serializer from the pointer array instead of `field.getChild()`
- `NestedArrayEntityState` and `TreeMapEntityState` extend `AbstractS2EntityState`
- `S2DTClass` loses all navigation methods
- `DTClass` interface loses `getNameForFieldPath` and `getFieldPathForName`. S1DTClass keeps them as own (non-override) methods
- `Entity` becomes the unified public API for field navigation, dispatching S1 → S1DTClass, S2 → state
- Existing `Entity.hasProperty()`, `getProperty()`, `toString()` updated to use the new Entity-level methods
- `S2FieldReader.readFields` gains an `EntityState` parameter. Uses state's pointer array + local `batchPointerOverrides` for field resolution during decode. Does NOT mutate the state — SwitchPointer overrides are tracked locally in the FieldReader during the batch
- `OnEntityPropertyChanged.propertyMatches` uses Entity instead of DTClass for name resolution
- Nested pointer fields (pointer inside a pointer target's serializer) are fully supported — CS2 has `CCSGameRules.m_pGameModeRules` as a nested pointer. Each PointerField gets a flat `pointerId` regardless of nesting depth

## Capabilities

### New Capabilities
- `pointer-state-tracking`: Per-entity pointer serializer tracking via cloned rootField in AbstractS2EntityState, with navigation methods on the state and Entity as unified API

### Modified Capabilities

## Impact

- `skadistats.clarity.io.s2.field.PointerField` — add copy constructor, make `getChild()` null-safe
- `skadistats.clarity.io.s2.Serializer` — expose `fieldNames` for cloning
- `skadistats.clarity.io.s2.S2DTClass` — remove `getFieldForFieldPath`, `getNameForFieldPath`, `getFieldPathForName`, `getTypeForFieldPath`, `getDecoderForFieldPath`
- `skadistats.clarity.io.s2.S2FieldReader` — `readFields` gains `EntityState` parameter, uses state for field resolution, mutates PointerField on SwitchPointer during decode
- `skadistats.clarity.io.FieldReader` — `readFields` signature change (+ `EntityState`)
- `skadistats.clarity.io.s1.S1FieldReader` — `readFields` signature change (state parameter ignored)
- `skadistats.clarity.io.s1.S1DTClass` — remove `@Override` on navigation methods
- `skadistats.clarity.model.DTClass` — remove `getNameForFieldPath`, `getFieldPathForName`
- `skadistats.clarity.model.Entity` — add `getNameForFieldPath`, `getFieldPathForName`, `getFieldForFieldPath`; update `hasProperty`, `getProperty`, `toString`
- `skadistats.clarity.model.state.AbstractS2EntityState` — **NEW**: abstract class with cloned rootField + navigation methods
- `skadistats.clarity.model.state.NestedArrayEntityState` — extends `AbstractS2EntityState`
- `skadistats.clarity.model.state.TreeMapEntityState` — extends `AbstractS2EntityState`, adds PointerField activation on SwitchPointer
- `skadistats.clarity.model.state.S2EntityStateType` — passes `SerializerField` to `TreeMapEntityState` constructor
- `skadistats.clarity.processor.entities.Entities` — passes `EntityState` to `readFields` calls
- `skadistats.clarity.processor.entities.OnEntityPropertyChanged` — `propertyMatches` uses Entity instead of DTClass
- `skadistats.clarity.processor.tempentities.TempEntities` — `readFields` signature update

**External projects (clarity-analyzer, clarity-examples):**

All external callers switch from `entity.getDtClass().getXxx(...)` to `entity.getXxx(...)`:

- `clarity-analyzer`: `ObservableEntity.java`, `CSGOS2AndDeadlockPositionBinder.java`, `DOTAS2PositionBinder.java`
- `clarity-examples`: `resources/Main.java`, `matchend/Main.java`, `propertychange/Main.java`, `position/Main.java`, `cooldowns/Cooldowns.java`, `dumpmana/Main.java`, `lifestate/SpawnsAndDeaths.java`
