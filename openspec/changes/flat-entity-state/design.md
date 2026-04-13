## Context

The S2 entity state is stored in `NestedArrayEntityState`, a tree of `Entry` objects each holding an `Object[]`. Every field value is a boxed reference (`Integer`, `Float`, etc.). Field access requires O(depth) traversal with virtual calls at each level. Copy-on-write allocates new `Object[]` arrays per `Entry`.

Analysis of a Dota 2 replay (build 6027) shows:
- 96.4% of all serializer fields are fixed primitives (int, float, long, bool, short)
- 3.2% are sub-serializers (fixed structure, flattenable)
- 0.4% are CUtlVector (variable-length arrays)
- A typical Hero has ~189 fixed fields across root + sub-serializers, and 1-2 variable arrays

JDK 17 is the minimum supported version. `VarHandle` on `byte[]` (JDK 9+) provides typed primitive access with JIT optimization.

## Dependency: decouple-field-from-state

This change depends on the `decouple-field-from-state` change which introduces:
- `StateOp` sealed interface (`WriteValue`, `ResizeVector`, `SwitchPointer`) replacing untyped `Object` values in the write chain
- `EntityState.applyOperation(FieldPath, StateOp)` replacing `setValueForFieldPath(FieldPath, Object)`
- Each State implementation owns its own traversal and dispatch logic

FlatEntityState receives typed `StateOp`s and dispatches on them at the leaf of its FieldLayout traversal.

## Critical Constraint: Field Sharing

**Fields are shared between Serializers.** In `FieldGenerator.generateSerializer`:

```java
if (fieldData[fi].field == null) {
    fieldData[fi].field = createField(sid, fieldData[fi]);
}
fields[i] = fieldData[fi].field;  // same Field object reused
```

Two Serializers referencing the same `fieldData` index share the same `Field` object. This means:
- **Offsets CANNOT be stored on Field objects** — different Serializers need different offsets for the same Field
- **Offsets must live in FieldLayout**, a separate tree owned by the Serializer/State, not by the Field

## Goals / Non-Goals

**Goals:**
- Implement `FlatEntityState` as alternative `EntityState` behind the existing interface
- Store primitive fields in a `byte[]` accessed via `VarHandle` — no boxing in internal state
- Hierarchical `FieldLayout` tree that mirrors the Field structure but stores offsets independently
- `PrimitiveType` sealed interface that encapsulates all type-specific read/write logic, with `Scalar` enum for fixed-size types and `VectorType(Scalar, int)` record for multi-element types
- Each `Decoder` provides its own `PrimitiveType` via `getPrimitiveType()` — no separate mapping needed
- Store reference types (Strings) and sub-states in a small `Object[]` sidecar
- Dispatch on `StateOp` at the leaf of FieldLayout traversal
- Run parallel to `NestedArrayEntityState` — switchable via `S2EntityStateType`

**Non-Goals:**
- Eliminating boxing at the decode boundary (Phase 2: `decodeTo` API — separate change)
- Typed read API (`getInt`, `getFloat` — Phase 3: separate change)
- Changes to S1 entity state (`ObjectArrayEntityState`)

## Design

### D1: Storage — `byte[]` with `PrimitiveType` sealed interface

Store all primitive field values in a single `byte[]` per entity. Type-specific access is encapsulated in `PrimitiveType`, a sealed interface with two implementations:

```java
sealed interface PrimitiveType {
    int size();
    void write(byte[] data, int offset, Object value);
    Object read(byte[] data, int offset);

    enum Scalar implements PrimitiveType {
        INT(4)   { /* INT_VH.set/get, unbox/box Integer */ },
        FLOAT(4) { /* FLOAT_VH.set/get, unbox/box Float */ },
        LONG(8)  { /* LONG_VH.set/get, unbox/box Long */ },
        BOOL(1)  { /* direct byte read/write, unbox/box Boolean */ };

        private final int size;
        Scalar(int size) { this.size = size; }
        public int size() { return size; }

        /** Raw write without flag byte — used by VectorType for element access. */
        abstract void writeRaw(byte[] data, int offset, float value);
        abstract float readRaw(byte[] data, int offset);
    }

    record VectorType(Scalar element, int count) implements PrimitiveType {
        public int size() { return count * element.size(); }
        public void write(byte[] data, int offset, Object value) {
            var v = (Vector) value;
            for (int i = 0; i < count; i++)
                element.writeRaw(data, offset + i * element.size(), v.getElement(i));
        }
        public Object read(byte[] data, int offset) {
            var floats = new float[count];
            for (int i = 0; i < count; i++)
                floats[i] = element.readRaw(data, offset + i * element.size());
            return new Vector(floats);
        }
    }
}
```

Static VarHandle instances (on PrimitiveType or Scalar):
```java
static final VarHandle INT_VH   = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
static final VarHandle FLOAT_VH = MethodHandles.byteArrayViewVarHandle(float[].class, ByteOrder.LITTLE_ENDIAN);
static final VarHandle LONG_VH  = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
```

**Key design decisions:**
- **Scalar is an enum** — singletons for the four fixed-size types, efficient identity comparison
- **VectorType is a record parameterized by (Scalar element, int count)** — covers Vector2D (FLOAT, 2), Vector (FLOAT, 3), Vector4D/Quaternion (FLOAT, 4) and any future dimension without new variants
- **VectorType delegates element access to its Scalar** via `writeRaw`/`readRaw` — no hardcoded element size or VarHandle

**Each Decoder provides its own PrimitiveType** via `getPrimitiveType()`. No separate Decoder→PrimitiveType mapping in FieldLayoutBuilder:

```java
public abstract class Decoder {
    /** Returns the PrimitiveType this decoder produces, or null for reference types (String). */
    public PrimitiveType getPrimitiveType() { return null; }
}

// IntVarUnsignedDecoder
public PrimitiveType getPrimitiveType() { return Scalar.INT; }

// VectorDefaultDecoder (has int dim field)
public PrimitiveType getPrimitiveType() { return new VectorType(Scalar.FLOAT, dim); }

// StringZeroTerminatedDecoder — returns null (reference type → FieldLayout.Ref)
```

FlatEntityState knows nothing about int, float, Vector — it delegates to `PrimitiveType.write/read`.

### D2: FieldLayout tree — independent of Field objects

The layout is a tree structure that mirrors the Field hierarchy but is completely independent of it. The layout stores offsets and types; Fields store structure. No cross-references in either direction.

```java
sealed interface FieldLayout {
    record Primitive(int offset, PrimitiveType type) implements FieldLayout {}
    record Ref(int refIndex) implements FieldLayout {}
    record Composite(FieldLayout[] children) implements FieldLayout {}
    record Array(int baseOffset, int stride, int length, FieldLayout element) implements FieldLayout {}
    record SubState(int refIndex, SubStateKind kind) implements FieldLayout {}
}

sealed interface SubStateKind {
    record Vector(int elementBytes, int elementRefs,
                  FieldLayout elementLayout) implements SubStateKind {}
    record Pointer(FieldLayout[] layouts, int[] layoutBytes,
                   int[] layoutRefs) implements SubStateKind {}
}
```

`Array.length` (from `ArrayField.getLength()`) is needed by `fieldPathIterator()` to know how many elements to scan.

**Key properties:**
- `FieldLayout` contains NO references to `Field`, `Serializer`, or `Decoder`
- `Field` and `Serializer` contain NO references to `FieldLayout` or offsets
- Complete separation of concerns: structure vs. storage

**Layout computation:** `FieldLayoutBuilder` walks the Field hierarchy once per Serializer, creates the corresponding FieldLayout tree. The Decoder's `getPrimitiveType()` determines the `PrimitiveType` — no separate mapping needed. After computation, the tree is pure offset/type data.

### D3: Traversal and StateOp dispatch

FlatEntityState traverses **only** the FieldLayout tree. No `field.getChild()` calls.

The traversal uses a `base` accumulator (int, starts at 0) that is only modified by Array nodes. Composite nodes advance the layout cursor but leave `base` unchanged — their children have offsets that are already absolute.

When a SubState is encountered mid-traversal, the loop swaps to the sub-FlatEntityState's context (data, refs, layout, base=0) and uses `continue` to reprocess the current FieldPath index. The traversal tracks the `current` FlatEntityState so that COW (`ensureModifiable`) operates on the correct instance — the root state or a sub-state:

```java
FlatEntityState current = this;
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
            layout = sub.rootLayout; base = 0;
            continue;
        }
        default -> throw new IllegalStateException();
    }
    if (i == last) break;
    i++;
}
```

After the loop, `layout` is the leaf node. The method dispatches on the `StateOp`, passing `current` so that write operations call `ensureModifiable` on the correct FlatEntityState and re-read `data`/`refs` after a potential COW clone:

```java
return switch (op) {
    case WriteValue wv     -> writeValue(current, layout, base, wv.value());
    case ResizeVector rv   -> resizeVector(layout, refs, rv.count());
    case SwitchPointer sp  -> switchPointer(layout, refs, sp.newSerializer());
};
```

- `WriteValue` + `Primitive` → `current.ensureModifiable()`, re-read data, flag byte + `PrimitiveType.write`
- `WriteValue` + `Ref` → `current.ensureModifiable()`, re-read refs, direct `refs[]` store
- `ResizeVector` + `SubState(Vector)` → resize sub-FlatEntityState's byte[]
- `SwitchPointer` + `SubState(Pointer)` → replace sub-FlatEntityState with new layout

### D4: Presence tracking — flag byte per Primitive slot

Each Primitive slot has a 1-byte flag prefix:

```
Slot layout in byte[]:
+--------+--------------+
| flag   | value        |
| 1 byte | type.size    |
+--------+--------------+
```

- On write: `data[base + offset] = 1`, then `type.write(data, base + offset + 1, value)`
- On read: if `data[base + offset] == 0` return null, else `type.read(data, base + offset + 1)`
- Total slot size: `1 + type.size` bytes

### D5: SubState handling

SubState nodes represent variable-length arrays (VectorField) and polymorphic sub-serializers (PointerField). Each SubState in `refs[]` is a sub-FlatEntityState with its own byte[], refs[], and rootLayout.

**VectorField sub-state:**
- rootLayout = `Array(baseOffset=0, stride=elementBytes, element=elementLayout)`
- On `ResizeVector(count)`: resize byte[] to `count * elementBytes`, preserve existing data
- On traversal through: `continue` swaps into sub-state, Array handles element indexing

**PointerField sub-state:**
- rootLayout = Composite for the current child Serializer
- On `SwitchPointer(newSerializer)`: if serializer changed, replace sub-FlatEntityState with new layout
- Pre-computed layouts for all possible child Serializers stored in `SubStateKind.Pointer`

### D6: Copy-on-write — single arraycopy

```java
public EntityState copy() {
    markSubStatesNonModifiable();
    var copy = new FlatEntityState(rootLayout, data.clone(), refs.clone());
    copy.modifiable = false;
    this.modifiable = false;
    return copy;
}

private void markSubStatesNonModifiable() {
    for (var ref : refs) {
        if (ref instanceof FlatEntityState sub) {
            sub.markSubStatesNonModifiable();
            sub.modifiable = false;
        }
    }
}
```

- `data.clone()` — one `System.arraycopy` for all primitives
- `refs.clone()` — shallow copy; sub-FlatEntityStates are shared as frozen snapshots
- `markSubStatesNonModifiable()` — recursively marks all sub-states as non-modifiable before sharing, so they act as immutable snapshots (same pattern as `NestedArrayEntityState` creating new `Entry` objects wrapping shared arrays)
- On first write through a sub-state path, `applyMutation` clones the sub-state object and replaces it in the parent's `refs[]` (see D3 traversal). This gives exclusive ownership before writing.
- `ensureModifiable()` clones data + refs on first write to a non-modifiable state

### D7: Layout caching

Layouts are computed once per Serializer and cached. The cache maps `Serializer -> (FieldLayout, totalBytes, totalRefs)`. This is safe because:
- The layout structure depends only on the Serializer's Field types and Decoders
- These are immutable after Serializer construction

### D8: Offset computation in FieldLayoutBuilder

The builder walks the Field hierarchy with a running offset cursor:

- **SerializerField**: recurse with the SAME offset cursor (flattening). Children's offsets continue from the parent.
- **ArrayField**: build element layout starting at offset 0 (element-relative). The Array node stores the absolute baseOffset. Advance cursor by `length * stride`.
- **VectorField/PointerField**: build element/child layouts starting at offset 0 (they become separate FlatEntityStates). Create SubState with refIndex.
- **ValueField (primitive)**: create Primitive at current offset, advance by `1 + type.size`.
- **ValueField (String)**: create Ref with refIndex, don't advance byte offset.

### D9: Parallel deployment — Runner config

```java
new SimpleRunner(source)
    .withS2EntityState(S2EntityStateType.FLAT)
    .runWith(processor);
```

Default remains `NESTED_ARRAY`. No breaking changes. The existing `withS2EntityState()` method on `AbstractFileRunner` already supports this — only the `FLAT` enum variant and its factory method need to be added.

## File Structure

```
skadistats.clarity.model.state
+-- PrimitiveType.java          // sealed interface: Scalar enum + VectorType record
+-- FieldLayout.java            // sealed interface + records (includes SubStateKind)
+-- FieldLayoutBuilder.java     // Serializer -> FieldLayout tree + sizes
+-- FlatEntityState.java        // byte[] + Object[], traverses FieldLayout, dispatches StateOp
+-- S2EntityStateType.java      // + FLAT variant (existing file)
+-- EntityStateFactory.java     // existing, unchanged

skadistats.clarity.io.decoder
+-- Decoder.java                // + getPrimitiveType() method (default: null)
+-- (each concrete decoder)     // overrides getPrimitiveType() with its Scalar/VectorType
```

## Phase 2 Compatibility

Phase 1 boxes at the interface boundary: `StateMutation.WriteValue(Object)`. Phase 2 eliminates this boxing. The architecture is designed so that Phase 2 does not require changes to PrimitiveType, FieldLayout, or FlatEntityState — only to the decode pipeline.

### Constraint: Two-phase separation

The S2FieldReader cannot write directly into the EntityState. It collects mutations in `FieldChanges`, which are applied later when allowed:

```
Phase: Decode                       Phase: Apply
S2FieldReader                       FieldChanges.applyTo(state)
  decode → StateMutation[]            for each: state.applyMutation(fp, mutation)
  collect in FieldChanges
```

This separation is architectural — the apply timing is controlled by the entity processing pipeline. Phase 2 cannot merge these phases.

### Phase 2 approach: FieldChanges staging buffer

Instead of storing boxed values in `StateMutation.WriteValue(Object)`, FieldChanges gains a `byte[]` staging buffer. Decoders write unboxed primitives into this buffer during decode; FlatEntityState copies from it during apply:

```
Phase 1 (current):
  BitStream → Decoder.decode() → Object (boxed) → WriteValue(Object) → PrimitiveType.write(data, offset)

Phase 2 (future):
  BitStream → Decoder.decodeTo(bs, staging, cursor) → staging byte[] (unboxed)
  ...later...
  FieldChanges.applyTo(state) → copy staging[offset..] → state.data[offset..] (unboxed throughout)
```

Each Decoder already knows its `PrimitiveType` (via `getPrimitiveType()`), which provides the size needed to advance the staging cursor. The `decodeTo` method lives on the Decoder, not on PrimitiveType, because decode logic varies per decoder (varint vs. fixed-bit vs. coord-encoded etc.) even within the same PrimitiveType.

### Backward compatibility for other state types

`EntityState` gains a default method that falls back to the boxed path:

```java
interface EntityState {
    // existing
    boolean applyMutation(FieldPath fp, StateMutation mutation);

    // Phase 2 — default falls back to boxed path
    default boolean applyRaw(FieldPath fp, byte[] staging, int offset, PrimitiveType type) {
        return applyMutation(fp, new StateMutation.WriteValue(type.read(staging, offset)));
    }
}
```

FlatEntityState overrides with a direct byte[]→byte[] copy. NestedArrayEntityState and TreeMapEntityState inherit the default — they still work, just without the Phase 2 speedup.

### What stays on the boxed path

Only `WriteValue` mutations (96.4% of fields) benefit from the staging buffer. Structural operations remain boxed:
- `ResizeVector` — VectorField decodes an int (count) but transforms it into a structural operation
- `SwitchPointer` — PointerField decodes an int (type index) but transforms it into a serializer switch

These are 0.4% of fields. The hot path is unboxed; the rare structural path stays as-is.

## Risks / Trade-offs

**[Phase 1 still boxes at interface boundary]** The `StateMutation.WriteValue` wraps an `Object`. Gains in Phase 1 come from simplified COW (one arraycopy vs. N Entry copies) and reduced traversal overhead (no EntryRef indirection, no virtual dispatch on Field). Unboxed internal storage is the foundation for Phase 2 (staging buffer in FieldChanges).

**[Memory for layout tree]** The FieldLayout tree adds per-Serializer memory overhead. Mitigated by caching — typically dozens of unique Serializers, not thousands.

**[SubState complexity]** VectorField and PointerField require special handling (resize, serializer switch). This is cleanly separated: `StateOp` carries the operation type, `SubStateKind` carries the layout metadata, FlatEntityState dispatches on both.

**[SubState COW]** Sub-FlatEntityStates are shared as frozen snapshots after `copy()`. On first write through a sub-state path, the traversal clones the sub-state object and replaces it in the parent's `refs[]`. This cascades naturally through nested sub-states (each level clones on first write).
