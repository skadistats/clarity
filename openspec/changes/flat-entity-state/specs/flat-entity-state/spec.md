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

The `Decoder` abstract class SHALL provide a `getPrimitiveType()` method with default return value `null`. Each concrete decoder SHALL override this method to return the `PrimitiveType` it produces. The FieldLayoutBuilder and any other consumer SHALL determine layout type solely by calling `decoder.getPrimitiveType()` — **no code outside the decoder package may depend on concrete decoder classes or branch on decoder identity**. Adding a new decoder or changing an existing one's produced type is purely a local change to that decoder.

A return value of `null` indicates a reference type (value stored via `FieldLayout.Ref`). Any decoder whose decoded value cannot be stored in a fixed-size primitive slot SHALL inherit the default `null`.

#### Scenario: Decoder is the sole authority on its PrimitiveType

- **WHEN** a concrete decoder produces a scalar value (int / float / long / boolean)
- **THEN** it overrides `getPrimitiveType()` to return the matching `Scalar` variant
- **WHEN** a concrete decoder produces a fixed-dimension vector of floats
- **THEN** it overrides `getPrimitiveType()` to return `new VectorType(Scalar.FLOAT, dim)` with the decoder's own dimension
- **WHEN** a concrete decoder produces a value that is not representable as a fixed-size primitive
- **THEN** it leaves `getPrimitiveType()` at its default `null`
- **AND** no external mapping or type switch is needed — `FieldLayoutBuilder` simply invokes the method

#### Scenario: FieldLayoutBuilder uses Decoder.getPrimitiveType()

- **WHEN** the FieldLayoutBuilder encounters a ValueField
- **AND** `valueField.getDecoder().getPrimitiveType()` returns a non-null PrimitiveType
- **THEN** it creates `FieldLayout.Primitive(offset, primitiveType)` and advances the offset by `1 + primitiveType.size()`
- **WHEN** `valueField.getDecoder().getPrimitiveType()` returns `null`
- **THEN** it creates `FieldLayout.Ref(offset)` and advances the offset by `1 + 4` (flag + slot-index)
- **AND** the FieldLayoutBuilder SHALL NOT inspect the decoder's concrete class

### Requirement: FieldLayout tree mirrors Field hierarchy with byte offsets

The system SHALL provide a `FieldLayout` sealed interface in the `skadistats.clarity.model.state` package. The FieldLayout tree SHALL mirror the Field hierarchy but be completely independent of it — no references between FieldLayout and Field/Serializer/Decoder in either direction. The FieldLayout tree stores **byte offsets** and type information; the Field hierarchy stores structure. The layout has NO refIndex counter — refs are dynamically allocated at write time.

```java
sealed interface FieldLayout {
    record Primitive(int offset, PrimitiveType type) implements FieldLayout {}
    record Ref(int offset) implements FieldLayout {}
    record Composite(FieldLayout[] children) implements FieldLayout {}
    record Array(int baseOffset, int stride, int length, FieldLayout element) implements FieldLayout {}
    record SubState(int offset, SubStateKind kind) implements FieldLayout {}
}
```

`Array.length` (from `ArrayField.getLength()`) is needed by `fieldPathIterator()` to know how many elements to scan. `Array` has NO refStride — refs within array elements work automatically because each element has its own byte-offset region containing its own slot-indices.

#### Scenario: FieldLayout variants and their Field counterparts

| FieldLayout variant | Produced from | Role |
|---|---|---|
| Primitive(offset, type) | ValueField with primitive/vector decoder | Leaf: typed value in byte[] at offset |
| Ref(offset) | ValueField with String decoder | Leaf: flag + dynamically-allocated slot-index in byte[] |
| Composite(children[]) | SerializerField | Branch: children indexed by field index |
| Array(baseOffset, stride, length, element) | ArrayField | Branch: fixed-length array with uniform elements |
| SubState(offset, kind) | VectorField or PointerField | Flag + dynamically-allocated slot-index pointing to sub-Entry in global refs |

#### Scenario: Offset semantics — Composite children are flattened

- **WHEN** the FieldLayoutBuilder computes the layout for a Serializer with sub-serializers (SerializerField)
- **THEN** the sub-serializer's children receive offsets that continue from the parent's offset cursor
- **AND** all Primitive/Ref/SubState offsets within a single Entry's byte[] are absolute (relative to byte[0])
- **AND** the `base` accumulator in the traversal loop is only modified by Array, never by Composite

#### Scenario: Array element offsets are relative to element start

- **WHEN** the FieldLayoutBuilder computes the layout for an ArrayField
- **THEN** the element layout's offsets start at 0 (relative to the element's start position)
- **AND** Array.baseOffset is the absolute position of the array's first element in byte[]
- **AND** Array.stride equals the element layout's totalBytes (works for primitive, composite, ref-containing, or sub-state-containing elements)
- **AND** at runtime, element `i` is accessed at `base + baseOffset + i * stride`

#### Scenario: Array with String element works via per-element slot-indices

- **WHEN** an ArrayField has a String element (length=N)
- **THEN** the element layout is `Ref(offset=0)` occupying 1+4 bytes
- **AND** Array.stride = 5, length = N
- **AND** each array element's byte-slot holds its own dynamically-allocated refs slot-index
- **AND** elements are written/read independently without refIndex collision

#### Scenario: Array with composite element containing refs

- **WHEN** an ArrayField has a composite element containing a String and an int (length=N)
- **THEN** the element layout is `Composite[Ref(offset=0), Primitive(offset=5, INT)]`
- **AND** element totalBytes = 10 (5 for Ref + 5 for int Primitive)
- **AND** Array.stride = 10
- **AND** each element independently allocates its own String slot-index when written

#### Scenario: Presence tracking via flag byte — three slot schemas

- **WHEN** layout computation assigns a Primitive slot
- **THEN** the slot occupies `1 + type.size` bytes total (1 flag byte + value bytes)
- **AND** the offset in the Primitive record points to the flag byte position
- **AND** the value is written/read at `offset + 1`
- **WHEN** layout computation assigns a Ref slot
- **THEN** the slot occupies `1 + 4` bytes (1 flag byte + int32 slot-index LE)
- **AND** on first write the slot-index is dynamically allocated via `FlatEntityState.allocateRefSlot()` and stored at `offset + 1`
- **AND** on read: `flag == 0 ? null : refs.get(INT_VH.get(data, offset + 1))`
- **WHEN** layout computation assigns a SubState slot
- **THEN** the slot occupies `1 + 4` bytes (same schema as Ref)
- **AND** the slot holds an `Entry` instance in `FlatEntityState.refs`
- **WHEN** `applyMutation` writes a WriteValue to a Primitive at offset
- **THEN** it sets `data[base + offset] = 1` (flag) and writes the value via `type.write(data, base + offset + 1, value)`
- **WHEN** `getValueForFieldPath` reads from a Primitive where `data[base + offset] == 0`
- **THEN** it returns `null`

### Requirement: SubStateKind distinguishes Vector from Pointer layout metadata

The system SHALL provide a `SubStateKind` sealed interface to hold the layout metadata needed to manage sub-Entry instances for VectorField and PointerField.

```java
sealed interface SubStateKind {
    record Vector(int elementBytes, FieldLayout elementLayout) implements SubStateKind {}
    record Pointer(int pointerId, FieldLayout[] layouts, int[] layoutBytes) implements SubStateKind {}
}
```

`SubStateKind.Pointer` carries the `pointerId` so that `FlatEntityState` can update `pointerSerializers[]` (inherited from `AbstractS2EntityState`) without accessing the Field hierarchy during mutation traversal.

#### Scenario: VectorField produces SubState with Vector kind

- **WHEN** the FieldLayoutBuilder encounters a VectorField
- **THEN** it computes the element layout (starting at offset 0) and creates `SubState(offset=cursor, Vector(elementBytes, elementLayout))`
- **AND** the cursor advances by `1 + 4` (flag + slot-index) for this SubState
- **AND** the sub-Entry created on first `ResizeVector(count)` uses `Array(baseOffset=0, stride=elementBytes, length=count, element=elementLayout)` as its rootLayout

#### Scenario: PointerField produces SubState with Pointer kind

- **WHEN** the FieldLayoutBuilder encounters a PointerField with N possible serializers
- **THEN** it creates `SubState(offset=cursor, Pointer(pointerId, layouts[], layoutBytes[]))`
- **AND** the cursor advances by `1 + 4` (flag + slot-index)
- **AND** `pointerId` is taken from `PointerField.getPointerId()`
- **AND** `layouts[]` contains the pre-computed FieldLayout for each possible child Serializer
- **AND** `layoutBytes[]` contains the corresponding total byte size for each layout, used when allocating the sub-Entry's byte[]

### Requirement: FlatEntityState extends AbstractS2EntityState with global refs and lean Entry

The system SHALL provide a `FlatEntityState` class extending `AbstractS2EntityState`. It SHALL own a global refs container (shared across all nesting levels) and a tree of `Entry` instances that each hold only a `byte[]`. This mirrors `NestedArrayEntityState`'s pattern of a global `List<Entry> entries` with a `Deque<Integer> freeEntries` free-list (NestedArrayEntityState.java:17-18, 142-164).

`FlatEntityState` inherits from `AbstractS2EntityState`:
- `rootField` (SerializerField) — for field path name resolution and field navigation
- `pointerSerializers[]` — global, flat pointer serializer tracking (shared across all nesting levels)
- `getFieldForFieldPath()`, `getNameForFieldPath()`, `getFieldPathForName()`, `getTypeForFieldPath()` — operate on the Field hierarchy

`FlatEntityState` additionally holds:
- `ArrayList<Object> refs` — global container for Strings AND sub-Entry instances
- `Deque<Integer> freeSlots` — global free-list for refs recycling
- `boolean refsModifiable` — COW flag for the refs container
- `Entry rootEntry` — the root Entry containing the flat primitive storage

The inner `Entry` class is a pure byte[] container. It holds:
- `FieldLayout rootLayout` — the layout tree for this Entry's scope
- `byte[] data` — primitive value bytes and slot-indices for refs/sub-states
- `boolean modifiable` — COW flag

Sub-states for VectorField and PointerField are `Entry` instances living in `FlatEntityState.refs` at dynamically-allocated slot indices. They have NO refs of their own — all String values and nested sub-Entries across all nesting levels live in the single `FlatEntityState.refs` container. They do NOT carry their own pointerSerializers — pointer tracking is global on the outer `FlatEntityState`.

#### Scenario: Sub-states are Entry instances in FlatEntityState.refs

- **WHEN** a VectorField or PointerField sub-state is created
- **THEN** an `Entry` instance is created with its own `byte[] data` and `rootLayout`
- **AND** a slot is allocated in `FlatEntityState.refs` via `allocateRefSlot()`
- **AND** the Entry is stored at that slot via `refs.set(slot, entry)`
- **AND** the slot-index is stored in the parent Entry's `byte[]` at the SubState's offset+1
- **AND** the SubState's flag byte (at offset) is set to 1

#### Scenario: Dynamic ref slot allocation

- **WHEN** a WriteValue on a Ref is applied and the flag byte was 0
- **THEN** a slot is allocated from `FlatEntityState.refs` via `allocateRefSlot()` (reusing a free slot or appending)
- **AND** the slot-index is written to `data[offset+1]` via `INT_VH.set`
- **AND** the flag byte is set to 1
- **AND** `refs.set(slot, value)` stores the value
- **WHEN** a WriteValue on a Ref is applied and the flag byte was 1
- **THEN** the existing slot-index is read from `data[offset+1]`
- **AND** `refs.set(slot, value)` updates the value in place (no new allocation)

#### Scenario: Ref slot freed on null write

- **WHEN** a WriteValue with `value == null` is applied to a Ref with flag=1
- **THEN** the slot-index is read, `freeRefSlot(slot)` marks the slot reusable, and the flag is cleared

### Requirement: FlatEntityState dispatches on StateMutation via applyMutation

`applyMutation(FieldPath, StateMutation)` SHALL traverse the FieldLayout tree using a `base` accumulator, a `layout` cursor, and a `current` Entry reference. When a SubState is encountered mid-traversal, the loop SHALL read the slot-index from `current.data[base + s.offset + 1]`, look up the sub-Entry in `FlatEntityState.refs`, COW-clone it if non-modifiable (calling `ensureRefsModifiable()` first so the refs container itself is writable), update `refs.set(slot, clone)`, swap `current` to the sub-Entry, and `continue` to reprocess the current FieldPath index.

**Invariant:** A `WriteValue` over a SubState-traversed path requires that the SubState was previously initialized via `ResizeVector` or `SwitchPointer`. The decode protocol guarantees this.

At the leaf, the method SHALL dispatch on the `StateMutation` type:

```java
public boolean applyMutation(FieldPath fpX, StateMutation op) {
    var fp = fpX.s2();
    Entry current = this.rootEntry;
    FieldLayout layout = current.rootLayout;
    int base = 0;
    var last = fp.last();

    int i = 0;
    while (true) {
        var idx = fp.get(i);
        switch (layout) {
            case Composite c -> layout = c.children[idx];
            case Array a     -> { base += a.baseOffset + idx * a.stride; layout = a.element; }
            case SubState s  -> {
                if (i == last) break;  // SubState-as-leaf → structural op below
                int slot = (int) INT_VH.get(current.data, base + s.offset + 1);
                Entry sub = (Entry) refs.get(slot);
                if (!sub.modifiable) {
                    ensureRefsModifiable();
                    sub = sub.copy();
                    refs.set(slot, sub);
                }
                current = sub;
                layout = sub.rootLayout;
                base = 0;
                continue;
            }
            default -> throw new IllegalStateException();
        }
        if (i == last) break;
        i++;
    }

    return switch (op) {
        case StateMutation.WriteValue wv     -> writeValue(current, layout, base, wv.value());
        case StateMutation.ResizeVector rv   -> resizeVector(current, layout, base, rv.count());
        case StateMutation.SwitchPointer sp  -> switchPointer(current, layout, base, sp);
    };
}

private boolean writeValue(Entry target, FieldLayout layout, int base, Object value) {
    switch (layout) {
        case Primitive p -> {
            target.ensureModifiable();
            byte[] data = target.data;
            int flagPos = base + p.offset;
            byte oldFlag = data[flagPos];
            boolean willSet = value != null;
            data[flagPos] = willSet ? (byte) 1 : (byte) 0;
            if (willSet) p.type().write(data, flagPos + 1, value);
            return (oldFlag != 0) ^ willSet;
        }
        case Ref r -> {
            target.ensureModifiable();
            byte[] data = target.data;
            int flagPos = base + r.offset;
            byte oldFlag = data[flagPos];
            if (value == null) {
                if (oldFlag == 0) return false;
                ensureRefsModifiable();
                int slot = (int) INT_VH.get(data, flagPos + 1);
                freeRefSlot(slot);
                data[flagPos] = 0;
                return true;
            }
            ensureRefsModifiable();
            int slot;
            if (oldFlag != 0) {
                slot = (int) INT_VH.get(data, flagPos + 1);
            } else {
                slot = allocateRefSlot();
                INT_VH.set(data, flagPos + 1, slot);
                data[flagPos] = 1;
            }
            refs.set(slot, value);
            return oldFlag == 0;
        }
        default -> throw new IllegalStateException();
    }
}
```

#### Scenario: WriteValue on a Primitive field — capacity change on flag transition

- **WHEN** `applyMutation` receives a `WriteValue(non-null value)` and the leaf layout is a Primitive
- **THEN** the old flag byte is captured, the flag is set to 1, `PrimitiveType.write` writes the value at `offset+1`
- **AND** returns `true` iff the flag transitioned from 0 to 1 (new occupied path)
- **WHEN** `applyMutation` receives a `WriteValue(null)` and the leaf layout is a Primitive
- **THEN** the flag is set to 0; value bytes are not touched
- **AND** returns `true` iff the flag transitioned from 1 to 0

#### Scenario: WriteValue on a Ref field — dynamic slot allocation

- **WHEN** `applyMutation` receives a `WriteValue(non-null value)` and the leaf is a Ref with flag=0
- **THEN** `allocateRefSlot()` returns a new slot (from free-list or by appending to refs)
- **AND** `INT_VH.set(data, offset+1, slot)` writes the slot-index
- **AND** flag is set to 1
- **AND** `refs.set(slot, value)` stores the value
- **AND** returns `true` (capacity changed)
- **WHEN** `applyMutation` receives a `WriteValue(non-null value)` and the leaf is a Ref with flag=1
- **THEN** the existing slot is read from `data[offset+1]` and `refs.set(slot, value)` updates in place
- **AND** returns `false`
- **WHEN** `applyMutation` receives a `WriteValue(null)` and the leaf is a Ref with flag=1
- **THEN** `freeRefSlot(slot)` releases the slot and flag is set to 0
- **AND** returns `true`

#### Scenario: ResizeVector on a SubState(Vector)

- **WHEN** `applyMutation` receives a `ResizeVector(count)` at a SubState(Vector) leaf with no existing sub-Entry (flag=0)
- **AND** count > 0
- **THEN** a sub-Entry with `byte[count * elementBytes]` is created, a slot is allocated in `refs`, the slot-index and flag are stored in the parent's byte[]
- **AND** returns `true`
- **WHEN** a sub-Entry exists and `newCount != oldCount`
- **AND** `sub.modifiable == false` (sub-Entry is shared with another FlatEntityState post-copy)
- **THEN** `ensureRefsModifiable()` clones the refs container, `sub.copy()` creates a fresh Entry, `refs.set(slot, fresh)` replaces the shared Entry — **before** any in-place mutation
- **AND** the original FlatEntityState still sees its unchanged sub-Entry in its own refs container
- **WHEN** a sub-Entry exists and `newCount != oldCount`
- **THEN** a new `byte[newCount * elementBytes]` is allocated, existing data is copied up to `min(oldCount, newCount) * elementBytes`, and `sub.data` is replaced with the new array (no `ensureModifiable()` clone needed — the data is overwritten wholesale)
- **AND** `sub.rootLayout` is replaced with a new `Array` record carrying the new length
- **AND** `sub.modifiable` is set to `true` (the sub-Entry now owns its data)
- **AND** on shrink, dropped tail slot-indices are orphaned in refs (no recursive cleanup — matches `NestedArrayEntityState.clearEntryRef`)
- **AND** returns `true`
- **WHEN** `newCount == oldCount`
- **THEN** no mutation occurs and returns `false`

#### Scenario: SwitchPointer on a SubState(Pointer)

- **WHEN** `applyMutation` receives a `SwitchPointer(newSerializer)` at a SubState(Pointer) leaf
- **THEN** it reads `pointerId` from `SubStateKind.Pointer.pointerId()`
- **AND** if flag was 1 AND (`newSerializer == null` OR `pointerSerializers[pointerId] != newSerializer`): the direct slot is freed via `freeRefSlot` (nested slots are orphaned), flag cleared, `pointerSerializers[pointerId] = null`
- **AND** if `newSerializer != null` AND flag is now 0: a new sub-Entry is created with the pre-computed layout and bytes for `newSerializer`, a slot is allocated, slot-index and flag stored in parent's byte[], `pointerSerializers[pointerId] = newSerializer`
- **AND** returns `true` iff the sub-Entry was created or removed

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

- **WHEN** `applyMutation` is called with FieldPath `[4, 2]` where children[4] is SubState(offset=O)
- **THEN** i=0 advances layout to SubState via Composite
- **AND** i=1 encounters SubState, reads `slot = INT_VH.get(current.data, base+O+1)`, `sub = refs.get(slot)`, COW-clones sub if non-modifiable, swaps `current=sub, layout=sub.rootLayout, base=0`, and `continue`s
- **AND** i=1 is reprocessed with the sub-Entry's Array layout: `base = 0 + 2*stride`, layout = element
- **AND** the WriteValue targets the sub-Entry's byte[]

#### Scenario: SubState as leaf (structural operation)

- **WHEN** `applyMutation` is called with a `ResizeVector` and FieldPath `[4]` where children[4] is SubState
- **THEN** the loop advances layout to SubState and breaks because `i == last`
- **AND** the ResizeVector op is dispatched with the SubState layout

### Requirement: getValueForFieldPath uses same traversal

`getValueForFieldPath` SHALL use the same FieldLayout traversal loop (Composite, Array, SubState with continue). At the leaf:

#### Scenario: Read a primitive value

- **WHEN** the leaf is a Primitive and `data[base + p.offset] != 0` (presence flag set)
- **THEN** return `p.type().read(data, base + p.offset + 1)`
- **WHEN** the presence flag is 0
- **THEN** return `null`

#### Scenario: Read a reference value via dynamic slot lookup

- **WHEN** the leaf is a Ref and `data[base + r.offset] != 0`
- **THEN** read `slot = INT_VH.get(data, base + r.offset + 1)` and return `refs.get(slot)`
- **WHEN** the flag byte is 0
- **THEN** return `null`

### Requirement: FlatEntityState supports two-axis copy-on-write

FlatEntityState SHALL implement COW on two independent lazy axes:
1. **Entry-local**: each Entry has a `modifiable` flag; `ensureModifiable()` clones `byte[] data` on first write.
2. **Global refs**: `FlatEntityState.refsModifiable` flag; `ensureRefsModifiable()` clones the `refs` ArrayList and `freeSlots` Deque on first write that touches refs.

`copy()` SHALL:
1. Call `super(other)` (clones `pointerSerializers[]`)
2. Share the `refs` ArrayList and `freeSlots` Deque with the original (NOT cloned at copy time); set `refsModifiable = false` on both sides
3. Call `rootEntry.copy()`, which marks all sub-Entries as `modifiable = false` recursively and creates a new root Entry sharing the same byte[] until its first write

On first write to a non-modifiable Entry, `ensureModifiable()` SHALL clone `data` and set `modifiable = true`.

On first write that reaches a Ref/SubState leaf or traverses through a SubState requiring clone-and-replace, `ensureRefsModifiable()` SHALL clone the refs container.

Slot indices stored in byte[] remain valid across COW — they index into whichever `refs` container the current FlatEntityState owns.

#### Scenario: Copy and modify independently (primitive writes only)

- **WHEN** a FlatEntityState is copied via `copy()`
- **AND** `applyMutation` writes to a Primitive on the copy
- **THEN** `Entry.ensureModifiable()` clones the root Entry's byte[]
- **AND** `refs` is NOT cloned (refsModifiable remains false)
- **AND** the original's data and refs are unchanged

#### Scenario: Copy and modify refs independently

- **WHEN** a FlatEntityState is copied
- **AND** `applyMutation` writes a new String value to a Ref on the copy
- **THEN** `Entry.ensureModifiable()` clones the byte[]
- **AND** `ensureRefsModifiable()` clones the refs ArrayList and freeSlots Deque
- **AND** the copy allocates a slot in its own refs; the original's refs remains shared with whoever else referenced it
- **AND** the original's Ref values are unchanged

#### Scenario: SubState COW — clone-and-replace at boundary

- **WHEN** a FlatEntityState is copied and the copy traverses through a SubState to write a leaf value
- **THEN** the traversal reads the slot-index from `current.data[base+s.offset+1]`
- **AND** looks up the sub-Entry in `refs.get(slot)` (initially the same instance on both sides)
- **AND** if `sub.modifiable == false`: calls `ensureRefsModifiable()`, clones the sub-Entry via `sub.copy()`, `refs.set(slot, cloned)`
- **AND** the cloned sub-Entry keeps the same slot-index in its byte[] — slot stability across COW
- **AND** writes proceed on the cloned sub-Entry, the original's sub-Entry is unchanged

#### Scenario: SubState COW applies to SubState-as-leaf mutations too

- **WHEN** a FlatEntityState is copied and the copy applies `ResizeVector` to an existing vector sub-Entry
- **AND** `sub.modifiable == false`
- **THEN** the sub-Entry is cloned via `sub.copy()` and re-placed at its slot in the (now-modifiable) refs — **before** any in-place mutation of `data` or `rootLayout`
- **AND** the original's sub-Entry remains unmodified in the original's refs container
- **WHEN** a FlatEntityState is copied and the copy applies `SwitchPointer` to an existing pointer sub-Entry
- **THEN** no in-place mutation of the existing sub-Entry occurs — the old sub-Entry is either freed (direct slot in the copy's refs) or replaced by a newly constructed Entry, so no COW of the shared sub-Entry object is needed

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

### Requirement: FieldLayoutBuilder computes byte-offset layouts from Field hierarchy

The system SHALL provide a `FieldLayoutBuilder` in the `skadistats.clarity.model.state` package. It SHALL walk a Serializer's Field hierarchy once and produce a FieldLayout tree with computed byte offsets and a total byte count. The builder SHALL NOT track a refIndex counter — refs are allocated dynamically at write time.

The builder requires accessor methods on Field subclasses:
- `ArrayField.getElementField()` — the element Field (new getter needed)
- `ArrayField.getLength()` — already exists
- `VectorField.getElementField()` — the element Field (new getter needed)
- `PointerField.getSerializers()` — the possible child Serializers (new getter needed)
- `PointerField.getPointerId()` — already exists

Each recursive call SHALL return `(FieldLayout subtree, int totalBytes)`. Parent Array/Vector uses the element's `totalBytes` as `stride` / `elementBytes`.

#### Scenario: Layout computation algorithm

- **WHEN** `buildLayout(Serializer, startOffset=0)` is called
- **THEN** a running byte offset cursor starts at startOffset
- **AND** for each ValueField with primitive decoder: creates `Primitive(offset, type)`, advances offset by `1 + type.size`
- **AND** for each ValueField with String decoder: creates `Ref(offset)`, advances offset by `1 + 4` (flag + slot-index bytes)
- **AND** for each SerializerField: recursively builds with the SAME running offset (flattening)
- **AND** for each ArrayField(element, length): builds element layout at startOffset=0 with its own sub-cursor, captures `stride = elementTotalBytes`, creates `Array(baseOffset=cursor, stride, length, elementLayout)`, advances cursor by `length * stride`
- **AND** for each VectorField: builds element layout at startOffset=0, captures `elementBytes = elementTotalBytes`, creates `SubState(offset=cursor, Vector(elementBytes, elementLayout))`, advances cursor by `1 + 4`
- **AND** for each PointerField: for each possible child Serializer, recursively builds a layout starting at offset 0, captures `layoutBytes[i] = total`; creates `SubState(offset=cursor, Pointer(pointerId, layouts[], layoutBytes[]))`, advances cursor by `1 + 4`

#### Scenario: Layout caching per Serializer

- **WHEN** `buildLayout` is called for a Serializer that has already been computed
- **THEN** the cached result `(FieldLayout, totalBytes)` is returned
- **AND** caching is safe because Field types and Decoders are immutable after Serializer construction

### Requirement: Runner-configurable EntityStateType

The `S2EntityStateType` enum SHALL include a `FLAT` variant that produces FlatEntityState instances. The default SHALL remain `NESTED_ARRAY`. The existing `withS2EntityState()` method on `AbstractFileRunner` already supports selecting the state type — only the `FLAT` enum variant needs to be added.

#### Scenario: Configure FlatEntityState via Runner

- **WHEN** `new SimpleRunner(source).withS2EntityState(S2EntityStateType.FLAT).runWith(processor)` is called
- **THEN** all S2 entities created during the run use `FlatEntityState`

#### Scenario: Default to NestedArrayEntityState

- **WHEN** no `withS2EntityState()` is called
- **THEN** all S2 entities use `NestedArrayEntityState`

### Requirement: Field accessor methods for FieldLayoutBuilder

The following Field subclasses SHALL provide accessor methods for their internal fields, needed by `FieldLayoutBuilder`:

- `ArrayField.getElementField()` — returns the element Field
- `VectorField.getElementField()` — returns the element Field
- `PointerField.getSerializers()` — returns the `Serializer[]` of possible child serializers

#### Scenario: Accessor availability

- **WHEN** the FieldLayoutBuilder processes an ArrayField
- **THEN** `arrayField.getElementField()` returns the element Field used for recursive layout computation
- **WHEN** the FieldLayoutBuilder processes a VectorField
- **THEN** `vectorField.getElementField()` returns the element Field
- **WHEN** the FieldLayoutBuilder processes a PointerField
- **THEN** `pointerField.getSerializers()` returns the array of child Serializers
- **AND** `pointerField.getPointerId()` returns the globally-unique pointer identifier (existing method)
