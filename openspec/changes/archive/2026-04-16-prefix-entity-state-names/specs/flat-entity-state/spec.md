## RENAMED Requirements

- FROM: `### Requirement: FlatEntityState extends AbstractS2EntityState with global refs and lean Entry`
- TO: `### Requirement: S2FlatEntityState extends S2AbstractEntityState with global refs and lean Entry`

- FROM: `### Requirement: FlatEntityState dispatches on StateMutation via applyMutation`
- TO: `### Requirement: S2FlatEntityState dispatches on StateMutation via applyMutation`

- FROM: `### Requirement: FlatEntityState.copy() is an eager deep copy`
- TO: `### Requirement: S2FlatEntityState.copy() is an eager deep copy`

- FROM: `### Requirement: FlatEntityState provides fieldPathIterator`
- TO: `### Requirement: S2FlatEntityState provides fieldPathIterator`

- FROM: `### Requirement: FlatEntityState provides decodeInto for primitive decode-direct path`
- TO: `### Requirement: S2FlatEntityState provides decodeInto for primitive decode-direct path`

- FROM: `### Requirement: FlatEntityState provides a unified direct-write method`
- TO: `### Requirement: S2FlatEntityState provides a unified direct-write method`

## MODIFIED Requirements

### Requirement: PrimitiveType sealed interface encapsulates typed byte[] access

The system SHALL provide a `PrimitiveType` sealed interface in the `skadistats.clarity.model.state` package with two implementations: `Scalar` (enum) for fixed-size scalar types, and `VectorType` (record) for multi-element types parameterized by element scalar and count. `S2FlatEntityState` SHALL delegate all type-specific read/write operations to PrimitiveType — it SHALL NOT contain type-specific logic itself.

The static VarHandle instances SHALL be created via `MethodHandles.byteArrayViewVarHandle` with `ByteOrder.LITTLE_ENDIAN`.

#### Scenario: Scalar variants and their mappings

- **WHEN** a Scalar variant is used to read or write a value
- **THEN** it delegates to the corresponding VarHandle or direct byte access
- **AND** each scalar variant is represented as the boxed primitive type (Integer/Float/Long/Boolean)

#### Scenario: VectorType delegates element access to its Scalar

- **WHEN** `VectorType(FLOAT, 3).write(data, offset, value)` is called
- **THEN** it decomposes the `Vector` into 3 floats and writes each via `FLOAT.writeRaw(data, offset + i * 4, v.getElement(i))`
- **WHEN** `VectorType(FLOAT, 3).read(data, offset)` is called
- **THEN** it reads 3 floats via `FLOAT.readRaw(data, offset + i * 4)` and reconstructs a `Vector`

### Requirement: FieldLayout tree mirrors Field hierarchy with byte offsets

The system SHALL provide a `FieldLayout` sealed interface in the `skadistats.clarity.model.state` package. The FieldLayout tree SHALL mirror the Field hierarchy but be completely independent of it — no references between FieldLayout and Field/Serializer/Decoder in either direction. The FieldLayout tree stores **byte offsets** and type information; the Field hierarchy stores structure. The layout has NO refIndex counter — refs are dynamically allocated at write time.

`Array.length` (from `ArrayField.getLength()`) is needed by `fieldPathIterator()` to know how many elements to scan. `Array` has NO refStride — refs within array elements work automatically because each element has its own byte-offset region containing its own slot-indices.

#### Scenario: Presence tracking via flag byte — three slot schemas

- **WHEN** layout computation assigns a Primitive slot
- **THEN** the slot occupies `1 + type.size` bytes total (1 flag byte + value bytes)
- **AND** the offset in the Primitive record points to the flag byte position
- **AND** the value is written/read at `offset + 1`
- **WHEN** layout computation assigns a Ref slot
- **THEN** the slot occupies `1 + 4` bytes (1 flag byte + int32 slot-index LE)
- **AND** on first write the slot-index is dynamically allocated via `S2FlatEntityState.allocateRefSlot()` and stored at `offset + 1`
- **AND** on read: `flag == 0 ? null : refs.get(INT_VH.get(data, offset + 1))`
- **WHEN** layout computation assigns a SubState slot
- **THEN** the slot occupies `1 + 4` bytes (same schema as Ref)
- **AND** the slot holds an `Entry` instance in `S2FlatEntityState.refs`
- **WHEN** `applyMutation` writes a WriteValue to a Primitive at offset
- **THEN** it sets `data[base + offset] = 1` (flag) and writes the value via `type.write(data, base + offset + 1, value)`
- **WHEN** `getValueForFieldPath` reads from a Primitive where `data[base + offset] == 0`
- **THEN** it returns `null`

### Requirement: SubStateKind distinguishes Vector from Pointer layout metadata

The system SHALL provide a `SubStateKind` sealed interface to hold the layout metadata needed to manage sub-Entry instances for VectorField and PointerField.

`SubStateKind.Pointer` carries the `pointerId` so that `S2FlatEntityState` can update `pointerSerializers[]` (inherited from `S2AbstractEntityState`) without accessing the Field hierarchy during mutation traversal. It also carries a parallel `Serializer[]` reference (same array as in `PointerField`) so that `lookupLayoutIndex(newSerializer)` can map a `StateMutation.SwitchPointer.newSerializer()` to the corresponding entry in `layouts[]` / `layoutBytes[]` via reference equality.

#### Scenario: VectorField produces SubState with Vector kind

- **WHEN** the FieldLayoutBuilder encounters a VectorField
- **THEN** it computes the element layout (starting at offset 0) and creates `SubState(offset=cursor, Vector(elementBytes, elementLayout))`
- **AND** the cursor advances by `1 + 4` (flag + slot-index) for this SubState

#### Scenario: PointerField produces SubState with Pointer kind

- **WHEN** the FieldLayoutBuilder encounters a PointerField with N possible serializers
- **THEN** it creates `SubState(offset=cursor, Pointer(pointerId, layouts[], layoutBytes[]))`
- **AND** the cursor advances by `1 + 4` (flag + slot-index)
- **AND** `pointerId` is taken from `PointerField.getPointerId()`

### Requirement: S2FlatEntityState extends S2AbstractEntityState with global refs and lean Entry

The system SHALL provide a `S2FlatEntityState` class extending `S2AbstractEntityState`. It SHALL own a global refs container (shared across all nesting levels) and a tree of `Entry` instances that each hold only a `byte[]`. This mirrors `S2NestedArrayEntityState`'s pattern of a global `List<Entry> entries` with a `Deque<Integer> freeEntries` free-list.

`S2FlatEntityState` inherits from `S2AbstractEntityState`:
- `rootField` (SerializerField) — for field path name resolution and field navigation
- `pointerSerializers[]` — global, flat pointer serializer tracking (shared across all nesting levels)
- `getFieldForFieldPath()`, `getNameForFieldPath()`, `getFieldPathForName()`, `getTypeForFieldPath()` — operate on the Field hierarchy

`S2FlatEntityState` additionally holds:
- `Object[] refs` + `int refsSize` — global container for **sub-Entry instances only**
- `int[] freeSlots` + `int freeSlotsTop` — global free-list stack for refs recycling
- `Entry rootEntry` — the root Entry containing the flat primitive storage

The inner `Entry` class is a pure byte[] container. It holds:
- `FieldLayout rootLayout` — the layout tree for this Entry's scope
- `byte[] data` — primitive value bytes and slot-indices for refs/sub-states

Sub-states for VectorField and PointerField are `Entry` instances living in `S2FlatEntityState.refs` at dynamically-allocated slot indices. They have NO refs of their own — all String values and nested sub-Entries across all nesting levels live in the single `S2FlatEntityState.refs` container. They do NOT carry their own pointerSerializers — pointer tracking is global on the outer `S2FlatEntityState`.

There SHALL be no owner-pointer or modifiable-flag machinery on `S2FlatEntityState`, `Entry`, `refs`, `freeSlots`, or `pointerSerializers`. Every mutation path writes directly; `copy()` allocates an independent state graph up front (see `S2FlatEntityState.copy() is an eager deep copy`).

#### Scenario: Sub-states are Entry instances in S2FlatEntityState.refs

- **WHEN** a VectorField or PointerField sub-state is created
- **THEN** an `Entry` instance is created with its own `byte[] data` and `rootLayout`
- **AND** a slot is allocated in `S2FlatEntityState.refs` via `allocateRefSlot()`
- **AND** the Entry is stored at that slot via `refs` write
- **AND** the slot-index is stored in the parent Entry's `byte[]` at the SubState's offset+1
- **AND** the SubState's flag byte (at offset) is set to 1

#### Scenario: Dynamic ref slot allocation

- **WHEN** a WriteValue on a Ref is applied and the flag byte was 0
- **THEN** a slot is allocated from `S2FlatEntityState.refs` via `allocateRefSlot()` (reusing a free slot or appending)
- **AND** the slot-index is written to `data[offset+1]` via `INT_VH.set`
- **AND** the flag byte is set to 1
- **AND** the refs slab stores the value at that slot
- **WHEN** a WriteValue on a Ref is applied and the flag byte was 1
- **THEN** the existing slot-index is read from `data[offset+1]`
- **AND** the refs slab updates the value in place (no new allocation)

#### Scenario: Ref slot freed on null write

- **WHEN** a WriteValue with `value == null` is applied to a Ref with flag=1
- **THEN** the slot-index is read, `freeRefSlot(slot)` marks the slot reusable, and the flag is cleared

### Requirement: S2FlatEntityState dispatches on StateMutation via applyMutation

`applyMutation(FieldPath, StateMutation)` SHALL traverse the FieldLayout tree using a `base` accumulator, a `layout` cursor, and a `current` Entry reference. When a SubState is encountered mid-traversal, the loop SHALL read the slot-index from `current.data[base + s.offset + 1]`, look up the sub-Entry in `S2FlatEntityState.refs`, swap `current` to the sub-Entry, and `continue` to reprocess the current FieldPath index. Descent through a SubState consumes no fp index.

The SubState branch MUST NOT include its own `if (i == last) break` check. SubState-as-leaf operations (`SwitchPointer`, `ResizeVector`, or any other op targeting the SubState position itself) reach the dispatch via the Composite/Array branch advancing `layout = SubState` on the last idx and the loop's terminal break.

**Lazy sub-Entry creation:** When the flag byte at `base + s.offset` is 0 mid-traversal, `applyMutation` SHALL lazy-create the sub-Entry to mirror `S2NestedArrayEntityState`'s implicit creation:
- **Pointer with `serializers.length == 1`**: create sub-Entry with `layouts[0]` / `layoutBytes[0]`, allocate slot, set flag, set `pointerSerializers[pointerId] = serializers[0]`.
- **Pointer with `serializers.length > 1`**: throw — the protocol must emit `SwitchPointer` before any inner write.
- **Vector**: create sub-Entry sized to fit `nextIdx + 1` elements (`Array(0, elementBytes, nextIdx+1, elementLayout)`).

After any descent through `SubState(Vector)`, if the sub-Entry's `Array.length < nextIdx + 1`, `applyMutation` SHALL grow the sub-Entry via byte[] reallocation and update its `rootLayout` with the new length. This mirrors `S2NestedArrayEntityState.ensureNodeCapacity`'s `idx + 1` fallback.

At the leaf, the method SHALL dispatch on the `StateMutation` type: `WriteValue` writes the value at the leaf; `ResizeVector` resizes the SubState(Vector)'s sub-Entry; `SwitchPointer` swaps the SubState(Pointer)'s sub-Entry and updates `pointerSerializers[]`.

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
- **AND** `refs[slot] = value` stores the value
- **AND** returns `true` (capacity changed)
- **WHEN** `applyMutation` receives a `WriteValue(non-null value)` and the leaf is a Ref with flag=1
- **THEN** the existing slot is read from `data[offset+1]` and `refs[slot] = value` updates in place
- **AND** returns `false`
- **WHEN** `applyMutation` receives a `WriteValue(null)` and the leaf is a Ref with flag=1
- **THEN** `freeRefSlot(slot)` releases the slot and flag is set to 0
- **AND** returns `true`

#### Scenario: ResizeVector on a SubState(Vector)

- **WHEN** `applyMutation` receives a `ResizeVector(count)` at a SubState(Vector) leaf with no existing sub-Entry (flag=0)
- **AND** count > 0
- **THEN** a sub-Entry with `byte[count * elementBytes]` is created, a slot is allocated in `refs`, the slot-index and flag are stored in the parent's byte[]
- **AND** returns `false`
- **WHEN** a sub-Entry exists and `newCount != oldCount`
- **THEN** a new `byte[newCount * elementBytes]` is allocated, existing data is copied up to `min(oldCount, newCount) * elementBytes`, and `sub.data` is replaced with the new array
- **AND** `sub.rootLayout` is replaced with a new `Array` record carrying the new length
- **AND** on shrink, dropped tail slot-indices are orphaned in refs (no recursive cleanup — matches `S2NestedArrayEntityState.clearEntryRef`)
- **AND** returns `true` iff any occupied path existed in the dropped tail `[newCount, oldCount)`
- **WHEN** `newCount == oldCount`
- **THEN** no mutation occurs and returns `false`

#### Scenario: SwitchPointer on a SubState(Pointer)

- **WHEN** `applyMutation` receives a `SwitchPointer(newSerializer)` at a SubState(Pointer) leaf
- **THEN** it reads `pointerId` from `SubStateKind.Pointer.pointerId()`
- **AND** if flag was 1 AND (`newSerializer == null` OR `pointerSerializers[pointerId] != newSerializer`): the direct slot is freed via `freeRefSlot` (nested slots are orphaned), flag cleared, `pointerSerializers[pointerId] = null`
- **AND** if `newSerializer != null` AND flag is now 0: a new sub-Entry is created with the pre-computed layout and bytes for `newSerializer`, a slot is allocated, slot-index and flag stored in parent's byte[], `pointerSerializers[pointerId] = newSerializer`
- **AND** returns `true` iff the cleared old sub-Entry contained any occupied paths

### Requirement: S2FlatEntityState.copy() is an eager deep copy

`S2FlatEntityState.copy()` SHALL return a state that is fully independent of the original at the moment of return. No byte[] array, `refs` slot, `Entry` instance, `freeSlots` array, or `pointerSerializers` array SHALL be shared with the original after `copy()` returns. Subsequent mutations on either state SHALL NOT be observable from the other, without any additional per-write bookkeeping.

`copy()` SHALL:
1. Clone `pointerSerializers` via `Arrays.copyOf`.
2. Clone `refs` via `Arrays.copyOf(refs, refs.length)` (preserving slot indices at their original positions).
3. Clone `freeSlots` via `Arrays.copyOf(freeSlots, freeSlots.length)` and copy `freeSlotsTop`.
4. Clone `rootEntry` as a new `Entry` instance with `rootLayout` shared by reference (layout is immutable) and `data` cloned via `Arrays.copyOf`.
5. For each slot `i` in `0..refsSize-1`, if `refs[i]` is an `Entry` instance, replace the cloned `refs[i]` with a freshly cloned `Entry` (recursively cloning `data` and any descendant sub-Entries).

Slot stability SHALL be preserved — every sub-Entry in the clone occupies the same slot index it occupied in the original.

`copy()` SHALL NOT walk the FieldLayout tree. The sub-Entry traversal walks `refs` directly; FieldLayout shape is not needed to enumerate reachable Entries because the `refs` slab is the single back-reference container for all sub-Entries.

#### Scenario: copy() returns fully independent state

- **WHEN** `copy()` is invoked on a S2FlatEntityState
- **THEN** the returned state's `rootEntry`, `rootEntry.data`, `refs`, `freeSlots`, `pointerSerializers`, and every sub-`Entry` reachable through `refs` are newly allocated
- **AND** no byte[] or array in the returned state is `==` to any byte[] or array in the original
- **AND** every mutation on the copy (primitive write, ref write, sub-state resize, pointer switch) leaves the original's observable state unchanged
- **AND** vice versa

#### Scenario: Slot indices are preserved across copy

- **WHEN** the original has a sub-Entry at `refs[k]` reachable via slot-index `k` in some parent Entry's byte[]
- **THEN** the copy has the sub-Entry clone at `refs[k]` reachable via the same slot-index `k` in the copy's cloned parent byte[]
- **AND** the byte[] slot-indices stored in data bytes do not need rewriting

#### Scenario: Subsequent writes do not cross-affect

- **WHEN** `copy()` is invoked and the copy is then mutated via `applyMutation`, `write`, `decodeInto`, or any structural mutation
- **THEN** no ownership check or clone-on-write operation occurs during the mutation — the write proceeds directly on the copy's independently-allocated data
- **AND** the original's state is bit-for-bit unchanged

### Requirement: S2FlatEntityState provides fieldPathIterator

`fieldPathIterator()` SHALL iterate over all FieldPaths whose values have been set. It SHALL walk the FieldLayout tree and check presence flags for Primitives, non-null checks for Refs and SubStates. Structural operations (ResizeVector, SwitchPointer) do not store values, so their paths are naturally excluded.

#### Scenario: Iterate only set field paths

- **WHEN** `fieldPathIterator()` is called
- **THEN** it yields only FieldPaths whose Primitive flag byte is non-zero, whose Ref is non-null, or whose SubState exists
- **AND** for SubStates, it delegates to the sub-state's iterator to include nested FieldPaths

### Requirement: Runner-configurable EntityStateType

The `S2EntityStateType` enum SHALL include a `FLAT` variant that produces S2FlatEntityState instances. The default SHALL remain `NESTED_ARRAY`. The existing `withS2EntityState()` method on `AbstractFileRunner` already supports selecting the state type — only the `FLAT` enum variant needs to be added.

#### Scenario: Configure S2FlatEntityState via Runner

- **WHEN** `new SimpleRunner(source).withS2EntityState(S2EntityStateType.FLAT).runWith(processor)` is called
- **THEN** all S2 entities created during the run use `S2FlatEntityState`

#### Scenario: Default to S2NestedArrayEntityState

- **WHEN** no `withS2EntityState()` is called
- **THEN** all S2 entities use `S2NestedArrayEntityState`

### Requirement: Refs slot release is transitive

When `S2FlatEntityState` releases a `refs` slot that holds a sub-Entry, it SHALL also release every `FieldLayout.Ref` and `FieldLayout.SubState` slot transitively reachable through that Entry's `data` via its `rootLayout`, returning all of them to `freeSlots`. No sub-Entry or value-Ref SHALL remain in `refs` after its sole navigation path from the root has been removed.

The transitive release SHALL be triggered from both mutation primitives that remove navigation edges:

- `switchPointer` when an existing sub-Entry is replaced or cleared (the slot formerly occupied by the old sub-Entry)
- `resizeVector` shrink when the truncated tail of `sub.data` contains `FieldLayout.Ref` or `FieldLayout.SubState` slot indices

The walk SHALL read `data` byte arrays and mutate `this.refs` and `this.freeSlots` directly; no sharing check or clone step is required because `data`, `refs`, and `freeSlots` are owned outright by `this` after `copy()` performs eager deep cloning.

Plain `FieldLayout.Ref` leaves hold arbitrary `Object` values (not sub-Entries); their slots SHALL be freed non-recursively via `freeRefSlot`. `FieldLayout.SubState` slots hold sub-Entries and SHALL be released recursively.

#### Scenario: SwitchPointer releases the whole sub-Entry subtree

- **GIVEN** a pointer SubState whose current sub-Entry `E` has a `FieldLayout.Ref` at offset `r` storing slot `s1`, and a nested `FieldLayout.SubState` at offset `s` storing slot `s2` whose own sub-Entry contains further Ref/SubState slots
- **WHEN** `SwitchPointer` replaces or clears that pointer
- **THEN** the top-level slot of `E` is returned to `freeSlots`
- **AND** slot `s1` is returned to `freeSlots`
- **AND** slot `s2` and every further Ref/SubState slot reachable from its sub-Entry are returned to `freeSlots`

#### Scenario: ResizeVector shrink releases slots in the discarded tail

- **GIVEN** a vector sub-Entry whose element layout contains `FieldLayout.Ref` or `FieldLayout.SubState` positions, and element indices `[M..N-1]` are about to be dropped by a shrink from length `N` to `M`
- **WHEN** `resizeVector` applies the shrink
- **THEN** for each element index `i` in `[M..N-1]`, every occupied Ref or SubState slot reachable through that element is returned to `freeSlots` before `sub.data` is reallocated

### Requirement: S2FlatEntityState provides decodeInto for primitive decode-direct path

`S2FlatEntityState` SHALL provide a method `decodeInto(FieldPath fp, Decoder decoder, BitStream bs)` that traverses the FieldLayout tree to the leaf layout for `fp` and dispatches to the decoder's static `decodeInto` method, writing decoded bytes directly into the Entry's `byte[]` without producing an intermediate boxed `Object` or allocating a `StateMutation.WriteValue` record.

The traversal SHALL be identical in shape to `applyMutation`: Composite/Array/SubState cases walk the layout and accumulate `base`; SubState descent is a direct pointer-chase (`sub = (Entry) refs[slot]`) with no ownership check. Lazy sub-Entry creation (Pointer with `serializers.length == 1`, Vector sized to `nextIdx + 1`) and vector growth on traversal are identical to `applyMutation`'s behavior.

At the leaf:

- **If the leaf layout is `Primitive`**: `decodeInto` SHALL read the flag byte at `base + p.offset()`, set it to `(byte) 1`, and invoke `DecoderDispatch.decodeInto(bs, decoder, data, base + p.offset() + 1)`. It SHALL return `oldFlag == 0`.
- **If the leaf layout is `Ref`**: `decodeInto` SHALL fall back to the boxing path — invoke `DecoderDispatch.decode(bs, decoder)` to produce an `Object`, then perform the equivalent of `WriteValue` on the Ref slot.
- **If the leaf layout is `SubState`**: `decodeInto` SHALL throw `IllegalStateException`.

`decodeInto` SHALL NOT allocate any intermediate `StateMutation` record on the Primitive path.

**Capacity-change semantics:** values decoded from the bitstream are never null. On the Primitive path, `decodeInto` returns true only for the null→value transition (`oldFlag == 0` before the write). It SHALL NOT produce value→null transitions.

#### Scenario: Primitive decodeInto writes via VarHandle with no autobox

- **WHEN** `decodeInto(fp, intDecoder, bs)` is called and the leaf layout is `Primitive(offset, Scalar.INT)`
- **THEN** the flag byte at `base + offset` is set to 1
- **AND** `DecoderDispatch.decodeInto(bs, intDecoder, data, base + offset + 1)` dispatches to the decoder's static `decodeInto`, which reads the int from the bitstream and writes it via `INT_VH.set(data, base + offset + 1, value)`
- **AND** no `Integer` object is allocated
- **AND** no `StateMutation.WriteValue` record is allocated
- **AND** the method returns true if the old flag was 0, false otherwise

#### Scenario: Ref-typed decodeInto falls back to boxing path

- **WHEN** `decodeInto(fp, stringDecoder, bs)` is called and the leaf layout is `Ref(offset)`
- **THEN** the value is decoded via `DecoderDispatch.decode(bs, decoder)` producing a `String`
- **AND** the Ref write proceeds identically to `applyMutation(fp, WriteValue(string))`

#### Scenario: Structural leaf throws

- **WHEN** `decodeInto(fp, decoder, bs)` is called and the leaf layout is `SubState`
- **THEN** `IllegalStateException` is thrown
- **AND** callers are expected to invoke `applyMutation(fp, ResizeVector(...))` or `applyMutation(fp, SwitchPointer(...))` for structural leaves

### Requirement: Strings are stored inline in the composite byte[]

All String-typed leaves SHALL be stored inline in the composite `byte[]` rather than in the `refs` slab. There is no refs fallback for strings — the decoder's hardcoded 9-bit length prefix caps every string at 511 bytes at the wire level, which is small enough that uniform inline reservation is always viable.

FieldLayout SHALL expose an inline-string leaf shape — either a dedicated `Primitive.String(offset, prefixBytes, maxLength)` leaf type or an extension of the existing `Primitive` leaf carrying a `maxLength` field where `maxLength > 0` indicates a String leaf. The leaf reserves `prefixBytes + maxLength` bytes at the leaf's offset in the composite byte[]:

- **S2 `char[N]` props**: `maxLength = N`.
- **Unbounded-metadata strings** — S2 `CUtlString` AND all S1 `PropType.STRING`: `maxLength = 512` uniformly.
- **`prefixBytes = 2`** uniformly.

The length prefix encodes the actual string length in bytes; remaining bytes in the reserved span are unspecified.

`StringLenDecoder` SHALL provide a `decodeInto(BitStream, byte[], int offset)` variant that reads the 9-bit length + UTF-8 bytes from the bitstream and writes them as `[length-prefix][utf8-bytes]` at `data[offset..]`. The decoder SHALL assert `decodedLength ≤ maxLength` for the target leaf.

`S2FlatEntityState.decodeInto(fp, decoder, bs)` at an inline-string leaf SHALL dispatch to the String decoder's `decodeInto` variant. `S2FlatEntityState.write(fp, decoded)` at an inline-string leaf SHALL encode the String to UTF-8 bytes and write the length-prefixed form inline.

`S2FlatEntityState.getValueForFieldPath(fp)` at an inline-string leaf SHALL read the length prefix and allocate a `String` from the UTF-8 bytes.

#### Scenario: Inline string roundtrip

- **WHEN** an inline-string leaf is written via `decodeInto` with a 12-byte UTF-8 string
- **THEN** the length prefix at `offset` encodes 12
- **AND** bytes `offset+prefixBytes .. offset+prefixBytes+11` contain the UTF-8 bytes
- **AND** `getValueForFieldPath(fp)` returns a String equal to the original 12-byte value

#### Scenario: refs slab holds only sub-Entry instances

- **WHEN** a S2FlatEntityState is written to across any leaf shape
- **THEN** `refs` slot allocations only occur for sub-Entry creation (Pointer sub-states, Vector sub-Entries)
- **AND** no `refs` slot is used for String values

### Requirement: S2FlatEntityState provides a unified direct-write method

`S2FlatEntityState` SHALL provide a `write(FieldPath fp, Object decoded)` method that traverses the FieldLayout tree and dispatches on leaf shape, writing the decoded value directly without a `StateMutation` wrapper:

- **Primitive leaf (fixed-width)** → set flag byte to 1, call `PrimitiveType.write(data, offset, decoded)` to box-unbox the value into byte[].
- **Primitive leaf (inline-string)** → encode `(String) decoded` to UTF-8 bytes, write length-prefix + bytes inline.
- **Ref leaf** → `allocateRefSlot` if new, store `decoded` at slot, set flag byte.
- **SubState-Pointer leaf** → `decoded` is a `Serializer` (or null). If different from current: clear existing sub-Entry via the existing pointer-switch logic, set `pointerSerializers[pointerId] = decoded`, lazily create the new sub-Entry shell.
- **SubState-Vector leaf** → `decoded` is an `Integer` count. Resize the vector sub-Entry's capacity.

`write` SHALL be equivalent in observable behavior to `applyMutation(fp, field.createMutation(decoded, fp.last() + 1))` on all leaf shapes — with no intermediate `StateMutation` allocation.

#### Scenario: write on Primitive leaf

- **WHEN** `flatState.write(fp, Integer.valueOf(42))` is called on a Primitive int leaf
- **THEN** the flag byte at `offset` is set to 1
- **AND** bytes at `offset+1..offset+4` contain 42 via `INT_VH.set`
- **AND** the method returns true if the old flag was 0

#### Scenario: write on SubState-Pointer leaf switches serializer immediately

- **WHEN** `flatState.write(fp, newSerializer)` is called on a SubState-Pointer leaf and `newSerializer != current`
- **THEN** `pointerSerializers[pointerId]` is updated in place
- **AND** the existing sub-Entry (if any) is cleared from `refs`
- **AND** subsequent `resolveField` calls read the new serializer directly from `state.pointerSerializers`

#### Scenario: write on SubState-Vector leaf resizes

- **WHEN** `flatState.write(fp, Integer.valueOf(5))` is called on a SubState-Vector leaf whose current length is 3
- **THEN** the vector sub-Entry's capacity is grown to 5
- **AND** the method returns the same capacity-change signal as `applyMutation(fp, ResizeVector(5))` would have
