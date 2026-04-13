## Context

`PointerField` extends `SerializerField` and inherits a mutable `serializer` field. When a `SwitchPointer` mutation is applied, `NestedArrayEntityState` calls `pf.activateSerializer(newSerializer)` — mutating the shared Field object. All entities of the same DTClass share this Field hierarchy, but each entity can have a different active serializer for its pointer fields (e.g., `CBodyComponent` → `CBodyComponentPoint` vs. `CBodyComponentBaseAnimGraph`). When entity A's `SwitchPointer` sets the serializer on the shared `PointerField`, entity B's subsequent traversal through that pointer uses the wrong serializer — leading to silent data corruption or wrong decoders.

Analysis across Dota 2, CS2, and Deadlock replays shows:
- **Nested pointers exist in CS2**: `CCSGameRulesProxy.m_pGameRules → CCSGameRules` contains `m_pGameModeRules → CCSGameModeRules*` (a pointer inside a pointer target). Dota 2 and Deadlock currently have no nested pointers, but the solution must handle them.
- Maximum **7 pointer fields** per entity (Deadlock `CCitadelObserverPawn`)
- 9–22 unique PointerField objects per game
- CBodyComponent: 12–17 variants across 3 types with **0 common fields** in Deadlock/Dota 2

All existing open-source parsers (demoinfocs-golang, demoparser, source2-demo) either share the same mutable-state bug or ignore SwitchPointer entirely.

## Goals / Non-Goals

**Goals:**
- Eliminate shared mutable state on PointerField — each entity tracks its own active serializers
- Pointer state lives on the EntityState (not on the shared Field hierarchy) so it travels with `copy()` and baseline caching
- Support nested pointers (pointer inside a pointer target's serializer)
- PointerField becomes immutable — `serializer` field removed, replaced by per-entity lookup
- Entity becomes the unified public API for field navigation (S1 and S2)
- Remove navigation methods from DTClass interface

**Non-Goals:**
- Changes to S1 navigation logic (stays on S1DTClass)
- Fixing the `OnEntityPropertyChanged` cache for pointer paths (pre-existing bug)

## Decisions

### D1: Each PointerField gets a globally unique `pointerId`

During `FieldGenerator.createFields()`, each unique PointerField object is assigned a sequential `pointerId` (0, 1, 2, ...). The total count (`pointerCount`) is stored and passed through `EntityStateFactory` to each new EntityState.

```java
// In FieldGenerator
private int nextPointerId = 0;

// When creating a PointerField:
var pf = new PointerField(fieldType, decoder, serializerProperties, serializers);
pf.setPointerId(nextPointerId++);
```

Typical values: 9 (Deadlock), 13 (Dota 2), 22 (CS2).

### D2: PointerField becomes immutable

`PointerField` no longer has a mutable `serializer` field. It retains:
- `pointerId` — index into the EntityState's pointer array
- `defaultSerializer` — the single-variant default (null for multi-variant)
- `serializers[]` — all polymorphic variants (immutable)
- `decoder`, `serializerProperties` — unchanged

`activateSerializer()` and `resetSerializer()` are removed.

`getChild()` can no longer work standalone — it needs the active serializer from the EntityState. See D4.

### D3: EntityState holds a `Serializer[]` for pointer state

Each S2 EntityState holds a `Serializer[pointerCount]` array tracking the active serializer for each pointer:

```java
public abstract class AbstractS2EntityState implements EntityState {
    protected final Serializer[] pointerSerializers;

    protected AbstractS2EntityState(int pointerCount) {
        this.pointerSerializers = new Serializer[pointerCount];
    }

    // copy constructor
    protected AbstractS2EntityState(AbstractS2EntityState other) {
        this.pointerSerializers = other.pointerSerializers.clone();
    }
}
```

`copy()` clones the pointer array. At 9–22 entries, this is trivial.

`pointerCount` flows: `FieldGenerator` → `EntityStateFactory` → `S2EntityStateType.createState()` → `AbstractS2EntityState` constructor.

### D4: Navigation resolves pointers through the EntityState's pointer array

All Field-tree traversal on the EntityState uses a helper instead of `field.getChild()` when encountering a PointerField:

```java
protected Field resolveChild(Field field, int idx) {
    if (field instanceof PointerField pf) {
        var ser = pointerSerializers[pf.getPointerId()];
        if (ser == null) ser = pf.getDefaultSerializer();
        return ser != null ? ser.getField(idx) : null;
    }
    return field.getChild(idx);
}
```

This is used in:
- `AbstractS2EntityState.getFieldForFieldPath()` — replaces chained `getChild()` calls
- `AbstractS2EntityState.getNameForFieldPath()` — replaces `currentField.getChild(idx)`
- `AbstractS2EntityState.getFieldPathForName()` — replaces `currentField.getChild(fieldIdx)`
- `NestedArrayEntityState.applyMutation()` — replaces `field.getChild(idx)` in traversal
- `NestedArrayEntityState.getValueForFieldPath()` — replaces `field.getChild(idx)` in traversal
- `TreeMapEntityState.applyMutation()` — for PointerField resolution in SwitchPointer handling

The shared Field hierarchy is never mutated. `PointerField.getChild()` is no longer called during navigation.

### D5: SwitchPointer updates the EntityState's pointer array

When `applyMutation` encounters a `SwitchPointer`:

```java
case StateMutation.SwitchPointer sp -> {
    if (field instanceof PointerField pf) {
        pointerSerializers[pf.getPointerId()] = sp.newSerializer();
    }
    // ... existing entry clearing logic
}
```

No Field mutation. The pointer state change is local to this EntityState.

### D6: S2DTClass loses navigation, Entity becomes unified API

Same as before:
- `S2DTClass` loses `getFieldForFieldPath`, `getNameForFieldPath`, `getFieldPathForName`, `getTypeForFieldPath`, `getDecoderForFieldPath`
- `DTClass` interface loses `getNameForFieldPath`, `getFieldPathForName`
- `S1DTClass` keeps navigation as concrete methods
- `Entity` gains navigation methods dispatching S1 → S1DTClass, S2 → state

### D7: S2FieldReader uses EntityState + local batch overrides for field resolution

`readFields` gains an `EntityState` parameter. The FieldReader needs to resolve fields through pointers during decode — before `FieldChanges.applyTo()` runs. FieldPaths in a batch are ascending: a SwitchPointer at `[P]` comes before child fields at `[P, ...]`. The child fields are encoded with the **new** serializer (after the switch). The old code (4.0.0) handled this by mutating the shared PointerField directly during decode.

The FieldReader maintains a local `Serializer[] batchPointerOverrides` array, allocated lazily on first use and reused across batches (cleared with `Arrays.fill(null)` at batch end). The size is obtained from `state.getPointerCount()` on first allocation.

Field resolution first checks `batchPointerOverrides`, then falls back to the EntityState's `pointerSerializers`:

```java
private Serializer[] batchPointerOverrides;

private Field resolveField(AbstractS2EntityState state, Field field, int idx) {
    if (field instanceof PointerField pf) {
        var pid = pf.getPointerId();
        var ser = (batchPointerOverrides != null) ? batchPointerOverrides[pid] : null;
        if (ser == null) ser = state.getPointerSerializer(pid);
        if (ser == null) ser = pf.getDefaultSerializer();
        return ser != null ? ser.getField(idx) : null;
    }
    return field.getChild(idx);
}
```

When a SwitchPointer mutation is created during decode, the new serializer is stored in `batchPointerOverrides`:

```java
if (mutation instanceof StateMutation.SwitchPointer sp && field instanceof PointerField pf) {
    if (batchPointerOverrides == null) {
        batchPointerOverrides = new Serializer[s2state.getPointerCount()];
    }
    batchPointerOverrides[pf.getPointerId()] = sp.newSerializer();
}
```

This works for nested pointers because every PointerField (regardless of depth) has a unique `pointerId` — the lookup is flat, not tree-based.

At batch end, only the entries that were actually set need clearing. The array is reused across batches to avoid allocation.

**Callers in Entities.java:**
- Entity update: pass `entity.getState()`
- Entity create: get baseline before `readFields`, pass baseline state
- Entity recreate: pass baseline state
- Baseline parse: pass the new empty state
- TempEntities (S1 only): pass null

`S1FieldReader.readFields()` accepts and ignores the `EntityState` parameter.

### D8: OnEntityPropertyChanged uses Entity

Same as before — `propertyMatches` takes `Entity` instead of `DTClass`.

### D9: No nested pointer validation needed

Nested pointers are now fully supported. The `validateNoNestedPointers` check is removed. Every PointerField, regardless of nesting depth, gets a `pointerId` and is tracked in the EntityState's `Serializer[]` array.

## Risks / Trade-offs

**[Pointer array on every EntityState]** Every S2 EntityState carries a `Serializer[9..22]` array. At ~2000 entities per replay, this is ~40K references — negligible.

**[resolveChild on every navigation step]** Each `getChild()` in the traversal goes through `resolveChild`, which does an `instanceof PointerField` check. This is a fast type check that HotSpot optimizes well, and only fires at pointer boundaries (not for every field).

**[readFields signature change]** Adding `EntityState` to `readFields` is a breaking change for the `FieldReader` interface. S1FieldReader ignores the parameter.

**[DTClass interface breaking change]** Removing navigation methods from DTClass forces all callers to go through Entity. This is intentional.

**[OnEntityPropertyChanged cache]** The existing cache may return stale results for pointer paths. This is a pre-existing bug not introduced by this change.
