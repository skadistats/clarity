## MODIFIED Requirements

### Requirement: FlatEntityState extends AbstractS2EntityState with global refs and lean Entry

The system SHALL provide a `FlatEntityState` class extending `AbstractS2EntityState`. It SHALL own a global refs container (shared across all nesting levels) and a tree of `Entry` instances that each hold only a `byte[]`. This mirrors `NestedArrayEntityState`'s pattern of a global `List<Entry> entries` with a `Deque<Integer> freeEntries` free-list (NestedArrayEntityState.java:17-18, 142-164).

`FlatEntityState` inherits from `AbstractS2EntityState`:
- `rootField` (SerializerField) — for field path name resolution and field navigation
- `pointerSerializers[]` — global, flat pointer serializer tracking (shared across all nesting levels)
- `getFieldForFieldPath()`, `getNameForFieldPath()`, `getFieldPathForName()`, `getTypeForFieldPath()` — operate on the Field hierarchy

`FlatEntityState` additionally holds:
- `Object[] refs` + `int refsSize` — global container for **sub-Entry instances only** (Strings are stored inline in the composite byte[] via the inline-string leaf shape; the mixed Strings-and-sub-Entries design is removed). `refsSize` is the logical length; grow via `Arrays.copyOf` when `refsSize == refs.length`
- `int[] freeSlots` + `int freeSlotsTop` — global free-list stack for refs recycling
- `FlatEntityState refsOwner` — owner pointer for the refs slab pair; `refsOwner == this` means this state may mutate `refs` and `freeSlots` in place, otherwise first mutation clones both via `Arrays.copyOf` and sets owner
- `FlatEntityState pointerSerializersOwner` — owner pointer for the `pointerSerializers` array; same semantics
- `Entry rootEntry` — the root Entry containing the flat primitive storage (including inline-string bytes)

The inner `Entry` class is a pure byte[] container. It holds:
- `FieldLayout rootLayout` — the layout tree for this Entry's scope
- `byte[] data` — primitive value bytes and slot-indices for refs/sub-states
- `FlatEntityState owner` — owner pointer; `owner == <the FlatEntityState about to write>` means the write may proceed in place, otherwise the write clones `data` and produces a new Entry whose `owner` is set to the writing state

Sub-states for VectorField and PointerField are `Entry` instances living in `FlatEntityState.refs` at dynamically-allocated slot indices. They have NO refs of their own — all String values and nested sub-Entries across all nesting levels live in the single `FlatEntityState.refs` container. They do NOT carry their own pointerSerializers — pointer tracking is global on the outer `FlatEntityState`.

The previous `boolean modifiable` / `boolean refsModifiable` / `boolean pointerSerializersModifiable` flag machinery SHALL be removed in favor of the owner-pointer mechanism above. `markSubEntriesNonModifiable` SHALL be removed.

#### Scenario: Sub-states are Entry instances in FlatEntityState.refs

- **WHEN** a VectorField or PointerField sub-state is created
- **THEN** an `Entry` instance is created with its own `byte[] data`, `rootLayout`, and `owner = <creating FlatEntityState>`
- **AND** a slot is allocated in `FlatEntityState.refs` via `allocateRefSlot()`
- **AND** the Entry is stored at that slot via `refs` write
- **AND** the slot-index is stored in the parent Entry's `byte[]` at the SubState's offset+1
- **AND** the SubState's flag byte (at offset) is set to 1

#### Scenario: Dynamic ref slot allocation

- **WHEN** a WriteValue on a Ref is applied and the flag byte was 0
- **THEN** a slot is allocated from `FlatEntityState.refs` via `allocateRefSlot()` (reusing a free slot or appending)
- **AND** the slot-index is written to `data[offset+1]` via `INT_VH.set`
- **AND** the flag byte is set to 1
- **AND** the refs slab stores the value at that slot
- **WHEN** a WriteValue on a Ref is applied and the flag byte was 1
- **THEN** the existing slot-index is read from `data[offset+1]`
- **AND** the refs slab updates the value in place (no new allocation)

#### Scenario: Ref slot freed on null write

- **WHEN** a WriteValue with `value == null` is applied to a Ref with flag=1
- **THEN** the slot-index is read, `freeRefSlot(slot)` marks the slot reusable, and the flag is cleared

#### Scenario: Owner pointer governs Entry mutability

- **WHEN** a write needs to mutate an Entry and `entry.owner == writingState`
- **THEN** the write proceeds in place on `entry.data`
- **WHEN** a write needs to mutate an Entry and `entry.owner != writingState`
- **THEN** `entry.data` is cloned, a new Entry `{owner = writingState, data = clonedData, rootLayout = entry.rootLayout}` is produced, and the reference that pointed at the old Entry (either `rootEntry` or a refs slot) is updated to point at the new Entry

### Requirement: FlatEntityState supports two-axis copy-on-write

FlatEntityState SHALL implement COW on multiple independent lazy axes via owner-pointer checks:
1. **Per-Entry**: each Entry has an `owner` reference; a write by a state whose identity does not match `entry.owner` clones `entry.data`, produces a new Entry, and updates the referring slot.
2. **Global refs**: `FlatEntityState.refsOwner` reference; a write that needs to mutate the refs slab or freeSlots clones both containers (via `Arrays.copyOf` on `refs` and `freeSlots`) if `refsOwner != this` and sets `refsOwner = this`. `refs` and `freeSlots` are always cloned together — they represent a single logical allocator state.
3. **pointerSerializers**: `FlatEntityState.pointerSerializersOwner` reference; a `SwitchPointer` that needs to update `pointerSerializers[pointerId]` clones the array if the owner does not match, then sets the owner to `this`.

`copy()` SHALL be O(1). It SHALL:
1. Set the new state's `rootField` to the original's `rootField`
2. Share the `pointerSerializers` array by reference; neither state's `pointerSerializersOwner` equals the new state until first SwitchPointer write
3. Share the `refs` slab and `freeSlots` by reference; neither state's `refsOwner` equals the new state until first refs-touching write
4. Share the `rootEntry` by reference; neither state's identity matches `rootEntry.owner` until first write through the root
5. NOT walk the FieldLayout tree, NOT call any per-Entry bookkeeping, NOT allocate the `pointerSerializers` clone

`copy()` SHALL NOT read or write any byte[], SHALL NOT visit any sub-Entry, and SHALL NOT perform work proportional to the entity's size.

On first write to an Entry whose `owner` differs from the writing state, the write path SHALL clone `data`, construct a new Entry with `owner = <writing state>`, and update the reference that pointed to the old Entry.

On first write that reaches a Ref/SubState leaf where `refsOwner != writingState`, the refs slab and freeSlots SHALL both be cloned and `refsOwner` set to the writing state, before the ref write proceeds.

On first write of type `SwitchPointer` where `pointerSerializersOwner != writingState`, the `pointerSerializers` array SHALL be cloned and `pointerSerializersOwner` set to the writing state.

Slot indices stored in byte[] remain valid across COW — they index into whichever `refs` container the current FlatEntityState currently owns.

#### Scenario: copy() is O(1)

- **WHEN** `copy()` is invoked on a FlatEntityState
- **THEN** the method completes in constant time regardless of entity size, sub-Entry count, or field count
- **AND** no FieldLayout traversal occurs
- **AND** no `byte[]` allocation occurs
- **AND** no Entry object is constructed
- **AND** no pre-existing sub-Entries in the refs slab are visited

#### Scenario: Copy and modify independently (primitive writes only)

- **WHEN** a FlatEntityState is copied via `copy()`
- **AND** the first write on the copy is a Primitive WriteValue on the root Entry
- **THEN** the copy detects `rootEntry.owner != this`, clones `rootEntry.data`, creates a new Entry with `owner = copy`, and replaces its own `rootEntry` reference
- **AND** the refs slab is NOT cloned (no ref write occurred)
- **AND** the original state's `rootEntry` is unchanged

#### Scenario: Copy and modify refs independently

- **WHEN** a FlatEntityState is copied
- **AND** the first write on the copy writes a new String value to a Ref
- **THEN** the copy clones `rootEntry.data` (owner mismatch)
- **AND** the copy clones the `refs` slab and `freeSlots` (refsOwner mismatch) and sets `refsOwner = copy`
- **AND** the copy allocates a slot in its own refs; the original's refs is unchanged

#### Scenario: SubState COW — clone-and-replace at boundary

- **WHEN** a FlatEntityState is copied and the copy traverses through a SubState to write a leaf value
- **THEN** the traversal reads the slot-index from `current.data[base+s.offset+1]`
- **AND** looks up the sub-Entry in the refs slab (initially the same instance on both sides)
- **AND** if the sub-Entry's `owner != copy`: the refs slab is cloned if needed (refsOwner check), the sub-Entry's `data` is cloned, a replacement Entry with `owner = copy` is constructed, and the refs slot is updated
- **AND** the replacement sub-Entry keeps the same slot-index in its byte[] — slot stability across COW
- **AND** writes proceed on the replacement sub-Entry, the original's sub-Entry is unchanged

#### Scenario: SubState COW applies to SubState-as-leaf mutations too

- **WHEN** a FlatEntityState is copied and the copy applies `ResizeVector` to an existing vector sub-Entry
- **AND** the sub-Entry's `owner != copy`
- **THEN** the sub-Entry is cloned, the copy is placed at its slot in the (now-owned) refs, and the clone's `owner` is set to `copy` — before any in-place mutation of `data` or `rootLayout`
- **AND** the original's sub-Entry remains unmodified in the original's refs container
- **WHEN** a FlatEntityState is copied and the copy applies `SwitchPointer` to an existing pointer sub-Entry
- **THEN** `pointerSerializers` is cloned first if its owner does not match the copy
- **AND** no in-place mutation of the existing sub-Entry occurs — the old sub-Entry is either freed (direct slot in the copy's refs) or replaced by a newly constructed Entry, so no clone of the shared sub-Entry object is needed

## ADDED Requirements

### Requirement: FlatEntityState provides decodeInto for primitive decode-direct path

`FlatEntityState` SHALL provide a method `decodeInto(FieldPath fp, Decoder decoder, BitStream bs)` that traverses the FieldLayout tree to the leaf layout for `fp` and dispatches to the decoder's static `decodeInto` method, writing decoded bytes directly into the Entry's `byte[]` without producing an intermediate boxed `Object` or allocating a `StateMutation.WriteValue` record.

The traversal SHALL be identical in shape to `applyMutation`: Composite/Array/SubState cases walk the layout and accumulate `base`; `makeWritable` ownership checks are applied along the path as sub-Entries are reached. Lazy sub-Entry creation (Pointer with `serializers.length == 1`, Vector sized to `nextIdx + 1`) and vector growth on traversal are identical to `applyMutation`'s behavior.

At the leaf:

- **If the leaf layout is `Primitive`**: `decodeInto` SHALL read the flag byte at `base + p.offset()`, set it to `(byte) 1`, and invoke `DecoderDispatch.decodeInto(bs, decoder, data, base + p.offset() + 1)`. It SHALL return `oldFlag == 0` (null → value capacity-change signal).
- **If the leaf layout is `Ref`**: `decodeInto` SHALL fall back to the boxing path — invoke `DecoderDispatch.decode(bs, decoder)` to produce an `Object`, then perform the equivalent of `WriteValue` on the Ref slot (flag byte, slot allocation if needed, store in refs slab). It SHALL return the capacity-change signal per the same rules as `applyMutation`/`WriteValue` on Ref.
- **If the leaf layout is `SubState`**: `decodeInto` SHALL throw `IllegalStateException` — structural mutations (`ResizeVector`, `SwitchPointer`) do not go through `decodeInto`; callers must use `applyMutation` with the appropriate `StateMutation` variant.

`decodeInto` SHALL NOT allocate any intermediate `StateMutation` record on the Primitive path. The autobox and record allocation that occur on `applyMutation(fp, WriteValue(decoded))` SHALL NOT occur on `decodeInto(fp, decoder, bs)` when the leaf is `Primitive`.

**Capacity-change semantics:** values decoded from the bitstream are never null. On the Primitive path, `decodeInto` returns true only for the null→value transition (`oldFlag == 0` before the write). It SHALL NOT produce value→null transitions; those arise exclusively via `WriteValue(null)` through `applyMutation`, or via structural mutations.

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

### Requirement: Decoder class provides static decodeInto for primitive decoders

Each primitive `@RegisterDecoder`-annotated decoder class whose `getPrimitiveType()` returns non-null SHALL provide a `public static void decodeInto(BitStream bs, <Self> d, byte[] data, int offset)` method (or `public static void decodeInto(BitStream bs, byte[] data, int offset)` for stateless decoders). The method SHALL read from the bitstream and write the decoded value directly at `data[offset..]` without allocating any intermediate object.

Reference-producing decoders (String decoders, any decoder whose `getPrimitiveType()` returns null) SHALL NOT provide a `decodeInto` method. The call site is responsible for dispatching to `decode` and using the boxing path for such decoders.

Compound primitive decoders (`VectorDefaultDecoder`, `VectorDecoder`, `VectorXYDecoder`, `ArrayDecoder` of primitive inner) SHALL provide `decodeInto` that dispatches to the inner decoder's `decodeInto` at the appropriate element offset (`offset + i * element.size()` for Vector/Array). No intermediate `Vector` or `Object[]` SHALL be allocated.

#### Scenario: IntSignedDecoder.decodeInto

- **WHEN** `IntSignedDecoder.decodeInto(bs, d, data, offset)` is called
- **THEN** it reads `bs.readSBitInt(d.nBits)` and writes via `INT_VH.set(data, offset, value)`
- **AND** no `Integer` is allocated

#### Scenario: VectorDefaultDecoder.decodeInto composes element decodeInto

- **WHEN** `VectorDefaultDecoder.decodeInto(bs, d, data, offset)` is called with `d.dim = 3`
- **THEN** for each `i` in `[0, 3)` it calls `DecoderDispatch.decodeInto(bs, d.innerDecoder, data, offset + i * 4)`
- **AND** no `Vector` or `float[]` is allocated

#### Scenario: String decoder has no decodeInto

- **WHEN** a decoder's `getPrimitiveType()` returns `null` (e.g., `StringLenDecoder`, `StringZeroTerminatedDecoder`)
- **THEN** no static `decodeInto` method is defined
- **AND** `DecoderDispatch.decodeInto` does not include a case for that decoder's ID
- **AND** callers must route such decoders through `DecoderDispatch.decode` and the boxing path

### Requirement: DecoderDispatch.decodeInto provides static dispatch

The generated `DecoderDispatch` class SHALL provide a `public static void decodeInto(BitStream bs, Decoder decoder, byte[] data, int offset)` method that switches on `decoder.id` and invokes the corresponding concrete decoder's static `decodeInto`, mirroring the existing `DecoderDispatch.decode` structure.

Only primitive decoders (those whose `getPrimitiveType()` returns non-null) SHALL appear in the switch. Reference-producing decoder IDs SHALL hit the `default` branch, which throws `IllegalStateException` — callers must pre-filter using `decoder.getPrimitiveType() != null` before invoking `decodeInto`.

#### Scenario: Primitive decoder dispatch

- **WHEN** `DecoderDispatch.decodeInto(bs, decoder, data, offset)` is called and `decoder` is a primitive decoder
- **THEN** the switch routes to the concrete decoder's static `decodeInto`
- **AND** the call is resolved via int-id switch with no virtual dispatch

#### Scenario: Non-primitive decoder throws

- **WHEN** `DecoderDispatch.decodeInto(bs, decoder, data, offset)` is called and `decoder` is a reference-producing decoder with no `decodeInto` path
- **THEN** `IllegalStateException` is thrown
- **AND** the caller is expected to have used `decoder.getPrimitiveType() != null || isInlineStringDecoder(decoder)` as a routing predicate

### Requirement: Strings are stored inline in the composite byte[]

All String-typed leaves SHALL be stored inline in the composite `byte[]` rather than in the `refs` slab. There is no refs fallback for strings — the decoder's hardcoded 9-bit length prefix caps every string at 511 bytes at the wire level, which is small enough that uniform inline reservation is always viable.

FieldLayout SHALL expose an inline-string leaf shape — either a dedicated `Primitive.String(offset, prefixBytes, maxLength)` leaf type or an extension of the existing `Primitive` leaf carrying a `maxLength` field where `maxLength > 0` indicates a String leaf. The leaf reserves `prefixBytes + maxLength` bytes at the leaf's offset in the composite byte[]:

- **S2 `char[N]` props**: `maxLength = N`, with N taken directly from the declared type (`char[128]` → maxLength 128, `char[256]` → 256, etc.).
- **Unbounded-metadata strings** — S2 `CUtlString` AND all S1 `PropType.STRING`: `maxLength = 512` uniformly. This rounds up from the `StringLenDecoder`'s intrinsic 511-byte wire cap.
- **`prefixBytes = 2`** uniformly (handles up to 65535, covers the 512 reservation with headroom).

The length prefix encodes the actual string length in bytes; remaining bytes in the reserved span are unspecified (may hold stale data from prior writes).

`StringLenDecoder` SHALL provide a `decodeInto(BitStream, byte[], int offset)` variant that reads the 9-bit length + UTF-8 bytes from the bitstream and writes them as `[length-prefix][utf8-bytes]` at `data[offset..]`. The decoder SHALL assert `decodedLength ≤ maxLength` for the target leaf — exceeding bounds is a schema violation, not a runtime-recoverable condition.

`FlatEntityState.decodeInto(fp, decoder, bs)` at an inline-string leaf SHALL dispatch to the String decoder's `decodeInto` variant. `FlatEntityState.write(fp, decoded)` at an inline-string leaf SHALL encode the String to UTF-8 bytes and write the length-prefixed form inline.

`FlatEntityState.getValueForFieldPath(fp)` at an inline-string leaf SHALL read the length prefix and allocate a `String` from the UTF-8 bytes. This is a per-read allocation (unlike the refs-slab path, which returns a cached reference). Accepted trade-off — decode-time savings dominate in the replay-parsing workload.

#### Scenario: Inline string roundtrip

- **WHEN** an inline-string leaf is written via `decodeInto` with a 12-byte UTF-8 string
- **THEN** the length prefix at `offset` encodes 12
- **AND** bytes `offset+prefixBytes .. offset+prefixBytes+11` contain the UTF-8 bytes
- **AND** `getValueForFieldPath(fp)` returns a String equal to the original 12-byte value

#### Scenario: char[N] uses declared bound as inline size

- **WHEN** a S2 String prop is typed `char[128]`
- **THEN** FieldLayoutBuilder emits an inline-string leaf with `maxLength = 128`, `prefixBytes = 2`
- **AND** the leaf reserves 130 bytes in the byte[]

#### Scenario: Unbounded string uses uniform 512-byte reservation

- **WHEN** a S2 `CUtlString` prop, or any S1 `PropType.STRING` prop, is laid out
- **THEN** FieldLayoutBuilder emits an inline-string leaf with `maxLength = 512`, `prefixBytes = 2`
- **AND** the leaf reserves 514 bytes in the byte[]

#### Scenario: String exceeds declared max length

- **WHEN** `StringLenDecoder.decodeInto` reads a length prefix exceeding the leaf's `maxLength`
- **THEN** an `IllegalStateException` or equivalent schema-violation exception is thrown
- **AND** no partial write is committed beyond what was already written before the length check

#### Scenario: refs slab holds only sub-Entry instances

- **WHEN** a FlatEntityState is written to across any leaf shape
- **THEN** `refs` slot allocations only occur for sub-Entry creation (Pointer sub-states, Vector sub-Entries)
- **AND** no `refs` slot is used for String values (when inline threshold is in effect for the prop)

### Requirement: FlatEntityState provides a unified direct-write method

`FlatEntityState` SHALL provide a `write(FieldPath fp, Object decoded)` method that traverses the FieldLayout tree and dispatches on leaf shape, writing the decoded value directly without a `StateMutation` wrapper:

- **Primitive leaf (fixed-width)** → set flag byte to 1, call `PrimitiveType.write(data, offset, decoded)` to box-unbox the value into byte[]. Returns capacity-change `(oldFlag == 0) XOR (decoded == null)`.
- **Primitive leaf (inline-string)** → encode `(String) decoded` to UTF-8 bytes, write length-prefix + bytes inline. Returns capacity-change on null↔value transition.
- **Ref leaf** → `allocateRefSlot` if new, store `decoded` at slot, set flag byte. Returns capacity-change on null↔value transition.
- **SubState-Pointer leaf** → `decoded` is a `Serializer` (or null). If different from current: clear existing sub-Entry via the existing pointer-switch logic, set `pointerSerializers[pointerId] = decoded`, lazily create the new sub-Entry shell. Returns true if the previous sub-Entry held occupied state.
- **SubState-Vector leaf** → `decoded` is an `Integer` count. Resize the vector sub-Entry's capacity (same behavior as today's `handleResizeVector`). Returns true if the resize discarded occupied slots.

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
