## ADDED Requirements

### Requirement: PrimitiveType sealed interface encapsulates typed byte[] access

The system SHALL provide a `PrimitiveType` sealed interface in the `skadistats.clarity.model.state` package with two implementations: `Scalar` (enum) for fixed-size scalar types, and `VectorType` (record) for multi-element types parameterized by element scalar and count. FlatEntityState SHALL delegate all type-specific read/write operations to PrimitiveType — it SHALL NOT contain type-specific logic itself.

The static VarHandle instances SHALL be created via `MethodHandles.byteArrayViewVarHandle` with `ByteOrder.LITTLE_ENDIAN`.

```java
sealed interface PrimitiveType {
    int size();
    void write(byte[] data, int offset, Object value);
    Object read(byte[] data, int offset);

    enum Scalar implements PrimitiveType {
        INT(4), FLOAT(4), LONG(8), BOOL(1);
        // Each variant implements write/read via its VarHandle
        // FLOAT additionally provides writeRaw(byte[], int, float) / readRaw for VectorType
    }

    record VectorType(Scalar element, int count) implements PrimitiveType {
        // size = count * element.size()
        // write: decompose Vector, N× element.writeRaw
        // read: N× element.readRaw, reconstruct Vector
    }
}
```

#### Scenario: Scalar variants and their mappings

| Scalar | size | VarHandle | write | read |
|---|---|---|---|---|
| INT | 4 | INT_VH | `INT_VH.set(data, offset, ((Integer)value).intValue())` | `(Integer) INT_VH.get(data, offset)` |
| FLOAT | 4 | FLOAT_VH | `FLOAT_VH.set(data, offset, ((Float)value).floatValue())` | `(Float) FLOAT_VH.get(data, offset)` |
| LONG | 8 | LONG_VH | `LONG_VH.set(data, offset, ((Long)value).longValue())` | `(Long) LONG_VH.get(data, offset)` |
| BOOL | 1 | (none) | `data[offset] = ((Boolean)value) ? (byte)1 : (byte)0` | `data[offset] != 0` |

#### Scenario: VectorType delegates element access to its Scalar

- **WHEN** `VectorType(FLOAT, 3).write(data, offset, value)` is called
- **THEN** it decomposes the `Vector` into 3 floats and writes each via `FLOAT.writeRaw(data, offset + i * 4, v.getElement(i))`
- **WHEN** `VectorType(FLOAT, 3).read(data, offset)` is called
- **THEN** it reads 3 floats via `FLOAT.readRaw(data, offset + i * 4)` and reconstructs a `Vector`
- **AND** `VectorType(FLOAT, 2)` handles Vector2D, `VectorType(FLOAT, 4)` handles Vector4D/Quaternion — no separate variants needed

### Requirement: Each Decoder provides its own PrimitiveType

The `Decoder` abstract class SHALL provide a `getPrimitiveType()` method (default: `null`). Each concrete decoder SHALL override this to return the `PrimitiveType` it produces. The FieldLayoutBuilder SHALL use `decoder.getPrimitiveType()` to determine the layout — no separate Decoder→PrimitiveType mapping.

A return value of `null` indicates a reference type (String) — the FieldLayoutBuilder creates a `FieldLayout.Ref` instead of a `Primitive`.

#### Scenario: Decoder PrimitiveType assignments

- **WHEN** `IntVarUnsignedDecoder.getPrimitiveType()` is called → returns `Scalar.INT`
- **WHEN** `IntVarSignedDecoder.getPrimitiveType()` is called → returns `Scalar.INT`
- **WHEN** `IntUnsignedDecoder.getPrimitiveType()` is called → returns `Scalar.INT`
- **WHEN** `IntSignedDecoder.getPrimitiveType()` is called → returns `Scalar.INT`
- **WHEN** `IntMinusOneDecoder.getPrimitiveType()` is called → returns `Scalar.INT`
- **WHEN** `FloatDefaultDecoder.getPrimitiveType()` is called → returns `Scalar.FLOAT`
- **WHEN** any other float decoder's `getPrimitiveType()` is called → returns `Scalar.FLOAT`
- **WHEN** any long decoder's `getPrimitiveType()` is called → returns `Scalar.LONG`
- **WHEN** `BoolDecoder.getPrimitiveType()` is called → returns `Scalar.BOOL`
- **WHEN** `VectorDecoder.getPrimitiveType()` is called → returns `new VectorType(Scalar.FLOAT, 3)`
- **WHEN** `VectorNormalDecoder.getPrimitiveType()` is called → returns `new VectorType(Scalar.FLOAT, 3)`
- **WHEN** `VectorXYDecoder.getPrimitiveType()` is called → returns `new VectorType(Scalar.FLOAT, 2)`
- **WHEN** `VectorDefaultDecoder.getPrimitiveType()` is called → returns `new VectorType(Scalar.FLOAT, dim)` where `dim` is the decoder's dimension field
- **WHEN** any QAngle decoder's `getPrimitiveType()` is called → returns `new VectorType(Scalar.FLOAT, 3)`
- **WHEN** `StringZeroTerminatedDecoder.getPrimitiveType()` is called → returns `null`

#### Scenario: FieldLayoutBuilder uses Decoder.getPrimitiveType()

- **WHEN** the FieldLayoutBuilder encounters a ValueField
- **AND** `valueField.getDecoder().getPrimitiveType()` returns a non-null PrimitiveType
- **THEN** it creates `FieldLayout.Primitive(offset, primitiveType)` and advances the offset by `1 + primitiveType.size()`
- **WHEN** `valueField.getDecoder().getPrimitiveType()` returns `null`
- **THEN** it creates `FieldLayout.Ref(refIndex++)` without advancing the byte offset

### Requirement: FieldLayout tree mirrors Field hierarchy with offsets

The system SHALL provide a `FieldLayout` sealed interface in the `skadistats.clarity.model.state` package. The FieldLayout tree SHALL mirror the Field hierarchy but be completely independent of it — no references between FieldLayout and Field/Serializer/Decoder in either direction. The FieldLayout tree stores offsets and type information; the Field hierarchy stores structure.

```java
sealed interface FieldLayout {
    record Primitive(int offset, PrimitiveType type) implements FieldLayout {}
    record Ref(int refIndex) implements FieldLayout {}
    record Composite(FieldLayout[] children) implements FieldLayout {}
    record Array(int baseOffset, int stride, int length, FieldLayout element) implements FieldLayout {}
    record SubState(int refIndex, SubStateKind kind) implements FieldLayout {}
}
```

`Array.length` (from `ArrayField.getLength()`) is needed by `fieldPathIterator()` to know how many elements to scan.

#### Scenario: FieldLayout variants and their Field counterparts

| FieldLayout variant | Produced from | Role |
|---|---|---|
| Primitive(offset, type) | ValueField with primitive/vector decoder | Leaf: typed value in byte[] at offset |
| Ref(refIndex) | ValueField with String decoder | Leaf: reference in Object[] sidecar |
| Composite(children[]) | SerializerField | Branch: children indexed by field index |
| Array(baseOffset, stride, element) | ArrayField | Branch: fixed-length array with uniform elements |
| SubState(refIndex, kind) | VectorField or PointerField | Sub-FlatEntityState in Object[] sidecar |

#### Scenario: Offset semantics — Composite children are flattened

- **WHEN** the FieldLayoutBuilder computes the layout for a Serializer with sub-serializers (SerializerField)
- **THEN** the sub-serializer's children receive offsets that continue from the parent's offset cursor
- **AND** all Primitive offsets within a single FlatEntityState's byte[] are absolute (relative to byte[0])
- **AND** the `base` accumulator in the traversal loop is only modified by Array, never by Composite

#### Scenario: Array element offsets are relative to element start

- **WHEN** the FieldLayoutBuilder computes the layout for an ArrayField
- **THEN** the element layout's offsets start at 0 (relative to the element's start position)
- **AND** Array.baseOffset is the absolute position of the array's first element in byte[]
- **AND** at runtime, element `i` is accessed at `base + baseOffset + i * stride`

#### Scenario: Presence tracking via flag byte

- **WHEN** layout computation assigns a Primitive slot
- **THEN** the slot occupies `1 + type.size` bytes total (1 flag byte + value bytes)
- **AND** the offset in the Primitive record points to the flag byte position
- **AND** the value is written/read at `offset + 1`
- **WHEN** `applyMutation` writes a WriteValue to a Primitive at offset
- **THEN** it sets `data[base + offset] = 1` (flag) and writes the value via `type.write(data, base + offset + 1, value)`
- **WHEN** `getValueForFieldPath` reads from a Primitive where `data[base + offset] == 0`
- **THEN** it returns `null`

### Requirement: SubStateKind distinguishes Vector from Pointer layout metadata

The system SHALL provide a `SubStateKind` sealed interface to hold the layout metadata needed to manage sub-FlatEntityStates for VectorField and PointerField.

```java
sealed interface SubStateKind {
    record Vector(int elementBytes, int elementRefs,
                  FieldLayout elementLayout) implements SubStateKind {}
    record Pointer(FieldLayout[] layouts, int[] layoutBytes,
                   int[] layoutRefs) implements SubStateKind {}
}
```

#### Scenario: VectorField produces SubState with Vector kind

- **WHEN** the FieldLayoutBuilder encounters a VectorField
- **THEN** it computes the element layout (starting at offset 0) and creates `SubState(refIndex, Vector(elementBytes, elementRefs, elementLayout))`
- **AND** the sub-FlatEntityState uses `Array(baseOffset=0, stride=elementBytes, element=elementLayout)` as its rootLayout

#### Scenario: PointerField produces SubState with Pointer kind

- **WHEN** the FieldLayoutBuilder encounters a PointerField with N possible serializers
- **THEN** it creates `SubState(refIndex, Pointer(layouts[], layoutBytes[], layoutRefs[]))` for the sub-state
- **AND** `layouts[]` contains the pre-computed FieldLayout for each possible child Serializer

### Requirement: FlatEntityState dispatches on StateMutation via applyMutation

The system SHALL provide a `FlatEntityState` class implementing the `EntityState` interface. It SHALL store primitive values in a `byte[]` and reference types / sub-states in an `Object[]` sidecar.

`applyMutation(FieldPath, StateMutation)` SHALL traverse the FieldLayout tree using a `base` accumulator, a `layout` cursor, and a `current` FlatEntityState reference. When a SubState is encountered mid-traversal, the loop SHALL first ensure the current state is modifiable (so its `refs[]` can be updated), then COW-clone the sub-state if it is non-modifiable, replace it in `refs[]`, swap `current` to the new sub-state, and `continue` to reprocess the current FieldPath index. This cascading COW ensures exclusive ownership at each level before writing.

At the leaf, the method SHALL dispatch on the `StateMutation` type, passing `current` to write operations:

```java
public boolean applyMutation(FieldPath fpX, StateMutation op) {
    var fp = fpX.s2();
    FlatEntityState current = this;
    byte[] data = this.data;
    Object[] refs = this.refs;
    FieldLayout layout = this.rootLayout;
    int base = 0;
    var last = fp.last();

    int i = 0;
    while (true) {
        var idx = fp.get(i);
        switch (layout) {
            case Composite c -> layout = c.children[idx];
            case Array a     -> { base += a.baseOffset + idx * a.stride; layout = a.element; }
            case SubState s  -> {
                current.ensureModifiable();
                data = current.data; refs = current.refs;
                var sub = (FlatEntityState) refs[s.refIndex];
                if (!sub.modifiable) {
                    sub = new FlatEntityState(sub.rootLayout, sub.data.clone(), sub.refs.clone());
                    sub.modifiable = true;
                    refs[s.refIndex] = sub;
                }
                current = sub;
                data = sub.data; refs = sub.refs;
                layout = sub.rootLayout;
                base = 0;
                continue;
            }
            default -> throw new IllegalStateException();
        }
        if (i == last) break;
        i++;
    }

    // leaf: dispatch on StateMutation
    return switch (op) {
        case StateMutation.WriteValue wv -> writeValue(current, layout, base, wv.value());
        case StateMutation.ResizeVector rv -> resizeVector(layout, refs, rv.count());
        case StateMutation.SwitchPointer sp -> switchPointer(layout, refs, sp.newSerializer());
    };
}

private boolean writeValue(FlatEntityState target, FieldLayout layout, int base, Object value) {
    switch (layout) {
        case Primitive p -> {
            target.ensureModifiable();
            var data = target.data;
            data[base + p.offset] = 1;
            p.type().write(data, base + p.offset + 1, value);
            return false;
        }
        case Ref r -> {
            target.ensureModifiable();
            target.refs[r.refIndex] = value;
            return false;
        }
        default -> throw new IllegalStateException();
    }
}
```

#### Scenario: WriteValue on a Primitive field

- **WHEN** `applyMutation` receives a `WriteValue` and the leaf layout is a Primitive
- **THEN** it sets the presence flag and writes via `PrimitiveType.write`
- **AND** returns `false` (no capacity change)

#### Scenario: WriteValue on a Ref field

- **WHEN** `applyMutation` receives a `WriteValue` and the leaf layout is a Ref
- **THEN** it stores the value in `refs[refIndex]`
- **AND** returns `false`

#### Scenario: ResizeVector on a SubState

- **WHEN** `applyMutation` receives a `ResizeVector(count)` and the leaf layout is a SubState(Vector)
- **THEN** the sub-FlatEntityState's byte[] is resized to `count * elementBytes`
- **AND** its refs[] is resized to `count * elementRefs` (if elementRefs > 0)
- **AND** existing data is preserved up to `min(oldCount, newCount)` elements
- **AND** returns `true` (capacity changed)

#### Scenario: SwitchPointer on a SubState

- **WHEN** `applyMutation` receives a `SwitchPointer(newSerializer)` and the leaf layout is a SubState(Pointer)
- **AND** the new serializer differs from the current sub-state's serializer
- **THEN** the sub-FlatEntityState is replaced with a new instance using the pre-computed layout for `newSerializer`
- **WHEN** the newSerializer is null
- **THEN** the sub-FlatEntityState is cleared (set to null in refs[])

#### Scenario: Traversal through nested Composites (sub-serializer access)

- **WHEN** `applyMutation` is called with FieldPath `[2, 1]` where children[2] is a Composite (sub-serializer)
- **THEN** i=0 advances layout to `Composite.children[2]`, base stays 0
- **AND** i=1 advances layout to the inner `Composite.children[1]` (a Primitive)
- **AND** the WriteValue is applied at `base(0) + offset` using the Primitive's PrimitiveType

#### Scenario: Traversal through Array (fixed-length array access)

- **WHEN** `applyMutation` is called with FieldPath `[3, 7]` where children[3] is an Array(baseOffset=33, stride=5)
- **THEN** i=0 advances layout to the Array
- **AND** i=1 computes `base = 0 + 33 + 7*5 = 68`, layout = element Primitive(offset=0)
- **AND** the WriteValue is applied at `base(68) + offset(0) = 68`

#### Scenario: Traversal through SubState with context switch

- **WHEN** `applyMutation` is called with FieldPath `[4, 2]` where children[4] is SubState(refIndex=0)
- **THEN** i=0 advances layout to SubState via Composite
- **AND** i=1 encounters SubState, swaps `current` to sub-FlatEntityState (+ data, refs, layout, base=0), and `continue`s
- **AND** i=1 is reprocessed with the sub-state's Array layout: `base = 0 + 2*stride`, layout = element
- **AND** the WriteValue calls `current.ensureModifiable()` (targeting the sub-state), re-reads `current.data`, and writes in the sub-state's byte[]

#### Scenario: SubState as leaf (structural operation)

- **WHEN** `applyMutation` is called with a `ResizeVector` and FieldPath `[4]` where children[4] is SubState
- **THEN** i=0 advances layout to SubState via Composite
- **AND** the loop breaks (i == last)
- **AND** the ResizeVector op is handled: sub-state is resized

### Requirement: getValueForFieldPath uses same traversal

`getValueForFieldPath` SHALL use the same FieldLayout traversal loop (Composite, Array, SubState with continue). At the leaf:

#### Scenario: Read a primitive value

- **WHEN** the leaf is a Primitive and `data[base + p.offset] != 0` (presence flag set)
- **THEN** return `p.type().read(data, base + p.offset + 1)`
- **WHEN** the presence flag is 0
- **THEN** return `null`

#### Scenario: Read a reference value

- **WHEN** the leaf is a Ref
- **THEN** return `refs[r.refIndex]`

### Requirement: FlatEntityState supports copy-on-write

`copy()` SHALL:
1. Recursively mark all sub-FlatEntityStates in `refs[]` as `modifiable = false` (they become frozen snapshots)
2. Clone `data` and `refs` (shallow copy)
3. Mark both the original and the copy as `modifiable = false`

On first write to a non-modifiable state, `ensureModifiable()` SHALL clone `data` and `refs` and set `modifiable = true`.

On first write through a sub-state path, `applyMutation` SHALL COW-clone the sub-state object and replace it in the parent's `refs[]` before writing (see applyMutation SubState traversal). This cascading COW gives each parent exclusive ownership of its sub-states.

#### Scenario: Copy and modify independently

- **WHEN** a FlatEntityState is copied via `copy()`
- **AND** `applyMutation` is called on the copy
- **THEN** `ensureModifiable()` clones byte[] and refs[] before writing
- **AND** the original's data is unchanged

#### Scenario: SubState COW — clone-and-replace at boundary

- **WHEN** a FlatEntityState is copied and the copy's sub-state is written to
- **THEN** `copy()` has marked all sub-states as `modifiable = false` (frozen snapshots)
- **AND** both original and copy's `refs[]` point to the same sub-state objects (shallow clone)
- **WHEN** `applyMutation` traverses through a SubState on the copy
- **THEN** it first calls `current.ensureModifiable()` — the copy's `refs[]` is now exclusively owned
- **AND** it clones the sub-state object (`new FlatEntityState(sub.rootLayout, sub.data.clone(), sub.refs.clone())`)
- **AND** it replaces `refs[s.refIndex]` with the clone — the original's refs still points to the frozen snapshot
- **AND** writes proceed on the clone, leaving the original's sub-state unchanged

### Requirement: FlatEntityState provides fieldPathIterator

`fieldPathIterator()` SHALL iterate over all FieldPaths whose values have been set. It SHALL walk the FieldLayout tree and check presence flags for Primitives, non-null checks for Refs and SubStates. Structural operations (ResizeVector, SwitchPointer) do not store values, so their paths are naturally excluded.

#### Scenario: Iterate only set field paths

- **WHEN** `fieldPathIterator()` is called
- **THEN** it yields only FieldPaths whose Primitive flag byte is non-zero, whose Ref is non-null, or whose SubState exists
- **AND** for SubStates, it delegates to the sub-state's iterator to include nested FieldPaths

#### Scenario: Iteration builds FieldPaths from layout structure

- **WHEN** the iterator walks a Composite with 3 children where children[0] and children[2] are set
- **THEN** it yields FieldPaths with indices [0] and [2] (skipping [1])
- **AND** for nested Composites, it extends the FieldPath with additional indices

### Requirement: FieldLayoutBuilder computes layouts from Field hierarchy

The system SHALL provide a `FieldLayoutBuilder` in the `skadistats.clarity.model.state` package. It SHALL walk a Serializer's Field hierarchy once and produce a FieldLayout tree with computed offsets, a total byte count, and a total ref count.

#### Scenario: Layout computation algorithm

- **WHEN** `buildLayout(Serializer, startOffset=0)` is called
- **THEN** a running offset cursor starts at startOffset
- **AND** for each ValueField with primitive decoder: creates `Primitive(offset, type)`, advances offset by `1 + type.size`
- **AND** for each ValueField with String decoder: creates `Ref(refIndex++)`, offset unchanged
- **AND** for each SerializerField: recursively builds with the SAME running offset (flattening)
- **AND** for each ArrayField(element, length): builds element layout at startOffset=0, creates `Array(offset, stride, elementLayout)`, advances offset by `length * stride`
- **AND** for each VectorField: builds element layout at startOffset=0, creates `SubState(refIndex++, Vector(...))`
- **AND** for each PointerField: builds layouts for each possible serializer, creates `SubState(refIndex++, Pointer(...))`

#### Scenario: Layout caching per Serializer

- **WHEN** `buildLayout` is called for a Serializer that has already been computed
- **THEN** the cached result is returned
- **AND** caching is safe because Field types and Decoders are immutable after Serializer construction

### Requirement: Runner-configurable EntityStateType

The `S2EntityStateType` enum SHALL include a `FLAT` variant that produces FlatEntityState instances. The default SHALL remain `NESTED_ARRAY`.

#### Scenario: Configure FlatEntityState via Runner

- **WHEN** `new SimpleRunner(source).withS2EntityState(S2EntityStateType.FLAT).runWith(processor)` is called
- **THEN** all S2 entities created during the run use `FlatEntityState`

#### Scenario: Default to NestedArrayEntityState

- **WHEN** no `withS2EntityState()` is called
- **THEN** all S2 entities use `NestedArrayEntityState`
