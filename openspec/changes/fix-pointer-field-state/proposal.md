## Why

`PointerField` stores the active serializer as mutable state on a shared Field object. All entities of the same DTClass share the same Field hierarchy, but each entity can have a different active serializer for its pointer fields (e.g., `CBodyComponent` → `CBodyComponentPoint` vs. `CBodyComponentBaseAnimGraph`). When entity A's `SwitchPointer` sets the serializer on the shared `PointerField`, entity B's subsequent traversal through that pointer uses the wrong serializer — leading to silent data corruption or wrong decoders. This was confirmed in Deadlock replays where entity-index recycling causes pointer variant changes across entities of the same DTClass.

## What Changes

- `PointerField` becomes immutable: the mutable `serializer` field is removed. It retains `serializers[]` (all possible variants), `defaultSerializer`, and a new `pointerIndex` (small integer assigned at construction time)
- `EntityState` gains a `Serializer[]` array to track the active serializer per pointer position. Updated on `SwitchPointer` application. Cloned on `copy()`
- `FieldChanges` tracks new pointer serializers decoded in the current batch, so that child field paths after a `SwitchPointer` in the same decode round can be resolved correctly
- `S2FieldReader` maintains a local `Serializer[]` array, primed from the `EntityState` before each decode batch, updated when `SwitchPointer` mutations are decoded. Field resolution uses this array instead of the `PointerField`'s mutable state
- All field-navigating methods on `S2DTClass` (`getFieldForFieldPath`, `getNameForFieldPath`, `getFieldPathForName`) gain a `Serializer[]` parameter for pointer resolution. All three currently use `field.getChild()` which has the same pointer bug
- `readFields` gains an `EntityState` parameter (nullable for first baseline parse). Callers pass the entity's state (for updates), the baseline state (for creates), or null (for initial baseline parse)
- Pointer fields nested inside sub-serializers or vectors are not supported and will throw a `ClarityException` at FieldGenerator construction time if encountered — currently all pointer fields in Dota 2, CS2, and Deadlock replays are at root serializer level
- Maximum 8 pointer fields per entity (current maximum across all games: 7 in Deadlock `CCitadelObserverPawn`). Exceeding this throws a `ClarityException` at construction time

## Capabilities

### New Capabilities
- `pointer-state-tracking`: Active pointer serializer tracking in EntityState and FieldChanges, with immutable PointerField and per-decode-batch resolution in S2FieldReader

### Modified Capabilities

## Impact

- `skadistats.clarity.io.s2.field.PointerField` — remove mutable `serializer`, add immutable `pointerIndex`
- `skadistats.clarity.io.s2.field.SerializerField` — `serializer` field becomes final again
- `skadistats.clarity.io.s2.S2FieldReader` — `resolveField` with pointer-aware loop, local `Serializer[]` primed from state, `readFields` gains `EntityState` parameter
- `skadistats.clarity.io.s2.S2DTClass` — `getFieldForFieldPath`, `getNameForFieldPath`, `getFieldPathForName` all gain `Serializer[]` parameter
- `skadistats.clarity.io.FieldReader` — `readFields` signature change (+ `EntityState`)
- `skadistats.clarity.io.FieldChanges` — tracks pointer serializers for current decode batch
- `skadistats.clarity.model.state.EntityState` — `Serializer[]` for pointer tracking, `fillPointerSerializers` method
- `skadistats.clarity.model.state.NestedArrayEntityState` — stores and copies pointer serializer array, uses it during own traversal instead of `field.getChild()` through pointers
- `skadistats.clarity.model.state.TreeMapEntityState` — stores pointer serializer array
- `skadistats.clarity.processor.entities.Entities` — passes `EntityState` to `readFields` calls (entity state for updates, baseline for creates, null for baseline parse)
- `skadistats.clarity.processor.sendtables.FieldGenerator` — assigns `pointerIndex` to `PointerField`, throws on nested pointer fields
- `skadistats.clarity.io.s1.S1FieldReader` — `readFields` signature change (state parameter ignored)

- `skadistats.clarity.model.Entity` — convenience methods `getFieldForFieldPath`, `getNameForFieldPath`, `getFieldPathForName` that delegate to DTClass with the state's pointer serializers. Simplifies external callers

**External projects (clarity-analyzer, clarity-examples):**

All external callers of `getFieldForFieldPath`, `getNameForFieldPath`, `getFieldPathForName`, and `getTypeForFieldPath` have access to the entity's State and need to pass the `pointerSerializers` array. Affected files:

- `clarity-analyzer`: `ObservableEntity.java` (4 call sites), `CSGOS2AndDeadlockPositionBinder.java`, `DOTAS2PositionBinder.java`
- `clarity-examples`: `resources/Main.java`, `matchend/Main.java`, `propertychange/Main.java`, `position/Main.java`, `cooldowns/Cooldowns.java`, `dumpmana/Main.java`, `lifestate/SpawnsAndDeaths.java`
