## 1. PrimitiveType Enum

- [ ] 1.1 Create `PrimitiveType` enum with variants INT, FLOAT, LONG, BOOL, VECTOR2, VECTOR3
- [ ] 1.2 Implement `write(byte[], int, Object)` and `read(byte[], int) -> Object` for each variant
- [ ] 1.3 Define static VarHandle instances (INT_VH, FLOAT_VH, LONG_VH) via `MethodHandles.byteArrayViewVarHandle`

## 2. FieldLayout + SubStateKind

- [ ] 2.1 Create `FieldLayout` sealed interface with records: `Primitive(int offset, PrimitiveType type)`, `Ref(int refIndex)`, `Composite(FieldLayout[] children)`, `Array(int baseOffset, int stride, FieldLayout element)`, `SubState(int refIndex, SubStateKind kind)`
- [ ] 2.2 Create `SubStateKind` sealed interface with records: `Vector(int elementBytes, int elementRefs, FieldLayout elementLayout)`, `Pointer(FieldLayout[] layouts, int[] layoutBytes, int[] layoutRefs)`

## 3. FieldLayoutBuilder

- [ ] 3.1 Create `FieldLayoutBuilder` that walks a Serializer's Field hierarchy and produces a FieldLayout tree, totalBytes, and totalRefs
- [ ] 3.2 Implement Decoder-to-PrimitiveType mapping for all decoder types
- [ ] 3.3 Implement SerializerField flattening: recurse with same offset cursor
- [ ] 3.4 Implement ArrayField handling: element layout at offset 0, Array node with absolute baseOffset and stride
- [ ] 3.5 Implement VectorField handling: element layout at offset 0, SubState with Vector kind
- [ ] 3.6 Implement PointerField handling: SubState with Pointer kind, pre-computed layouts for all possible child serializers
- [ ] 3.7 Add layout caching per Serializer

## 4. FlatEntityState Implementation

- [ ] 4.1 Create `FlatEntityState implements EntityState` with `byte[] data`, `Object[] refs`, `FieldLayout rootLayout`, `boolean modifiable`
- [ ] 4.2 Implement `applyOperation(FieldPath, StateOp)`: while-loop FieldLayout traversal with Composite/Array/SubState switch, continue-trick for SubState context swap, dispatch on StateOp at leaf
- [ ] 4.3 Implement `getValueForFieldPath`: same traversal, leaf read with presence flag check for Primitives, null check for Refs
- [ ] 4.4 Implement WriteValue handling: Primitive → flag byte + PrimitiveType.write, Ref → refs[] store
- [ ] 4.5 Implement ResizeVector handling: resize sub-FlatEntityState byte[]/refs[] to count * element size
- [ ] 4.6 Implement SwitchPointer handling: replace sub-FlatEntityState with new layout from SubStateKind.Pointer
- [ ] 4.7 Implement `copy()`: data.clone(), refs.clone(), both marked non-modifiable
- [ ] 4.8 Implement `ensureModifiable()`: clone data + refs on first write to non-modifiable state
- [ ] 4.9 Implement `fieldPathIterator()`: walk FieldLayout tree, check presence flags / non-null refs, yield set FieldPaths

## 5. Factory Integration

- [ ] 5.1 Add `FLAT` variant to `S2EntityStateType` enum with factory method that creates FlatEntityState using FieldLayoutBuilder
- [ ] 5.2 Integrate layout building into entity creation path

## 6. Verification

- [ ] 6.1 Run existing examples with FlatEntityState enabled, verify identical output to NestedArrayEntityState
- [ ] 6.2 Benchmark FlatEntityState vs NestedArrayEntityState — measure parse time and memory allocation
