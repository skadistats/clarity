## ADDED Requirements

### Requirement: PrimitiveType enum encapsulates typed byte[] access

The system SHALL provide a `PrimitiveType` enum in the `skadistats.clarity.model.state` package. Each variant SHALL encapsulate the byte size, the `VarHandle` for byte[] access, and the `write(byte[], int, Object)` / `read(byte[], int) -> Object` logic for one value type. FlatEntityState SHALL delegate all type-specific read/write operations to PrimitiveType — it SHALL NOT contain type-specific logic itself.

The static VarHandle instances SHALL be created via `MethodHandles.byteArrayViewVarHandle` with `ByteOrder.LITTLE_ENDIAN`.

#### Scenario: PrimitiveType variants and their mappings

| PrimitiveType | size (bytes) | VarHandle | write | read |
|---|---|---|---|---|
| INT | 4 | INT_VH | `INT_VH.set(data, offset, ((Integer)value).intValue())` | `(Integer) INT_VH.get(data, offset)` |
| FLOAT | 4 | FLOAT_VH | `FLOAT_VH.set(data, offset, ((Float)value).floatValue())` | `(Float) FLOAT_VH.get(data, offset)` |
| LONG | 8 | LONG_VH | `LONG_VH.set(data, offset, ((Long)value).longValue())` | `(Long) LONG_VH.get(data, offset)` |
| BOOL | 1 | (none) | `data[offset] = ((Boolean)value) ? (byte)1 : (byte)0` | `data[offset] != 0` |
| VECTOR2 | 8 | FLOAT_VH | decompose Vector into 2 floats at offset, offset+4 | reconstruct Vector from 2 floats |
| VECTOR3 | 12 | FLOAT_VH | decompose Vector into 3 floats at offset, offset+4, offset+8 | reconstruct Vector from 3 floats |

#### Scenario: Decoder type determines PrimitiveType

- **WHEN** the FieldLayoutBuilder encounters a ValueField with an int decoder (IntUnsignedDecoder, IntSignedDecoder, IntMinusOneDecoder, IntVarUnsignedDecoder, IntVarSignedDecoder)
- **THEN** it assigns `PrimitiveType.INT`
- **WHEN** the FieldLayoutBuilder encounters a ValueField with a float decoder (FloatDefaultDecoder, FloatNoScaleDecoder, FloatCoordDecoder, FloatCoordMpDecoder, FloatCellCoordDecoder, FloatQuantizedDecoder, FloatNormalDecoder)
- **THEN** it assigns `PrimitiveType.FLOAT`
- **WHEN** the FieldLayoutBuilder encounters a ValueField with a long decoder (LongUnsignedDecoder, LongSignedDecoder, LongVarUnsignedDecoder, LongVarSignedDecoder)
- **THEN** it assigns `PrimitiveType.LONG`
- **WHEN** the FieldLayoutBuilder encounters a ValueField with a BoolDecoder
- **THEN** it assigns `PrimitiveType.BOOL`
- **WHEN** the FieldLayoutBuilder encounters a ValueField with a VectorDecoder, VectorNormalDecoder, QAngleBitCountDecoder, QAngleNoBitCountDecoder, QAngleNoScaleDecoder, QAnglePitchYawOnlyDecoder, or QAnglePreciseDecoder
- **THEN** it assigns `PrimitiveType.VECTOR3`
- **WHEN** the FieldLayoutBuilder encounters a ValueField with a VectorXYDecoder
- **THEN** it assigns `PrimitiveType.VECTOR2`
- **WHEN** the FieldLayoutBuilder encounters a ValueField with a VectorDefaultDecoder
- **THEN** it assigns `PrimitiveType.VECTOR2` if dim=2, or `PrimitiveType.VECTOR3` if dim=3

### Requirement: FieldLayout tree mirrors Field hierarchy with offsets

The system SHALL provide a `FieldLayout` sealed interface in the `skadistats.clarity.model.state` package. The FieldLayout tree SHALL mirror the Field hierarchy but be completely independent of it — no references between FieldLayout and Field/Serializer/Decoder in either direction. The FieldLayout tree stores offsets and type information; the Field hierarchy stores structure.

```java
sealed interface FieldLayout {
    record Primitive(int offset, PrimitiveType type) implements FieldLayout {}
    record Ref(int refIndex) implements FieldLayout {}
    record Composite(FieldLayout[] children) implements FieldLayout {}
    record Array(int baseOffset, int stride, FieldLayout element) implements FieldLayout {}
    record SubState(int refIndex, SubStateKind kind) implements FieldLayout {}
}
```

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

`applyMutation(FieldPath, StateMutation)` SHALL traverse the FieldLayout tree using a `base` accumulator and a `layout` cursor. When a SubState is encountered mid-traversal, the loop SHALL swap to the sub-FlatEntityState's data/refs/layout/base and `continue` to reprocess the current FieldPath index with the new context.

At the leaf, the method SHALL dispatch on the `StateMutation` type:

```java
public boolean applyMutation(FieldPath fpX, StateMutation op) {
    var fp = fpX.s2();
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
                var sub = (FlatEntityState) refs[s.refIndex];
                data = sub.data;
                refs = sub.refs;
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
        case StateMutation.WriteValue wv -> writeValue(layout, data, refs, base, wv.value());
        case StateMutation.ResizeVector rv -> resizeVector(layout, refs, rv.count());
        case StateMutation.SwitchPointer sp -> switchPointer(layout, refs, sp.newSerializer());
    };
}

private boolean writeValue(FieldLayout layout, byte[] data, Object[] refs, int base, Object value) {
    switch (layout) {
        case Primitive p -> {
            ensureModifiable();
            data[base + p.offset] = 1;
            p.type().write(data, base + p.offset + 1, value);
            return false;
        }
        case Ref r -> {
            ensureModifiable();
            refs[r.refIndex] = value;
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
- **THEN** i=0 advances layout to SubState
- **AND** i=1 encounters SubState, swaps to sub-FlatEntityState (data, refs, layout, base=0), and `continue`s
- **AND** i=1 is reprocessed with the sub-state's Array layout: `base = 0 + 2*stride`, layout = element
- **AND** the WriteValue is applied in the sub-state's byte[]

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

`copy()` SHALL return a new FlatEntityState with:
- `data` = `this.data.clone()`
- `refs` = `this.refs.clone()` (shallow copy)
- Both the original and the copy marked as `modifiable = false`

On first write to a non-modifiable state, `ensureModifiable()` SHALL clone `data` and `refs` and set `modifiable = true`.

#### Scenario: Copy and modify independently

- **WHEN** a FlatEntityState is copied via `copy()`
- **AND** `applyMutation` is called on the copy
- **THEN** `ensureModifiable()` clones byte[] and refs[] before writing
- **AND** the original's data is unchanged

#### Scenario: SubState COW

- **WHEN** a FlatEntityState is copied and the copy's sub-state is written to
- **THEN** the sub-FlatEntityState in refs[] is shared (shallow copy)
- **AND** the sub-state itself handles COW independently (its own modifiable flag)
- **AND** the copy's `ensureModifiable()` clones refs[], giving it its own reference to the sub-state
- **AND** the sub-state's `ensureModifiable()` clones its own data when first written

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
