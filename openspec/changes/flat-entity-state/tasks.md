## 1. PrimitiveType Sealed Interface

- [ ] 1.1 Create `PrimitiveType` sealed interface with `size()`, `write(byte[], int, Object)`, `read(byte[], int) -> Object`
- [ ] 1.2 Implement `Scalar` enum with variants INT, FLOAT, LONG, BOOL — each with VarHandle-based write/read and `writeRaw`/`readRaw` for element access
- [ ] 1.3 Implement `VectorType(Scalar element, int count)` record — delegates element access to its Scalar, decomposes/reconstructs Vector
- [ ] 1.4 Define static VarHandle instances (INT_VH, FLOAT_VH, LONG_VH) via `MethodHandles.byteArrayViewVarHandle`

## 2. Decoder.getPrimitiveType()

- [ ] 2.1 Add `getPrimitiveType()` method to `Decoder` (default: null)
- [ ] 2.2 Override in all int decoders → `Scalar.INT`
- [ ] 2.3 Override in all float decoders → `Scalar.FLOAT`
- [ ] 2.4 Override in all long decoders → `Scalar.LONG`
- [ ] 2.5 Override in `BoolDecoder` → `Scalar.BOOL`
- [ ] 2.6 Override in vector/QAngle decoders → `new VectorType(Scalar.FLOAT, dim)`
- [ ] 2.7 String decoders keep default (null → Ref)

## 3. FieldLayout + SubStateKind

- [ ] 3.1 Create `FieldLayout` sealed interface with records: `Primitive(int offset, PrimitiveType type)`, `Ref(int refIndex)`, `Composite(FieldLayout[] children)`, `Array(int baseOffset, int stride, int length, FieldLayout element)`, `SubState(int refIndex, SubStateKind kind)`
- [ ] 3.2 Create `SubStateKind` sealed interface with records: `Vector(int elementBytes, int elementRefs, FieldLayout elementLayout)`, `Pointer(FieldLayout[] layouts, int[] layoutBytes, int[] layoutRefs)`

## 4. FieldLayoutBuilder

- [ ] 4.1 Create `FieldLayoutBuilder` that walks a Serializer's Field hierarchy and produces a FieldLayout tree, totalBytes, and totalRefs
- [ ] 4.2 Use `decoder.getPrimitiveType()` to determine layout type — non-null → Primitive, null → Ref
- [ ] 4.3 Implement SerializerField flattening: recurse with same offset cursor
- [ ] 4.4 Implement ArrayField handling: element layout at offset 0, Array node with absolute baseOffset, stride, and length from ArrayField.getLength()
- [ ] 4.5 Implement VectorField handling: element layout at offset 0, SubState with Vector kind
- [ ] 4.6 Implement PointerField handling: SubState with Pointer kind, pre-computed layouts for all possible child serializers
- [ ] 4.7 Add layout caching per Serializer

## 5. FlatEntityState Implementation

- [ ] 5.1 Create `FlatEntityState implements EntityState` with `byte[] data`, `Object[] refs`, `FieldLayout rootLayout`, `boolean modifiable`
- [ ] 5.2 Implement `applyMutation(FieldPath, StateMutation)`: while-loop FieldLayout traversal with `current` FlatEntityState tracking, cascading SubState COW (ensureModifiable + clone-and-replace), dispatch on StateMutation at leaf
- [ ] 5.3 Implement `getValueForFieldPath`: same traversal (read-only, no COW), leaf read with presence flag check for Primitives, null check for Refs
- [ ] 5.4 Implement WriteValue handling: `current.ensureModifiable()`, re-read data, flag byte + `PrimitiveType.write`; for Ref: re-read refs, direct `refs[]` store
- [ ] 5.5 Implement ResizeVector handling: resize sub-FlatEntityState byte[]/refs[] to count * element size
- [ ] 5.6 Implement SwitchPointer handling: replace sub-FlatEntityState with new layout from SubStateKind.Pointer
- [ ] 5.7 Implement `copy()`: `markSubStatesNonModifiable()` (recursive), `data.clone()`, `refs.clone()`, both marked non-modifiable
- [ ] 5.8 Implement `ensureModifiable()`: clone data + refs on first write to non-modifiable state
- [ ] 5.9 Implement `fieldPathIterator()`: walk FieldLayout tree, check presence flags / non-null refs, use Array.length for element count, delegate to sub-states for nested FieldPaths

## 6. Factory Integration

- [ ] 6.1 Add `FLAT` variant to `S2EntityStateType` enum with factory method that creates FlatEntityState using FieldLayoutBuilder
- [ ] 6.2 Integrate layout building into entity creation path

## 7. Test Infrastructure

### 7.1 Test Helpers

- [ ] 7.1.1 Create `TestFields` helper in `src/test/java/skadistats/clarity/model/state/` — programmatic Serializer/Field hierarchy construction (no replay needed). Provides builders for: flat serializers (N ValueFields), nested serializers (SerializerField), arrays (ArrayField), vectors (VectorField), pointers (PointerField), mixed hierarchies
- [ ] 7.1.2 Helper methods for FieldPath construction and assertion utilities

### 7.2 PrimitiveTypeTest (FlatEntityState-specific)

- [ ] 7.2.1 Scalar roundtrip: write then read for each Scalar (INT, FLOAT, LONG, BOOL) — value identical
- [ ] 7.2.2 VectorType roundtrip: write then read for VectorType(FLOAT, 2/3/4) — components identical
- [ ] 7.2.3 Boundary values: Integer.MAX_VALUE, Float.NaN, Long.MIN_VALUE, negative vector components
- [ ] 7.2.4 Presence semantics: unwritten slot reads as zero bytes (used by FlatEntityState for null detection)

### 7.3 EntityStateTest (×3 implementations via DataProvider)

All tests run against NESTED_ARRAY, TREE_MAP, and FLAT via TestNG `@DataProvider`. Tests verify the EntityState contract, not implementation details.

**Basics:**
- [ ] 7.3.1 Write + read primitive (int, float, long, bool) — value roundtrips correctly
- [ ] 7.3.2 Write + read String (reference type) — value roundtrips correctly
- [ ] 7.3.3 Write + read Vector (2D, 3D, 4D) — components correct
- [ ] 7.3.4 Unwritten field returns null
- [ ] 7.3.5 Multiple fields independently writable and readable
- [ ] 7.3.6 Overwrite existing value — new value returned

**Traversal (nested field paths):**
- [ ] 7.3.7 Sub-serializer access: write/read at [compositeIdx, fieldIdx]
- [ ] 7.3.8 Array element access: write/read at [arrayIdx, elementIdx]
- [ ] 7.3.9 Nested: array inside sub-serializer, sub-serializer inside array

**Copy independence:**
- [ ] 7.3.10 copy() then write on original — copy unchanged
- [ ] 7.3.11 copy() then write on copy — original unchanged
- [ ] 7.3.12 Both sides writable after copy — independent values
- [ ] 7.3.13 Multiple copies from same source — all independent

**SubState operations:**
- [ ] 7.3.14 ResizeVector: grow, write elements, read back
- [ ] 7.3.15 ResizeVector: shrink, existing data preserved up to new size
- [ ] 7.3.16 ResizeVector: resize to 0
- [ ] 7.3.17 SwitchPointer: set serializer, write child fields, read back
- [ ] 7.3.18 SwitchPointer: switch to different serializer, old data cleared
- [ ] 7.3.19 SwitchPointer: set to null

**SubState + copy independence:**
- [ ] 7.3.20 copy() then write into sub-state (vector element) — original sub-state unchanged
- [ ] 7.3.21 copy() then resize vector on copy — original vector unchanged
- [ ] 7.3.22 Both sides write to same sub-state path after copy — independent

**fieldPathIterator:**
- [ ] 7.3.23 Empty state — iterator yields nothing
- [ ] 7.3.24 Some fields set — only set FieldPaths yielded
- [ ] 7.3.25 Array with sparse entries — only written indices yielded
- [ ] 7.3.26 Sub-state fields included in iteration
- [ ] 7.3.27 After ResizeVector shrink — removed paths no longer yielded

## 8. Verification

- [ ] 8.1 Run existing examples with FlatEntityState enabled, verify identical output to NestedArrayEntityState
- [ ] 8.2 Benchmark FlatEntityState vs NestedArrayEntityState — measure parse time and memory allocation
