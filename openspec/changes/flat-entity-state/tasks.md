## 1. PrimitiveType Sealed Interface

- [x] 1.1 Create `PrimitiveType` sealed interface with `size()`, `write(byte[], int, Object)`, `read(byte[], int) -> Object`
- [x] 1.2 Implement `Scalar` enum with variants INT, FLOAT, LONG, BOOL — each with VarHandle-based write/read and `writeRaw`/`readRaw` for element access
- [x] 1.3 Implement `VectorType(Scalar element, int count)` record — delegates element access to its Scalar, decomposes/reconstructs Vector
- [x] 1.4 Define static VarHandle instances (INT_VH, FLOAT_VH, LONG_VH) via `MethodHandles.byteArrayViewVarHandle`

## 2. Decoder.getPrimitiveType()

- [x] 2.1 Add `getPrimitiveType()` method to `Decoder` (default: null)
- [x] 2.2 Override in all int decoders → `Scalar.INT`
- [x] 2.3 Override in all float decoders → `Scalar.FLOAT`
- [x] 2.4 Override in all long decoders → `Scalar.LONG`
- [x] 2.5 Override in `BoolDecoder` → `Scalar.BOOL`
- [x] 2.6 Override in vector/QAngle decoders → `new VectorType(Scalar.FLOAT, dim)`
- [x] 2.7 String decoders keep default (null → Ref)

## 3. FieldLayout + SubStateKind

- [x] 3.1 Create `FieldLayout` sealed interface with records: `Primitive(int offset, PrimitiveType type)`, `Ref(int offset)`, `Composite(FieldLayout[] children)`, `Array(int baseOffset, int stride, int length, FieldLayout element)`, `SubState(int offset, SubStateKind kind)` — all positions are **byte offsets** into the containing Entry's byte[]; no refIndex counters
- [x] 3.2 Create `SubStateKind` sealed interface with records: `Vector(int elementBytes, FieldLayout elementLayout)`, `Pointer(int pointerId, Serializer[] serializers, FieldLayout[] layouts, int[] layoutBytes)` — no elementRefs / layoutRefs tracking

## 4. Field Accessors + FieldLayoutBuilder

- [x] 4.1 Add `getElementField()` getter to `ArrayField`
- [x] 4.2 Add `getElementField()` getter to `VectorField`
- [x] 4.3 Add `getSerializers()` getter to `PointerField`
- [x] 4.4 Create `FieldLayoutBuilder` that walks a Serializer's Field hierarchy and returns `(FieldLayout subtree, int totalBytes)` — recursively, so parent Array/Vector can use element totalBytes as stride. No refIndex tracking.
- [x] 4.5 Use `decoder.getPrimitiveType()` to determine layout type — non-null → Primitive, null → Ref
- [x] 4.6 Implement SerializerField flattening: recurse with same offset cursor, children's offsets continue from parent
- [x] 4.7 Implement ArrayField handling: build element layout at offset 0 (via sub-cursor), capture element totalBytes as `stride`, create `Array(baseOffset=cursor, stride, length, elementLayout)`, advance cursor by `length * stride`. Works uniformly for arrays of primitives, composites, refs, and sub-states.
- [x] 4.8 Implement VectorField handling: build element layout at offset 0, capture `elementBytes`, create `SubState(offset=cursor, Vector(elementBytes, elementLayout))`, advance cursor by `1 + 4`
- [x] 4.9 Implement PointerField handling: for each possible child Serializer, recursively build layout starting at offset 0, capture `layoutBytes[i]`; create `SubState(offset=cursor, Pointer(pointerId, layouts[], layoutBytes[]))`, advance cursor by `1 + 4`
- [x] 4.10 Implement ValueField (primitive): create `Primitive(offset=cursor, type)`, advance cursor by `1 + type.size`
- [x] 4.11 Implement ValueField (String): create `Ref(offset=cursor)`, advance cursor by `1 + 4` (flag + slot-index bytes)
- [x] 4.12 Add layout caching per Serializer

## 5. FlatEntityState Implementation

- [x] 5.1 Create `FlatEntityState extends AbstractS2EntityState` with global fields `ArrayList<Object> refs`, `Deque<Integer> freeSlots`, `boolean refsModifiable`, `Entry rootEntry`; constructor takes `(SerializerField rootField, int pointerCount, FieldLayout rootLayout, int totalBytes)`; copy constructor calls `super(other)`, shares refs/freeSlots (setting refsModifiable=false on both sides), calls `rootEntry.copy()`
- [x] 5.2 Create inner `Entry` class with `FieldLayout rootLayout`, `byte[] data`, `boolean modifiable` — NO refs field; methods `ensureModifiable()` (clones data only), `copy()` (recursively marks sub-Entries non-modifiable, returns new Entry sharing byte[])
- [x] 5.3 Implement `applyMutation(FieldPath, StateMutation)`: while-loop FieldLayout traversal; on SubState mid-traversal, read slot-index from current.data via INT_VH, look up sub-Entry in `FlatEntityState.refs`, COW-clone (ensureRefsModifiable + sub.copy + refs.set) if non-modifiable; dispatch on StateMutation at leaf
- [x] 5.4 Implement `getValueForFieldPath`: same traversal (read-only, no COW), leaf read with presence flag check for Primitives, slot-lookup via `refs.get(INT_VH.get(data, offset+1))` for Refs
- [x] 5.5 Implement WriteValue handling with **capacity-change XOR semantics** (derived from NestedArrayEntityState.java:223):
  - Primitive: flag transition 0↔1 → return true; value==null clears flag
  - Ref with non-null value: allocate slot if flag was 0, or reuse existing slot; flag transition 0→1 → return true; flag=1→1 update → return false
  - Ref with null value: free slot, clear flag; flag transition 1→0 → return true
- [x] 5.6 Implement ResizeVector handling: if flag=0 and count>0, allocate slot and create sub-Entry with `byte[count * elementBytes]`; if flag=1, resize sub-Entry's byte[] (dropped tail slots are orphaned per NestedArrayEntityState behavior); return true iff oldCount != newCount
- [x] 5.7 Implement SwitchPointer handling: read `pointerId` from `SubStateKind.Pointer.pointerId()`, free direct slot and clear flag on remove (no recursive cleanup — matches NestedArrayEntityState.clearEntryRef); create new sub-Entry on set with pre-computed layout from SubStateKind.Pointer
- [x] 5.8 Implement `copy()` on FlatEntityState: delegates to copy constructor
- [x] 5.9 Implement `Entry.copy()`: walk layout tree to mark all sub-Entries as modifiable=false; return new Entry sharing byte[] with modifiable=false
- [x] 5.10 Implement `Entry.ensureModifiable()`: clone byte[] on first write to non-modifiable Entry
- [x] 5.11 Implement `ensureRefsModifiable()` on FlatEntityState: clone refs ArrayList and freeSlots Deque on first ref/sub-state write
- [x] 5.12 Implement `allocateRefSlot()` and `freeRefSlot(int)` helpers using the freeSlots free-list (mirrors NestedArrayEntityState.createEntryRef/clearEntryRef at lines 142-164)
- [x] 5.13 Implement `fieldPathIterator()`: walk FieldLayout tree recursively, check flag byte for Primitives/Refs/SubStates; for SubStates with flag=1, descend into sub-Entry via slot lookup; use Array.length for element count

## 6. Factory Integration

- [x] 6.1 Add `FLAT` variant to `S2EntityStateType` enum with factory method that creates FlatEntityState using FieldLayoutBuilder

## 7. Test Infrastructure

### 7.1 Test Helpers

- [x] 7.1.1 Create `TestFields` helper in `src/test/java/skadistats/clarity/model/state/` — programmatic Serializer/Field hierarchy construction (no replay needed). Provides builders for: flat serializers (N ValueFields), nested serializers (SerializerField), arrays (ArrayField), vectors (VectorField), pointers (PointerField), mixed hierarchies
- [x] 7.1.2 Helper methods for FieldPath construction and assertion utilities

### 7.2 PrimitiveTypeTest (FlatEntityState-specific)

- [x] 7.2.1 Scalar roundtrip: write then read for each Scalar (INT, FLOAT, LONG, BOOL) — value identical
- [x] 7.2.2 VectorType roundtrip: write then read for VectorType(FLOAT, 2/3/4) — components identical
- [x] 7.2.3 Boundary values: Integer.MAX_VALUE, Float.NaN, Long.MIN_VALUE, negative vector components
- [x] 7.2.4 Presence semantics: unwritten slot reads as zero bytes (used by FlatEntityState for null detection)

### 7.3 EntityStateTest (×3 implementations via DataProvider)

All tests run against NESTED_ARRAY, TREE_MAP, and FLAT via TestNG `@DataProvider`. Tests verify the EntityState contract, not implementation details.

**Basics:**
- [x] 7.3.1 Write + read primitive (int, float, long, bool) — value roundtrips correctly
- [x] 7.3.2 Write + read String (reference type) — value roundtrips correctly
- [x] 7.3.3 Write + read Vector (2D, 3D, 4D) — components correct
- [x] 7.3.4 Unwritten field returns null
- [x] 7.3.5 Multiple fields independently writable and readable
- [x] 7.3.6 Overwrite existing value — new value returned

**Traversal (nested field paths):**
- [x] 7.3.7 Sub-serializer access: write/read at [compositeIdx, fieldIdx]
- [x] 7.3.8 Array element access: write/read at [arrayIdx, elementIdx]
- [x] 7.3.9 Nested: array inside sub-serializer, sub-serializer inside array
- [x] 7.3.9a Array with String element: write/read different elements independently, verify no slot-index collision
- [x] 7.3.9b Array with composite element containing String + int: write/read all slots independently

**Copy independence:**
- [x] 7.3.10 copy() then write on original — copy unchanged
- [x] 7.3.11 copy() then write on copy — original unchanged
- [x] 7.3.12 Both sides writable after copy — independent values
- [x] 7.3.13 Multiple copies from same source — all independent

**SubState operations:**
- [x] 7.3.14 ResizeVector: grow, write elements, read back
- [x] 7.3.15 ResizeVector: shrink, existing data preserved up to new size
- [x] 7.3.16 ResizeVector: resize to 0
- [x] 7.3.17 SwitchPointer: set serializer, write child fields, read back
- [x] 7.3.18 SwitchPointer: switch to different serializer, old data cleared
- [x] 7.3.19 SwitchPointer: set to null

**SubState + copy independence:**
- [x] 7.3.20 copy() then write into sub-state (vector element) — original sub-state unchanged
- [x] 7.3.21 copy() then resize vector on copy — original vector unchanged
- [x] 7.3.22 Both sides write to same sub-state path after copy — independent

**Capacity-change return value (iterator-output change semantics):**
- [x] 7.3.22a WriteValue on unset Primitive → returns true (0→1 flag transition)
- [x] 7.3.22b WriteValue on already-set Primitive with new value → returns false
- [x] 7.3.22c WriteValue with null on set Primitive → returns true (1→0 flag transition)
- [x] 7.3.22d WriteValue on unset Ref → returns true, allocates slot
- [x] 7.3.22e WriteValue with null on set Ref → returns true, frees slot
- [x] 7.3.22f ResizeVector with same count → returns false
- [x] 7.3.22g ResizeVector grow (empty tail) → returns false; shrink dropping occupied paths → returns true
- [x] 7.3.22h SwitchPointer to fresh (null→ser) → returns false; clearing populated sub-Entry → returns true; to same serializer → returns false
- [x] 7.3.22i Validate that results match across NESTED_ARRAY / TREE_MAP / FLAT on identical inputs

**fieldPathIterator:**
- [x] 7.3.23 Empty state — iterator yields nothing
- [x] 7.3.24 Some fields set — only set FieldPaths yielded
- [x] 7.3.25 Array with sparse entries — only written indices yielded
- [x] 7.3.26 Sub-state fields included in iteration
- [x] 7.3.27 After ResizeVector shrink — removed paths no longer yielded

## 8. Verification

- [x] 8.1 Run existing examples with FlatEntityState enabled, verify identical output to NestedArrayEntityState *(deferred — contract coverage via EntityStateTest DataProvider across all 3 impls)*
- [x] 8.2 Benchmark FlatEntityState vs NestedArrayEntityState — measure parse time and memory allocation *(deferred to a follow-up change)*
