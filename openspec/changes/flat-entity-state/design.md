## Context

The S2 entity state is stored in `NestedArrayEntityState`, a tree of `Entry` objects each holding an `Object[]`. Every field value is a boxed reference (`Integer`, `Float`, etc.). Field access requires O(depth) traversal with virtual calls at each level. Copy-on-write allocates new `Object[]` arrays per `Entry`.

Analysis of a Dota 2 replay (build 6027) shows:
- 96.4% of all serializer fields are fixed primitives (int, float, long, bool, short)
- 3.2% are sub-serializers (fixed structure, flattenable)
- 0.4% are CUtlVector (variable-length arrays)
- A typical Hero has ~189 fixed fields across root + sub-serializers, and 1-2 variable arrays

JDK 17 is the minimum supported version. `VarHandle` on `byte[]` (JDK 9+) provides typed primitive access with JIT optimization.

## Dependency: decouple-field-from-state (COMPLETE)

This change depended on the `decouple-field-from-state` change which introduced:
- `StateMutation` sealed interface (`WriteValue`, `ResizeVector`, `SwitchPointer`) — in `StateMutation.java`
- `EntityState.applyMutation(FieldPath, StateMutation)` — on the `EntityState` interface
- Each State implementation owns its own traversal and dispatch logic
- `AbstractS2EntityState` base class with `pointerSerializers[]` tracking, field path name resolution

FlatEntityState receives typed `StateMutation`s and dispatches on them at the leaf of its FieldLayout traversal.

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
- Store reference types (Strings) and sub-state Entries (Vector/Pointer) in a small `Object[]` sidecar
- Dispatch on `StateMutation` at the leaf of FieldLayout traversal
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

**Each Decoder provides its own PrimitiveType** via `getPrimitiveType()`. No separate Decoder→PrimitiveType mapping, and no code outside the decoder package branches on concrete decoder classes:

```java
public abstract class Decoder {
    /** Returns the PrimitiveType this decoder produces, or null for reference-typed values. */
    public PrimitiveType getPrimitiveType() { return null; }
}

// Each concrete decoder overrides this method to return its own PrimitiveType
// (Scalar variant or VectorType). Decoders whose value cannot be stored as a
// fixed-size primitive inherit the default null → FieldLayout.Ref.
```

FlatEntityState knows nothing about int, float, Vector — it delegates to `PrimitiveType.write/read`. FieldLayoutBuilder only calls `decoder.getPrimitiveType()`; it never inspects decoder class or identity.

### D2: FieldLayout tree — independent of Field objects

The layout is a tree structure that mirrors the Field hierarchy but is completely independent of it. The layout stores byte offsets and types; Fields store structure. No cross-references in either direction.

```java
sealed interface FieldLayout {
    record Primitive(int offset, PrimitiveType type) implements FieldLayout {}
    record Ref(int offset) implements FieldLayout {}
    record Composite(FieldLayout[] children) implements FieldLayout {}
    record Array(int baseOffset, int stride, int length, FieldLayout element) implements FieldLayout {}
    record SubState(int offset, SubStateKind kind) implements FieldLayout {}
}

sealed interface SubStateKind {
    record Vector(int elementBytes, FieldLayout elementLayout) implements SubStateKind {}
    record Pointer(int pointerId, Serializer[] serializers, FieldLayout[] layouts, int[] layoutBytes) implements SubStateKind {}
}
```

`Array.length` (from `ArrayField.getLength()`) is needed by `fieldPathIterator()` to know how many elements to scan.

**Ref / SubState indirection via byte[]:**

`Ref` and `SubState` store only a **byte offset** in the layout. The slot index in the refs container is **dynamically allocated** at write time and stored in the byte[] at that offset. This gives:

- Arrays with refs/sub-states: each element has its own byte-slot, holds its own dynamic slot-index → no `refBase/refStride` arithmetic in the layout, no per-element refs offset tracking in the builder.
- Strings, Vector sub-Entries, and Pointer sub-Entries all live in the same refs container — uniform handling.
- Slot-index lookup: `int slot = INT_VH.get(data, base + offset + 1); Object value = refs.get(slot);`

**Key properties:**
- `FieldLayout` contains NO references to `Field` or `Decoder`
- `SubStateKind.Pointer` carries a parallel `Serializer[]` reference (same array PointerField holds) for `lookupLayoutIndex` — Serializers are immutable schema carriers, the practical choice over name-based or ID-based lookup
- `Field` and `Serializer` contain NO references to `FieldLayout` or offsets
- Complete separation of concerns: structure vs. storage
- `SubStateKind.Pointer` carries `pointerId` so that `FlatEntityState` can update `pointerSerializers[]` without accessing the Field hierarchy
- `Array` has no `refStride` — refs inside array elements work automatically because slot-indices live in each element's byte-slot

**Layout computation:** `FieldLayoutBuilder` walks the Field hierarchy once per Serializer, creates the corresponding FieldLayout tree. The Decoder's `getPrimitiveType()` determines the `PrimitiveType` — no separate mapping needed. The builder only tracks byte offsets; there is no refIndex counter.

### D3: FlatEntityState structure — global refs, lean Entry

FlatEntityState extends `AbstractS2EntityState` and owns both the refs container (shared across all nesting levels) and a tree of lightweight `Entry` instances that each hold only `byte[] data`. This mirrors the pattern in `NestedArrayEntityState`, which stores all Entry instances in a global `List<Entry> entries` with a `Deque<Integer> freeEntries` free-list (NestedArrayEntityState.java:17-18, 142-164).

```
FlatEntityState extends AbstractS2EntityState
├── rootField, pointerSerializers[]   (inherited, global)
├── ArrayList<Object> refs            (global: Strings AND sub-Entries)
├── Deque<Integer> freeSlots          (global free-list for refs)
├── boolean refsModifiable            (COW flag for the refs container)
├── Entry rootEntry                   (the flat primitive storage)
│
│   class Entry {
│       FieldLayout rootLayout
│       byte[] data
│       boolean modifiable
│   }
│
│   refs contains:
│   ├── String values (from ValueField with String decoder)
│   └── Entry instances (from VectorField and PointerField sub-states)
│       — stored at dynamically-allocated slot indices
```

**Entry is a pure byte[] container** — no refs of its own, no rootField, no pointerSerializers. Sub-states for VectorField and PointerField are Entry instances living in `FlatEntityState.refs` at dynamically-allocated slots. Each Entry's `byte[]` contains slot-indices pointing into `FlatEntityState.refs` at the appropriate offsets.

**Why global refs:**
- Parallel to NestedArrayEntityState's global `entries` + `freeEntries`
- Single free-list gives better slot recycling across the whole entity
- COW is flat: one refs.clone() per entity copy, then shared by all Entries
- Sub-Entry COW is cheap: only byte[] cloning, no per-Entry refs/free-list
- Slot indices are stable across copy() (same positions hold the same logical values after shallow clone)

**Slot-index stability across COW:**
After `copy()`, original and copy share no state but point to the same sub-Entries via the same slot indices. When either side writes, it lazily clones its refs container and/or the sub-Entry. The slot index stored in the sub-Entry's byte[] remains valid — it points into whichever refs container the current FlatEntityState owns. See D7 for details.

Constructor:
```java
FlatEntityState(SerializerField rootField, int pointerCount,
                FieldLayout rootLayout, int totalBytes) {
    super(rootField, pointerCount);
    this.refs = new ArrayList<>();
    this.freeSlots = new ArrayDeque<>();
    this.refsModifiable = true;
    this.rootEntry = new Entry(rootLayout, new byte[totalBytes]);
}
```

Copy constructor:
```java
private FlatEntityState(FlatEntityState other) {
    super(other);  // clones pointerSerializers
    this.refs = other.refs;                 // shared (shallow) until first ref write
    this.freeSlots = other.freeSlots;       // shared until refs clone
    this.refsModifiable = false;
    other.refsModifiable = false;
    this.rootEntry = other.rootEntry.copy(); // recursively marks sub-Entries non-modifiable
}
```

The refs container is NOT cloned at copy() time — it stays shared with the original until the first write reaches a Ref or SubState. See D7.

### D4: Traversal and StateMutation dispatch

FlatEntityState traverses **only** the FieldLayout tree. No `field.getChild()` calls.

The traversal uses a `base` accumulator (int, starts at 0) that is only modified by Array nodes. Composite nodes advance the layout cursor but leave `base` unchanged — their children have offsets that are already absolute within the current Entry's byte[].

When a SubState is encountered mid-traversal, the loop reads the slot-index from `current.data[base + s.offset + 1]` via `INT_VH.get`, looks up the sub-Entry in `this.refs`, and swaps to the sub-Entry's context (data, layout, base=0). The traversal tracks the `current` Entry so that COW (`ensureModifiable`) operates on the correct instance. Descent through a SubState consumes **no** fp index — the next iteration re-uses the same `i` against the sub-Entry's layout.

**SubState-as-leaf vs. SubState-as-transition.** The two cases reach the dispatch differently:
- *Leaf* (SwitchPointer, ResizeVector, or anything targeting the SubState position itself): the path's last idx points at the SubState's slot in its parent. The Composite/Array branch advances `layout = SubState` on that idx and the loop's terminal `if (i == last) break` fires. The SubState branch is never entered. Dispatch sees `layout = SubState`.
- *Transition* (any mutation targeting a leaf inside the sub-Entry): the SubState is reached as the current `layout` at the start of an iteration. The SubState branch descends without consuming an idx.

Therefore the SubState branch must NOT have its own `if (i == last) break` — that would prevent the final descent for transitional WriteValue/Read operations whose leaf lives in the sub-Entry.

**Lazy creation of sub-Entries.** `NestedArrayEntityState` stores untyped `Object[]` and creates intermediate sub-Entries lazily via `subEntry(idx)`. The decode protocol relies on this for two cases:
- **Pointer with single (default) serializer**: protocol may write into the sub-Entry without emitting `SwitchPointer`, since `PointerField.resolveSerializer` falls back to `defaultSerializer`. FLAT must lazy-create the sub-Entry using `layouts[0]` and set `pointerSerializers[pointerId] = serializers[0]`. For multi-polymorphic Pointers the protocol is required to emit `SwitchPointer` first; lazy-create throws if `serializers.length > 1`.
- **Vector**: protocol may write elements directly without emitting `ResizeVector`. FLAT must lazy-create the vector sub-Entry sized to fit `nextIdx + 1` elements, and grow on subsequent descents whose nextIdx exceeds current `Array.length`.

Both lazy creations happen in the SubState branch when the flag byte is 0, before reading the slot index.

**Vector growth on traversal.** Even after a vector sub-Entry exists, a subsequent transition with `nextIdx >= currentLength` must extend the byte[] and update the `Array.length`. This mirrors `NestedArrayEntityState.ensureNodeCapacity`'s `idx + 1` fallback for Vector/Pointer parent fields.

**Invariant:** `Primitive` and `Ref` only appear at leaves. `Composite`, `Array`, `SubState` only appear at branches (except SubState-as-leaf for structural ops).

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
            default -> throw new IllegalStateException("non-branch layout at non-leaf position");
        }
        if (i == last) break;
        i++;
    }

    // leaf: dispatch on StateMutation
    return switch (op) {
        case StateMutation.WriteValue wv     -> writeValue(current, layout, base, wv.value());
        case StateMutation.ResizeVector rv   -> resizeVector(current, layout, base, rv.count());
        case StateMutation.SwitchPointer sp  -> switchPointer(current, layout, base, sp);
    };
}
```

- `WriteValue` + `Primitive` → `current.ensureModifiable()`, flag-XOR capacity check, `PrimitiveType.write`
- `WriteValue` + `Ref` → dynamic slot allocation if flag was 0; flag-XOR capacity check; `refs.set(slot, value)`
- `ResizeVector` + `SubState(Vector)` → resize sub-Entry's byte[]; capacity changes if count differs
- `SwitchPointer` + `SubState(Pointer)` → allocate/free sub-Entry slot, update `pointerSerializers[pointerId]`

#### WriteValue

```java
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
            return (oldFlag != 0) ^ willSet;  // capacity changed on 0↔1 transition
        }
        case Ref r -> {
            target.ensureModifiable();
            byte[] data = target.data;
            int flagPos = base + r.offset;
            byte oldFlag = data[flagPos];
            if (value == null) {
                if (oldFlag != 0) {
                    int oldSlot = (int) INT_VH.get(data, flagPos + 1);
                    ensureRefsModifiable();
                    freeRefSlot(oldSlot);
                    data[flagPos] = 0;
                    return true;
                }
                return false;
            } else {
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
                return oldFlag == 0;  // capacity changed only on null→value
            }
        }
        default -> throw new IllegalStateException();
    }
}
```

#### SwitchPointer

```java
private boolean switchPointer(Entry current, FieldLayout layout, int base,
                              StateMutation.SwitchPointer sp) {
    if (!(layout instanceof SubState s) || !(s.kind() instanceof SubStateKind.Pointer p))
        throw new IllegalStateException();

    var newSerializer = sp.newSerializer();
    var currentSerializer = pointerSerializers[p.pointerId()];
    byte[] data = current.data;
    int flagPos = base + s.offset;
    boolean hadSub = data[flagPos] != 0;
    boolean changed = false;

    if (hadSub && (newSerializer == null || currentSerializer != newSerializer)) {
        current.ensureModifiable();
        int oldSlot = (int) INT_VH.get(data, flagPos + 1);
        ensureRefsModifiable();
        freeRefSlot(oldSlot);                // only the direct slot — matches NestedArrayEntityState
        current.data[flagPos] = 0;
        pointerSerializers[p.pointerId()] = null;
        hadSub = false;
        changed = true;
    }
    if (newSerializer != null && !hadSub) {
        current.ensureModifiable();
        int layoutIdx = lookupLayoutIndex(p, newSerializer);
        var sub = new Entry(p.layouts()[layoutIdx], new byte[p.layoutBytes()[layoutIdx]]);
        ensureRefsModifiable();
        int slot = allocateRefSlot();
        refs.set(slot, sub);
        INT_VH.set(current.data, flagPos + 1, slot);
        current.data[flagPos] = 1;
        pointerSerializers[p.pointerId()] = newSerializer;
        changed = true;
    }
    return changed;
}
```

#### ResizeVector

```java
private boolean resizeVector(Entry current, FieldLayout layout, int base, int newCount) {
    if (!(layout instanceof SubState s) || !(s.kind() instanceof SubStateKind.Vector v))
        throw new IllegalStateException();

    byte[] data = current.data;
    int flagPos = base + s.offset;
    Entry sub;
    if (data[flagPos] == 0) {
        if (newCount == 0) return false;
        current.ensureModifiable();
        ensureRefsModifiable();
        var elementArray = new FieldLayout.Array(0, v.elementBytes(), newCount, v.elementLayout());
        sub = new Entry(elementArray, new byte[newCount * v.elementBytes()]);
        int slot = allocateRefSlot();
        refs.set(slot, sub);
        INT_VH.set(current.data, flagPos + 1, slot);
        current.data[flagPos] = 1;
        return true;
    }
    int slot = (int) INT_VH.get(data, flagPos + 1);
    sub = (Entry) refs.get(slot);
    var oldArray = (FieldLayout.Array) sub.rootLayout;
    if (oldArray.length() == newCount) return false;
    // COW: if sub is shared with another FlatEntityState (post-copy), replace with a
    // fresh Entry in our refs container before any in-place mutation. Mirrors the
    // mid-traversal SubState COW in applyMutation. No ensureModifiable() call needed
    // — we overwrite sub.data wholesale below, so cloning the old data first would be
    // wasted work.
    if (!sub.modifiable) {
        ensureRefsModifiable();
        sub = sub.copy();
        refs.set(slot, sub);
    }
    // On shrink, dropped tail slots are orphaned in refs — matches NestedArrayEntityState.
    byte[] newData = new byte[newCount * v.elementBytes()];
    System.arraycopy(sub.data, 0, newData, 0, Math.min(sub.data.length, newData.length));
    sub.data = newData;
    sub.rootLayout = new FieldLayout.Array(0, v.elementBytes(), newCount, v.elementLayout());
    sub.modifiable = true;
    return true;
}
```

#### Ref-slot management helpers

```java
private int allocateRefSlot() {
    if (!freeSlots.isEmpty()) return freeSlots.removeFirst();
    refs.add(null);
    return refs.size() - 1;
}

private void freeRefSlot(int slot) {
    refs.set(slot, null);
    freeSlots.addLast(slot);
}

private void ensureRefsModifiable() {
    if (!refsModifiable) {
        refs = new ArrayList<>(refs);          // shallow clone
        freeSlots = new ArrayDeque<>(freeSlots);
        refsModifiable = true;
    }
}

```

**No recursive cleanup.** When a sub-Entry is released (SwitchPointer to null/other serializer, Vector shrink), only its **direct** slot in `refs` is freed. Any nested ref-slots held by the released sub-Entry (its Strings, its own sub-Entries) become **orphaned** in `refs` — they remain allocated but unreachable. They are reclaimed by JVM GC when the entire FlatEntityState instance is discarded.

This matches `NestedArrayEntityState.clearEntryRef` (lines 154-157), which also clears only the direct slot without walking the cleared Entry's state[] for nested EntryRefs. Accepted because:
- SwitchPointer and Vector shrink are rare operations in S2 replays
- Entities have bounded lifetimes (projectiles seconds, heroes match-long)
- Slot recycling via direct ref writes keeps `refs.size()` stable in practice
- Simpler code, no new class of bugs

### D5: Presence tracking — flag byte per slot

Every storage slot in byte[] has a 1-byte flag prefix. Three slot schemas:

**Primitive slot:** `1 + type.size` bytes
```
+--------+--------------+
| flag   | value bytes  |
| 1 byte | type.size    |
+--------+--------------+
```
- Write: `data[base + p.offset] = 1; p.type().write(data, base + p.offset + 1, value)`
- Read: `data[base + p.offset] == 0 ? null : p.type().read(data, base + p.offset + 1)`

**Ref slot:** `1 + 4` bytes
```
+--------+----------------------+
| flag   | slot index (int32)   |
| 1 byte | 4 bytes LE           |
+--------+----------------------+
```
- The slot-index is dynamically allocated from `FlatEntityState.refs` at first write
- Write: allocate slot if flag was 0, store index at `offset + 1`, set flag to 1, `refs.set(slot, value)`
- Read: `flag == 0 ? null : refs.get(INT_VH.get(data, offset + 1))`

**SubState slot:** `1 + 4` bytes (same as Ref)
```
+--------+----------------------+
| flag   | slot index (int32)   |
+--------+----------------------+
```
- Slot in `refs` holds an `Entry` instance (sub-Entry) instead of a String
- Created on first `ResizeVector(count>0)` or `SwitchPointer(newSerializer != null)`
- Freed on `ResizeVector(0)`, `SwitchPointer(null)`, or when a Pointer switches serializer

**Capacity-change semantics — "the set of occupied FieldPaths changed":**
- Primitive: flag transition 0→1 or 1→0 → `true`
- Ref: flag transition 0→1 or 1→0 → `true`
- SubState(Vector) ResizeVector: `true` iff any occupied path existed in the shrunk tail (grow alone never adds paths)
- SubState(Pointer) SwitchPointer: `true` iff the cleared old sub-Entry contained any occupied paths (creating an empty sub-Entry alone never adds paths)

This is the signal for `fieldPathIterator` invalidation — we return true precisely when the iterator output would change. Creating empty capacity (grown vector slot, fresh pointer sub-Entry) adds no occupied paths and therefore returns false.

### D6: SubState handling

SubState nodes represent variable-length arrays (VectorField) and polymorphic sub-serializers (PointerField). Each SubState slot in `FlatEntityState.refs` holds an `Entry` instance (sub-Entry) with its own `byte[]` and `rootLayout`. Sub-Entries have no refs of their own — all refs go through `FlatEntityState.refs`.

**VectorField sub-state:**
- rootLayout = `Array(baseOffset=0, stride=elementBytes, length=count, element=elementLayout)` where `count` tracks the current vector length
- On `ResizeVector(count)` with no existing sub-Entry: allocate slot in refs, create Entry with `byte[count * elementBytes]`
- On `ResizeVector(count)` with existing sub-Entry: resize byte[] and update Array.length. Dropped tail slots orphan their ref-indices (no recursive cleanup)
- On traversal through: read slot from byte[], look up sub-Entry in refs, Array handles element indexing within sub-Entry's byte[]

**PointerField sub-state:**
- Sub-Entry's rootLayout = Composite for the current child Serializer (from `p.layouts[]`)
- On `SwitchPointer(newSerializer)`:
  1. Look up `pointerId` from `SubStateKind.Pointer.pointerId()`
  2. If flag was 1 AND (`newSerializer == null` OR `pointerSerializers[pointerId] != newSerializer`): free the direct slot in refs, clear flag, clear `pointerSerializers[pointerId]`. Nested ref-slots held by the discarded sub-Entry are orphaned (no recursive cleanup)
  3. If `newSerializer != null` AND flag is now 0: set `pointerSerializers[pointerId] = newSerializer`, create new sub-Entry with `p.layouts[layoutIdx]`, allocate slot, store slot-index, set flag
- Pre-computed layouts for all possible child Serializers stored in `SubStateKind.Pointer.layouts[]`

Mirrors `NestedArrayEntityState.handlePointerSwitch` (lines 96-112) and `clearEntryRef` (lines 154-157) — both skip recursive cleanup of nested sub-entries.

### D7: Copy-on-write

COW has two independent axes:

1. **Entry-local COW** — each Entry has `modifiable` flag; `ensureModifiable()` clones `data` on first write.
2. **Global refs COW** — `FlatEntityState.refsModifiable` flag; `ensureRefsModifiable()` clones the `refs` ArrayList and `freeSlots` Deque on first write that touches refs.

Both axes are lazy — copy() does not eagerly clone anything except pointerSerializers.

**Sub-Entry COW rule:** Whenever a mutation would modify a non-modifiable sub-Entry (traversal-through for nested write OR SubState-as-leaf for ResizeVector on an existing vector), the sub-Entry MUST be replaced in refs with a fresh Entry via `sub.copy()` + `refs.set(slot, copy)` **before** any in-place mutation on the sub-Entry. `Entry.ensureModifiable()` alone is insufficient because the sub-Entry object is shared with the original's refs container — mutating `data` or `rootLayout` in place corrupts the original. `ensureRefsModifiable()` must run first so the `refs.set` does not mutate the shared container. SwitchPointer is exempt because it never mutates existing sub-Entries in place (it either frees the slot or allocates a fresh Entry).

```java
@Override
public EntityState copy() {
    return new FlatEntityState(this);
}

private FlatEntityState(FlatEntityState other) {
    super(other);                            // clones pointerSerializers[]
    this.refs = other.refs;                  // shared until first ref write
    this.freeSlots = other.freeSlots;
    this.refsModifiable = false;
    other.refsModifiable = false;            // both must clone-on-write now
    this.rootEntry = other.rootEntry.copy(); // recursively marks sub-Entries non-modifiable
}
```

Entry.copy():
```java
Entry copy() {
    markNonModifiableRecursive();
    return new Entry(rootLayout, data, false);  // shares data until first write
}

void markNonModifiableRecursive() {
    if (!modifiable) return;
    modifiable = false;
    // Sub-Entries are reachable only through FlatEntityState.refs; we walk
    // the layout tree to find SubState slots and mark their sub-Entries too.
    markSubEntriesNonModifiable(rootLayout, 0);
}
```

Note: marking sub-Entries as non-modifiable requires walking the layout and reading slot indices from byte[]. Since both the original's and copy's sub-Entries are the same objects (refs is shared), marking on either side applies to both — consistent semantics.

Entry.ensureModifiable():
```java
void ensureModifiable() {
    if (!modifiable) {
        data = data.clone();
        modifiable = true;
    }
}
```

**Slot-index stability across COW:**

After copy(), slot indices in any Entry's byte[] are valid in EITHER refs container. On the first write:
- If the write hits a primitive → only `Entry.ensureModifiable()` triggers; data is cloned
- If the write hits a Ref/SubState → both `Entry.ensureModifiable()` AND `ensureRefsModifiable()` trigger; data and refs container both cloned
- Subsequent writes through a sub-Entry path → traverse to sub-Entry, `sub.copy()` clones the sub-Entry's data, and `refs.set(slot, clonedSub)` updates the copy's refs container with the new sub-Entry at the same slot index

Because slot-indices are stored IN the data[] (not in the layout), they never need renumbering. The original and the copy use different `refs` containers after COW, but both map the same slot index to the same logical entity.

### D8: Layout caching

Layouts are computed once per Serializer and cached. The cache maps `Serializer -> (FieldLayout, totalBytes)`. totalRefs is no longer tracked — refs are dynamically allocated.

Safe because:
- The layout structure depends only on the Serializer's Field types and Decoders
- These are immutable after Serializer construction

### D9: Offset computation in FieldLayoutBuilder

The builder walks the Field hierarchy with a running **byte** offset cursor. There is NO refIndex counter — refs are allocated at runtime. It requires accessor methods on the Field subclasses:
- `ArrayField.getElementField()` — element Field for recursive layout (new getter)
- `ArrayField.getLength()` — already exists
- `VectorField.getElementField()` — element Field for sub-state layout (new getter)
- `PointerField.getSerializers()` — possible child Serializers (new getter)
- `PointerField.getPointerId()` — already exists

Each recursive call returns `(FieldLayout subtree, int totalBytes)`. The parent uses `totalBytes` as stride when building Array nodes over composite elements.

Offset computation rules:

- **SerializerField**: recurse with the SAME offset cursor (flattening). Children's offsets continue from the parent. Returns `Composite(children[])` and total bytes consumed.
- **ArrayField**: build element layout at offset 0 (element-relative). Capture the element's totalBytes as `stride`. Create `Array(baseOffset=currentOffset, stride, length, elementLayout)`. Advance cursor by `length * stride`.
- **VectorField**: build element layout at offset 0 with its own sub-cursor. Capture `elementBytes = elementLayout totalBytes`. Create `SubState(offset=currentOffset, Vector(elementBytes, elementLayout))`. Advance cursor by `1 + 4` (flag + slot).
- **PointerField**: for each possible child Serializer, recursively build a layout (starting at offset 0 within that sub-Entry). Capture `layoutBytes[i]`. Create `SubState(offset=currentOffset, Pointer(pointerId, layouts[], layoutBytes[]))`. Advance cursor by `1 + 4`.
- **ValueField (primitive)**: `type = decoder.getPrimitiveType()`. Create `Primitive(offset=currentOffset, type)`. Advance cursor by `1 + type.size`.
- **ValueField (String)**: Create `Ref(offset=currentOffset)`. Advance cursor by `1 + 4` (flag + slot).

**Arrays with refs/sub-states work automatically:** the element layout has its own byte-offset layout, and at runtime each array element has its own slot indices in its element-sized byte region. No refStride needed.

### D10: Parallel deployment — Runner config

```java
new SimpleRunner(source)
    .withS2EntityState(S2EntityStateType.FLAT)
    .runWith(processor);
```

Default remains `NESTED_ARRAY`. No breaking changes. The existing `withS2EntityState()` method on `AbstractFileRunner` already supports this — only the `FLAT` enum variant and its factory method in `S2EntityStateType` need to be added.

### D11: fieldPathIterator

Iterates all FieldPaths whose values are set. Walks the layout tree for each Entry and yields a FieldPath for every Primitive/Ref/SubState with flag=1. For SubStates, descends into the sub-Entry to yield nested paths.

```java
public Iterator<FieldPath> fieldPathIterator() {
    var out = new ArrayList<FieldPath>();
    walk(rootEntry, rootEntry.rootLayout, 0, new int[FieldPath.MAX_DEPTH], 0, out);
    return out.iterator();
}

private void walk(Entry entry, FieldLayout layout, int base,
                  int[] path, int depth, List<FieldPath> out) {
    switch (layout) {
        case FieldLayout.Composite c -> {
            for (int i = 0; i < c.children().length; i++) {
                path[depth] = i;
                walk(entry, c.children()[i], base, path, depth + 1, out);
            }
        }
        case FieldLayout.Array a -> {
            for (int i = 0; i < a.length(); i++) {
                path[depth] = i;
                walk(entry, a.element(), base + a.baseOffset() + i * a.stride(),
                     path, depth + 1, out);
            }
        }
        case FieldLayout.Primitive p -> {
            if (entry.data[base + p.offset()] != 0) {
                out.add(FieldPath.of(path, depth));
            }
        }
        case FieldLayout.Ref r -> {
            if (entry.data[base + r.offset()] != 0) {
                out.add(FieldPath.of(path, depth));
            }
        }
        case FieldLayout.SubState s -> {
            if (entry.data[base + s.offset()] != 0) {
                // Yield the SubState path itself? In NestedArrayEntityState a VectorField
                // is not a value-bearing path unless elements are set. We descend:
                int slot = (int) INT_VH.get(entry.data, base + s.offset() + 1);
                Entry sub = (Entry) refs.get(slot);
                walk(sub, sub.rootLayout, 0, path, depth, out);
            }
        }
    }
}
```

The exact contract for SubState-as-own-path must match NestedArrayEntityState's iterator semantics — verified by DataProvider tests in 7.3.23-7.3.27.

### D12: Capacity change return value

`applyMutation` returns `true` iff **the set of occupied FieldPaths changed** — i.e., the `fieldPathIterator` output would differ before vs. after the mutation. Empty-capacity additions (grow, fresh pointer sub-Entry) do not count because they add no occupied paths.

| Operation | Returns true when |
|---|---|
| WriteValue on Primitive | flag-byte transition 0→1 or 1→0 |
| WriteValue on Ref | flag-byte transition 0→1 or 1→0 |
| ResizeVector | any occupied path existed in `[newCount, oldCount)` (shrink dropped data); grow alone → false |
| SwitchPointer | the cleared old sub-Entry contained any occupied paths; fresh creation alone → false |

This signals that `fieldPathIterator` results are invalidated.

## File Structure

```
skadistats.clarity.model.state
+-- PrimitiveType.java          // sealed interface: Scalar enum + VectorType record
+-- FieldLayout.java            // sealed interface + records (includes SubStateKind)
+-- FieldLayoutBuilder.java     // Serializer -> FieldLayout tree + sizes
+-- FlatEntityState.java        // extends AbstractS2EntityState, inner Entry class
+-- S2EntityStateType.java      // + FLAT variant (existing file)
+-- EntityStateFactory.java     // existing, unchanged

skadistats.clarity.io.decoder
+-- Decoder.java                // + getPrimitiveType() method (default: null)
+-- (each concrete decoder)     // overrides getPrimitiveType() with its Scalar/VectorType

skadistats.clarity.io.s2.field
+-- ArrayField.java             // + getElementField() getter
+-- VectorField.java            // + getElementField() getter
+-- PointerField.java           // + getSerializers() getter
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

**[Phase 1 still boxes at interface boundary]** The `StateMutation.WriteValue` wraps an `Object`, and `PrimitiveType.write(Object)` unboxes internally. Gains in Phase 1 come from simplified COW (one arraycopy for data + one ArrayList clone for refs vs. N Entry copies in nested design), reduced traversal overhead (no virtual dispatch on Field), and byte-packed primitive storage (better cache locality). Unboxed internal storage is the foundation for Phase 2 (staging buffer in FieldChanges).

**[Ref indirection cost]** Ref reads require an extra memory load (byte[] → int → refs.get(int)) vs. direct Object[] access in NestedArrayEntityState. Mitigated by refs being rare (Strings ~0.x% of fields in Dota 2).

**[Orphan slots on structural ops]** `SwitchPointer(null)`, pointer serializer change, and `ResizeVector(shrink)` free only the direct slot in `refs`, not nested ref-slots held by the discarded sub-Entry. Matches `NestedArrayEntityState.clearEntryRef` (lines 154-157). Orphaned slots accumulate in `refs` until the whole FlatEntityState is GC'd. Accepted because structural ops are rare in S2 replays and entity lifetimes are bounded.

**[Memory for layout tree]** The FieldLayout tree adds per-Serializer memory overhead. Mitigated by caching — typically dozens of unique Serializers, not thousands.

**[SubState complexity]** VectorField and PointerField require special handling (resize, serializer switch). This is cleanly separated: `StateMutation` carries the operation type, `SubStateKind` carries the layout metadata, FlatEntityState dispatches on both.

**[Two-axis COW]** Entry COW (byte[]) and refs COW (ArrayList) are independent lazy clones. Primitive-only writes never touch refs. Ref writes trigger both. Cleanly separated — no cascading object graph clone.

**[Arrays with refs/sub-states]** Automatically handled via byte-offset indirection: each array element has its own byte-slot holding its own slot-index. No refStride arithmetic needed in the layout.
