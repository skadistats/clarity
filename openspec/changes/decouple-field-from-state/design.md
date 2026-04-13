## Context

The S2 write chain currently flows: `S2FieldReader` decodes values → `FieldChanges(FieldPath[], Object[])` → `state.setValueForFieldPath(fp, value)`. Inside the state, `S2EntityState` provides a shared traversal that calls `field.getChild(idx)` + `field.setValue(NestedEntityState, ...)`. This forces all S2 state implementations to implement `NestedEntityState`, even when their storage model doesn't match (TreeMapEntityState requires a View adapter, FlatEntityState cannot implement it at all).

The FieldReader already knows the Field type at decode time (it looks up the decoder via `dtClass.getDecoderForFieldPath`), but discards this information. The State must rediscover it during traversal.

## Goals / Non-Goals

**Goals:**
- Introduce `StateMutation` sealed interface to type the write chain end-to-end
- Replace `Field.setValue/getValue/ensureCapacity/isHiddenFieldPath` with a single `Field.createMutation(Object)` method
- Replace `EntityState.setValueForFieldPath(FieldPath, Object)` with `EntityState.applyMutation(FieldPath, StateMutation)`
- Dissolve `S2EntityState` shared traversal — each State implementation owns its traversal
- Simplify `TreeMapEntityState` by removing the View adapter
- Reduce `NestedEntityState` to an internal detail of `NestedArrayEntityState`

**Non-Goals:**
- Changes to S1 entity state beyond wrapping values in `WriteValue`
- Changes to the event system (`OnEntityUpdated` etc. — these receive `FieldPath[]`, unaffected)
- Performance optimization of the state implementations themselves (that's `flat-entity-state`)

## Decisions

### D1: StateMutation as sealed interface with three variants

```java
package skadistats.clarity.model.state;

public sealed interface StateMutation {
    record WriteValue(Object value) implements StateMutation {}
    record ResizeVector(int count) implements StateMutation {}
    record SwitchPointer(Serializer newSerializer) implements StateMutation {}
}
```

**Why sealed records:** The JIT can compile the `switch(mutation)` to a tableswitch. Records are immutable value carriers. The set of mutation types is fixed — every S2 field operation maps to exactly one of these three.

**Why not an enum with data:** The variants carry different payload types (`Object`, `int`, `Serializer`). Records model this naturally.

**Alternative considered:** Passing a generic callback or visitor. Rejected because it adds indirection and prevents the clean `switch` dispatch that each State benefits from.

### D2: Field.createMutation — polymorphic creation at the source

```java
// Field (default):
public StateMutation createMutation(Object decodedValue) {
    return new StateMutation.WriteValue(decodedValue);
}

// VectorField:
public StateMutation createMutation(Object decodedValue) {
    var count = (Integer) decodedValue;
    var maxLength = S2LongFieldPathFormat.maxIndexAtDepth(/* depth from field */) + 1;
    if (count < 0 || count > maxLength) throw new ClarityException(...);
    return new StateMutation.ResizeVector(count);
}

// PointerField:
public StateMutation createMutation(Object decodedValue) {
    var pointer = (Pointer) decodedValue;
    var typeIndex = pointer.getTypeIndex();
    var newSerializer = typeIndex != null ? serializers[typeIndex] : null;
    return new StateMutation.SwitchPointer(newSerializer);
}
```

**Why on the Field:** The Field knows its own semantics — VectorField knows it represents a resize, PointerField knows how to resolve a Serializer from a Pointer. This replaces four methods (`setValue`, `getValue`, `ensureCapacity`, `isHiddenFieldPath`) with one.

**Validation stays on the Field:** VectorField validates the count range. PointerField resolves the Serializer. The State doesn't need to know about these details.

**Note on VectorField depth:** VectorField.createMutation needs the FieldPath depth for validation (`maxIndexAtDepth`). The FieldReader has this info (`fp.last() + 1`). Options: pass depth to createMutation, or move the validation into the State. The simpler path: `createMutation(Object decodedValue, int depth)` with depth defaulting to 0 for non-vector fields (ignored by the default implementation).

### D3: FieldChanges carries StateMutation[] instead of Object[]

```java
public class FieldChanges {
    private final FieldPath[] fieldPaths;
    private final StateMutation[] mutations;

    public boolean applyTo(EntityState state) {
        var capacityChanged = false;
        for (var i = 0; i < fieldPaths.length; i++) {
            capacityChanged |= state.applyMutation(fieldPaths[i], mutations[i]);
        }
        return capacityChanged;
    }
}
```

The `getValue(int idx)` / `setValue(int idx, Object value)` methods on FieldChanges change to `getMutation(int idx)` / `setMutation(int idx, StateMutation mutation)`.

**Note on debug path:** `S2FieldReader.readFieldsDebug` currently accesses `result.getValue(r)` for display. With StateMutation, the debug path would extract the value via pattern match: `mutation instanceof WriteValue wv ? wv.value() : mutation.toString()`.

### D4: EntityState.applyMutation replaces setValueForFieldPath

```java
public interface EntityState {
    boolean applyMutation(FieldPath fp, StateMutation mutation);
    <T> T getValueForFieldPath(FieldPath fp);
    EntityState copy();
    Iterator<FieldPath> fieldPathIterator();
}
```

`getValueForFieldPath` stays — reads don't need StateMutation. Only writes change.

### D5: NestedArrayEntityState — own traversal with StateMutation dispatch

The traversal moves from `S2EntityState` into `NestedArrayEntityState`. It's the same Field+Entry traversal, but at the leaf it dispatches on StateMutation instead of calling `field.setValue`:

```java
public boolean applyMutation(FieldPath fpX, StateMutation mutation) {
    var fp = fpX.s2();
    Field field = rootField;
    Entry node = rootEntry();
    var last = fp.last();
    capacityChanged = false;

    int i = 0;
    while (true) {
        var idx = fp.get(i);
        if (node.length() <= idx) {
            ensureNodeCapacity(field, node, idx);
        }
        field = field.getChild(idx);
        if (i == last) {
            return switch (mutation) {
                case WriteValue wv -> { node.set(idx, wv.value()); yield capacityChanged; }
                case ResizeVector rv -> { node.sub(idx).capacity(rv.count(), true); yield true; }
                case SwitchPointer sp -> handlePointerSwitch(node, idx, sp);
            };
        }
        node = node.sub(idx);
        i++;
    }
}
```

`ensureNodeCapacity` uses structural info from the Field (SerializerField → fieldCount, ArrayField → length) without calling `field.ensureCapacity`.

`getValueForFieldPath` moves here too, using the same Field+Entry traversal. At the leaf, it reads from the Entry directly (no `field.getValue` call needed).

### D6: TreeMapEntityState — direct TreeMap operations, no View adapter

```java
public boolean applyMutation(FieldPath fpX, StateMutation mutation) {
    return switch (mutation) {
        case WriteValue wv -> {
            var prev = state.put(fpX.s2(), wv.value());
            yield prev == null;  // capacityChanged if new key
        }
        case ResizeVector rv -> trimEntries(fpX, rv.count());
        case SwitchPointer sp -> switchPointer(fpX, sp);
    };
}
```

No traversal, no View, no NestedEntityState. The TreeMap stores FieldPaths as keys directly. `ResizeVector` trims entries via `subMap().clear()`. `SwitchPointer` clears sub-entries when the serializer changes.

`getValueForFieldPath` becomes `state.get(fp.s2())`.

The entire `View` inner class and all `NestedEntityState` method implementations are removed.

### D7: S1 — ObjectArrayEntityState wraps in WriteValue

S1 only has value writes (no vectors/pointers in the hidden-path sense). `S1FieldReader` wraps every decoded value in `StateMutation.WriteValue`. `ObjectArrayEntityState.applyMutation` extracts the value and writes it.

### D8: S2FieldReader uses field.createMutation

```java
private FieldChanges readFieldsFast(BitStream bs, DTClass dtClassGeneric) {
    var dtClass = dtClassGeneric.s2();
    // ... read field paths ...
    var result = new FieldChanges(fieldPaths, n);
    for (var r = 0; r < n; r++) {
        var fp = fieldPaths[r].s2();
        var field = dtClass.getFieldForFieldPath(fp);
        var decoded = DecoderDispatch.decode(bs, field.getDecoder());
        result.setMutation(r, field.createMutation(decoded));
    }
    return result;
}
```

The decoder is now fetched via the Field (`field.getDecoder()`) instead of `dtClass.getDecoderForFieldPath()`. This avoids a double field traversal — the FieldReader already needs the Field for createMutation.

## Risks / Trade-offs

**[StateMutation allocation]** Each write creates a StateMutation record. These are tiny (1-2 fields), short-lived (die with FieldChanges after applyTo), and never escape Young Gen. No measurable GC impact. The JIT may eliminate the allocation entirely via escape analysis for sealed records.

**[Duplicated hidden-path logic]** Vector resize logic (count validation) and Pointer switch logic (serializer resolution) now live in `Field.createMutation` instead of `Field.setValue`. This is a move, not a duplication — the old methods are removed. The State-side handling (actual resize/switch) is necessarily different per implementation.

**[Breaking change to EntityState interface]** `setValueForFieldPath(FieldPath, Object)` → `applyMutation(FieldPath, StateMutation)`. This is an internal interface — external users interact with entities via `getValueForFieldPath` and event listeners, which are unaffected. `FieldChanges.applyTo` is the only caller of the write path.

**[FieldReader now does Field lookup]** `S2FieldReader.readFieldsFast` now calls `dtClass.getFieldForFieldPath(fp)` for every field. This is a traversal of depth 1-7 via `field.getChild()`. The cost is offset by removing the separate `dtClass.getDecoderForFieldPath()` call — same traversal, now done once instead of twice (the decoder is fetched from the Field directly).
