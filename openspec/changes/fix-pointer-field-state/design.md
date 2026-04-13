## Context

`PointerField` extends `SerializerField` and inherits a mutable `serializer` field. When a `SwitchPointer` mutation is applied, `NestedArrayEntityState.handlePointerSwitch()` calls `pf.activateSerializer(newSerializer)` — mutating the shared Field object. All entities of the same DTClass share this Field, so one entity's pointer update corrupts another's navigation.

The affected navigation methods on `S2DTClass` — `getFieldForFieldPath`, `getNameForFieldPath`, `getFieldPathForName` — all chain `field.getChild(idx)` calls, which on `PointerField` delegates to `this.serializer.getField(idx)`. The same bug affects `NestedArrayEntityState.applyMutation()` and `getValueForFieldPath()` which also traverse via `field.getChild()`.

Analysis across Dota 2, CS2, and Deadlock replays confirms:
- All pointer fields are at **root serializer level** (never nested in sub-serializers or vectors)
- Maximum **7 pointer fields** per entity (Deadlock `CCitadelObserverPawn`)
- CBodyComponent: 12–17 variants across 3 types (Point, BaseModelEntity, BaseAnimGraph) with **0 common fields** in Deadlock/Dota 2 — no meaningful schema view without per-entity state
- Most other pointer types are single-variant and work correctly with `defaultSerializer`

## Goals / Non-Goals

**Goals:**
- Eliminate shared mutable state on PointerField — each entity gets its own PointerField instances via cloned rootField
- Move S2 field navigation from S2DTClass to EntityState (where the per-entity state lives)
- Entity becomes the unified public API for field navigation (S1 and S2)
- Remove navigation methods from DTClass interface
- PointerField stays mutable (safe because per-entity)
- Null-safe navigation through unset pointers (multi-variant pointer before SwitchPointer)

**Non-Goals:**
- Supporting nested pointers (inside sub-serializers or vectors) — not observed in any game
- Changes to S1 navigation logic (stays on S1DTClass)
- Fixing the `OnEntityPropertyChanged` cache for pointer paths (pre-existing bug, different entities of same DTClass can have different names for same FieldPath)

## Decisions

### D1: Clone rootField per EntityState with own PointerField instances

When creating an S2 EntityState, the root Serializer's `Field[]` array is cloned. Each `PointerField` in it is replaced with a copy (via new copy constructor). Non-pointer fields are shared (immutable).

```java
static SerializerField cloneWithOwnPointers(SerializerField original) {
    var origSer = original.getSerializer();
    var newFields = new Field[origSer.getFieldCount()];
    for (int i = 0; i < newFields.length; i++) {
        var f = origSer.getField(i);
        newFields[i] = (f instanceof PointerField pf) ? new PointerField(pf) : f;
    }
    var newSer = new Serializer(origSer.getId(), newFields, origSer.getFieldNames());
    return new SerializerField(original.getType(), newSer);
}
```

Cost: 1 Serializer + 1 Field[] + 2–7 PointerField copies per entity. ~10 objects. With ~2000 entities in a Dota 2 replay, ~20K small objects — noise relative to the millions a replay parse creates.

On `copy()`: **copy-on-write.** Both original and copy share the rootField. Both set `rootFieldOwned = false`. When either needs to mutate a PointerField (in `handlePointerSwitch`), `ensureOwnRootField()` clones first:

```java
protected boolean rootFieldOwned;

protected void ensureOwnRootField() {
    if (!rootFieldOwned) {
        rootField = cloneWithOwnPointers(rootField);
        rootFieldOwned = true;
    }
}
```

On `copy()`, the original also sets `rootFieldOwned = false` — it no longer exclusively owns the rootField. Whichever state mutates first pays the clone cost; the other keeps sharing until it mutates too.

The initial constructor (from `S2EntityStateType.createState`) clones the DTClass's shared rootField and sets `rootFieldOwned = true`.

Most entities never change their pointer state after creation — they inherit the baseline's pointer and keep it. COW avoids ~10 object allocations per `copy()` for these entities.

**Why clone over Serializer[] parameter:** The Serializer[] approach required `instanceof PointerField` checks in every navigation method, unrolled pointer resolution at root level, `UnsupportedOperationException` overrides on PointerField.getChild(), and a new parameter on 5+ methods. Cloning eliminates all of that — `getChild()` just works because each entity's PointerField has the correct serializer set.

### D2: AbstractS2EntityState holds cloned rootField and navigation methods

New abstract class between `EntityState` interface and concrete S2 implementations:

```java
public abstract class AbstractS2EntityState implements EntityState {
    protected final SerializerField rootField;

    protected AbstractS2EntityState(SerializerField sharedField) {
        this.rootField = cloneWithOwnPointers(sharedField);
    }

    protected AbstractS2EntityState(AbstractS2EntityState other) {
        this.rootField = cloneWithOwnPointers(other.rootField);
    }

    public Field getFieldForFieldPath(S2FieldPath fp) {
        return switch (fp.last()) {
            case 0 -> rootField.getChild(fp.get(0));
            case 1 -> rootField.getChild(fp.get(0)).getChild(fp.get(1));
            case 2 -> rootField.getChild(fp.get(0)).getChild(fp.get(1)).getChild(fp.get(2));
            // ... same structure as current S2DTClass, just rootField instead of field
            default -> throw new UnsupportedOperationException();
        };
    }

    public String getNameForFieldPath(FieldPath fpX) { /* same body as current S2DTClass */ }
    public FieldPath getFieldPathForName(String name) { /* same body as current S2DTClass */ }
    public FieldType getTypeForFieldPath(S2FieldPath fp) { /* delegates to getFieldForFieldPath */ }
    public Decoder getDecoderForFieldPath(S2FieldPath fp) { /* delegates to getFieldForFieldPath */ }
}
```

`NestedArrayEntityState` and `TreeMapEntityState` extend this class.

**Why not on the EntityState interface:** S1 EntityState implementations don't have a field tree. S1 navigation lives on S1DTClass. Putting S2-specific navigation on the generic interface would force S1 implementations to carry dead methods.

### D3: PointerField copy constructor and null-safe getChild()

`PointerField` gains a copy constructor:

```java
public PointerField(PointerField other) {
    super(other.getType(), other.defaultSerializer);
    this.decoder = other.decoder;
    this.serializerProperties = other.serializerProperties;
    this.serializers = other.serializers;           // shared, immutable
    this.defaultSerializer = other.defaultSerializer; // immutable
}
```

`getChild()` becomes null-safe for multi-variant pointers where no SwitchPointer has been received:

```java
@Override
public Field getChild(int idx) {
    return serializer != null ? serializer.getField(idx) : null;
}
```

The existing null checks in `getNameForFieldPath` (`if (segment == null) return null`) catch this gracefully — returns null instead of NPE.

`activateSerializer()` and `resetSerializer()` stay unchanged. They now mutate the entity's own copy, which is safe.

### D4: S2DTClass loses navigation, S1DTClass keeps it

`S2DTClass` loses `getFieldForFieldPath`, `getNameForFieldPath`, `getFieldPathForName`, `getTypeForFieldPath`, `getDecoderForFieldPath`. These are now on `AbstractS2EntityState`.

`DTClass` interface loses `getNameForFieldPath(FieldPath)` and `getFieldPathForName(String)`.

`S1DTClass` keeps its navigation methods as concrete (non-override) methods. S1 has no pointer concept — navigation is a stateless lookup in `receiveProps[]` and `propsByName`, no per-entity variation.

### D5: Entity as unified public API

`Entity` gains navigation methods that dispatch based on engine:

```java
public String getNameForFieldPath(FieldPath fp) {
    if (state instanceof AbstractS2EntityState s2s) {
        return s2s.getNameForFieldPath(fp);
    }
    return ((S1DTClass) dtClass).getNameForFieldPath(fp);
}

public FieldPath getFieldPathForName(String property) {
    if (state instanceof AbstractS2EntityState s2s) {
        return s2s.getFieldPathForName(property);
    }
    return ((S1DTClass) dtClass).getFieldPathForName(property);
}
```

Uses direct `instanceof` check on the state — no lambda allocation, no boxing. In any given replay all entities are the same engine type, so HotSpot profiles this as monomorphic.

Existing methods updated to use own navigation:

```java
public boolean hasProperty(String property) {
    return getFieldPathForName(property) != null;
}

public <T> T getProperty(String property) {
    var fp = getFieldPathForName(property);
    if (fp == null) throw new IllegalArgumentException(...);
    return getPropertyForFieldPath(fp);
}

public String toString() {
    var title = "idx: " + getIndex() + ", serial: " + getSerial() + ", class: " + getDtClass().getDtName();
    return getState().dump(title, this::getNameForFieldPath);
}
```

External callers switch from `entity.getDtClass().getXxx(...)` to `entity.getXxx(...)`.

### D6: S2FieldReader — local batch tracking, state is read-only during decode

`readFields` gains an `EntityState` parameter (nullable):

```java
public FieldChanges readFields(BitStream bs, DTClass dtClass, EntityState state, boolean debug)
```

**Critical invariant:** The FieldReader MUST NOT mutate the state during decode. Pointer mutations are deferred to `FieldChanges.applyTo()`. This is essential because (a) COW means multiple states can share a rootField, and (b) decode and mutation application are separate phases.

For field resolution, the reader uses the state's `getFieldForFieldPath()` for non-pointer paths. For intra-batch pointer changes (SwitchPointer followed by child fields in the same batch), the reader maintains a **local `batchPointerOverrides` array**, indexed by root field position (`fp.get(0)`):

```java
private Serializer[] batchPointerOverrides;

private Field resolveField(AbstractS2EntityState state, S2FieldPath fp) {
    var f0 = state.getRootField().getChild(fp.get(0));
    if (fp.last() == 0) return f0;

    Field f1;
    if (f0 instanceof PointerField pf) {
        var ser = (batchPointerOverrides != null) ? batchPointerOverrides[fp.get(0)] : null;
        if (ser == null) ser = pf.getSerializer();
        if (ser == null) ser = pf.getDefaultSerializer();
        f1 = (ser != null) ? ser.getField(fp.get(1)) : null;
    } else {
        f1 = f0.getChild(fp.get(1));
    }
    if (fp.last() == 1) return f1;

    var f = f1;
    for (int i = 2; i <= fp.last(); i++) {
        f = f.getChild(fp.get(i));
    }
    return f;
}
```

When a `SwitchPointer` mutation is created during decode:

```java
if (mutation instanceof StateMutation.SwitchPointer sp) {
    if (batchPointerOverrides == null) {
        batchPointerOverrides = new Serializer[rootFieldCount];
    }
    batchPointerOverrides[fp.get(0)] = sp.newSerializer();
}
```

This is the **only place in the system** with pointer-aware resolution logic. It's local to the FieldReader and scoped to a single batch. The `batchPointerOverrides` array is nulled at the end of each `readFields` call.

For baseline parse: state is the fresh empty state being built (cloned rootField with default pointers).

**Callers in Entities.java:**
- Entity update: pass `entity.getState()`
- Entity create: restructure `queueEntityCreate` to get baseline before `readFields`, pass baseline state
- Entity recreate: pass baseline state
- Baseline parse: pass the new empty state
- TempEntities: pass null or empty state

`S1FieldReader.readFields()` accepts and ignores the `EntityState` parameter.

### D7: NestedArrayEntityState — minimal changes

`NestedArrayEntityState` extends `AbstractS2EntityState` instead of implementing `EntityState` directly. Constructor passes the `SerializerField` to super. Copy constructor passes `other` to super.

`handlePointerSwitch` stays almost identical — calls `pf.activateSerializer()` on the entity's own PointerField copy (safe).

`applyMutation` traversal via `field.getChild()` works correctly because the PointerField is entity-specific.

`getValueForFieldPath` traversal works correctly for the same reason.

### D8: TreeMapEntityState — gains rootField

`TreeMapEntityState` extends `AbstractS2EntityState`. Constructor now accepts `SerializerField` (previously ignored it).

On `SwitchPointer` in `applyMutation`: in addition to existing `clearSubEntries` logic, also activates the new serializer on the entity's PointerField copy. This is needed so that navigation through the state's rootField resolves correctly.

### D9: OnEntityPropertyChanged uses Entity

`propertyMatches` changes signature from `(DTClass dtClass, FieldPath fp)` to `(Entity entity, FieldPath fp)`. Uses `entity.getNameForFieldPath(fp)` instead of `dtClass.getNameForFieldPath(fp)`. The `raise()` method already has the Entity available.

The cache keyed by `(DTClass, FieldPath) → Boolean` has a pre-existing correctness issue: different entities of the same DTClass can have different active pointer serializers, so the same FieldPath can map to different names. This pre-dates this change and is not addressed here.

## Risks / Trade-offs

**[Root-level-only assumption]** All current games have pointers at root level only. If Valve introduces nested pointers, the FieldGenerator validation will throw immediately, making the issue visible.

**[Memory cost]** ~10 extra objects per entity for the cloned rootField. Negligible compared to the millions of objects a replay parse creates.

**[readFields signature change]** Adding `EntityState` to `readFields` is a breaking change for the `FieldReader` interface. S1FieldReader ignores the parameter.

**[Entity create restructuring]** `queueEntityCreate` must get the baseline before calling `readFields` (to have a state for field resolution). This changes the ordering in `Entities.java` but not the semantics.

**[DTClass interface breaking change]** Removing navigation methods from DTClass forces all callers to go through Entity. This is intentional — it makes the pointer-state dependency explicit and prevents silent bugs.

**[OnEntityPropertyChanged cache]** The existing cache may return stale results for pointer paths. This is a pre-existing bug not introduced by this change.
