## ADDED Requirements

### Requirement: S1FlatEntityState added alongside ObjectArrayEntityState

The system SHALL provide a `S1FlatEntityState` class in the `skadistats.clarity.model.state` package implementing `EntityState`. It SHALL store primitive, inline-string, and reference field values in a single `byte[] data` slab with an optional `Object[] refs` sidecar for reference-typed leaves (currently only `ArrayDecoder`-produced `Object[]` values). It SHALL NOT extend `AbstractS2EntityState` — S1 has no nested serializers, no polymorphic pointers, and no global pointer-serializer tracking.

`ObjectArrayEntityState` SHALL be retained as a parallel, selectable variant for benchmarking and as a fallback. `S1EntityStateType` SHALL gain a `FLAT` variant alongside the existing `OBJECT_ARRAY`; both SHALL remain selectable at runtime via `withS1EntityState(S1EntityStateType)`. `AbstractFileRunner.s1EntityStateType` SHALL default to `S1EntityStateType.FLAT`; consumers wishing to use the legacy boxed path MAY opt in with `.withS1EntityState(S1EntityStateType.OBJECT_ARRAY)`.

`S1FlatEntityState` SHALL hold:
- `S1FlatLayout layout` — shared per-DTClass, immutable.
- `byte[] data` — primitive bytes, inline-string bytes, and slot-indices for refs, sized at construction to `layout.dataBytes()`.
- `Object[] refs` — **null** when `layout.refSlots() == 0`; otherwise a slab for reference-typed leaves.
- `int[] freeSlots` + `int freeSlotsTop` — free-list for `refs`; null when `refs` is null.

There SHALL be no owner-pointer, modifiable flag, or copy-on-write machinery. Every write mutates `data` / `refs` in place.

#### Scenario: Construction allocates byte[] plus optional refs

- **WHEN** a new `S1FlatEntityState` is constructed for a given `S1DTClass`
- **THEN** `data` is allocated as `new byte[layout.dataBytes()]` (all bytes zero — flag bytes start at 0)
- **AND** `refs` is allocated only if `layout.refSlots() > 0`; it starts empty with `freeSlotsTop = 0`

#### Scenario: No pointer-serializer tracking

- **WHEN** the `S1FlatEntityState` class is inspected
- **THEN** it SHALL NOT extend `AbstractS2EntityState`
- **AND** it SHALL NOT hold a `pointerSerializers[]` array
- **AND** name resolution (`Entity.getProperty("name")` etc.) SHALL resolve through `S1DTClass.propsByName` + `S1FlatLayout`, not through state-held data

### Requirement: S1FlatLayout maps receive-prop indices to leaf kind and offset

The system SHALL provide a `S1FlatLayout` class in the `skadistats.clarity.model.state` package. It SHALL be built once per `S1DTClass` (eagerly or lazily on first `getFlatLayout()` call) and cached on the class. It SHALL be immutable after construction.

`S1FlatLayout` SHALL expose:
- `LeafKind[] kinds()` — one entry per receive-prop, indexed by `S1FieldPath.idx()`.
- `int[] offsets()` — byte offset of each prop's (flag + value) region in `data[]`.
- `PrimitiveType[] primTypes()` — non-null only at indices where `kinds()[i] == PRIMITIVE`.
- `int[] maxLengths()` — non-zero only at indices where `kinds()[i] == INLINE_STRING`; uniform value 512 at those indices.
- `int dataBytes()` — total `byte[]` size.
- `int refSlots()` — upper bound on `refs.length`; equals the count of indices where `kinds()[i] == REF`.

```java
public enum LeafKind { PRIMITIVE, INLINE_STRING, REF }
```

#### Scenario: LeafKind classification rules

- **WHEN** the builder processes a receive-prop whose decoder's `getPrimitiveType()` returns non-null
- **THEN** `kinds()[i]` is `PRIMITIVE`
- **AND** `primTypes()[i]` is set to that `PrimitiveType`
- **AND** `offsets()[i]` advances by `1 + primType.size()` (1 flag byte + value bytes)

- **WHEN** the builder processes a receive-prop whose decoder is a `StringLenDecoder`
- **THEN** `kinds()[i]` is `INLINE_STRING`
- **AND** `maxLengths()[i]` is `512`
- **AND** `offsets()[i]` advances by `1 + 2 + 512` (flag + 2-byte LE length prefix + 512 data bytes)

- **WHEN** the builder processes a receive-prop whose decoder is neither primitive nor `StringLenDecoder` (e.g. `ArrayDecoder`)
- **THEN** `kinds()[i]` is `REF`
- **AND** `offsets()[i]` advances by `1 + 4` (flag + 4-byte int slot-index via `INT_VH`)
- **AND** `refSlots()` is incremented

#### Scenario: Layout is shared across all entity instances of a DTClass

- **WHEN** two `S1FlatEntityState` instances are created for the same `S1DTClass`
- **THEN** both hold the same `S1FlatLayout` reference (identity-equal)
- **AND** neither instance's writes modify the shared layout

#### Scenario: Uniform inline-string reservation

- **WHEN** any S1 `PropType.STRING` prop is laid out
- **THEN** its leaf reserves exactly `1 + 2 + 512 = 515` bytes
- **AND** the 512-byte value region is grounded in `StringLenDecoder`'s intrinsic 9-bit wire cap (511 bytes) rounded for alignment
- **AND** no per-prop metadata is consulted (S1 STRING props carry no max-length in `SendProp` metadata)

### Requirement: S1FlatEntityState.decodeInto writes directly for primitive and inline-string leaves

`S1FlatEntityState.decodeInto(FieldPath fp, Decoder decoder, BitStream bs)` SHALL resolve `idx = fp.s1().idx()`, read `offset = layout.offsets()[idx]`, set `data[offset] = 1` (flag byte), and dispatch on `layout.kinds()[idx]`:

- `PRIMITIVE` → `DecoderDispatch.decodeInto(bs, decoder, data, offset + 1)`.
- `INLINE_STRING` → `StringLenDecoder.decodeIntoInline(bs, data, offset + 1, layout.maxLengths()[idx])`.
- `REF` → throw `IllegalStateException` — the caller must route REF leaves to `write` instead.

The method SHALL return `false` unconditionally. S1 has no structural capacity changes; `FieldChanges.capacityChanged` is always `false` on the S1 fast path.

#### Scenario: Primitive decode writes directly into byte[]

- **WHEN** `decodeInto(fp, intSignedDecoder, bs)` is called on a prop with `kinds()[idx] == PRIMITIVE`
- **THEN** `data[offset]` is set to `1` (flag)
- **AND** `DecoderDispatch.decodeInto` writes the decoded int into `data[offset+1..offset+4]` via `INT_VH`
- **AND** no `Integer`, `WriteValue`, or other wrapper is allocated

#### Scenario: Inline-string decode writes length-prefixed UTF-8 into byte[]

- **WHEN** `decodeInto(fp, stringLenDecoder, bs)` is called on a prop with `kinds()[idx] == INLINE_STRING`
- **THEN** `data[offset]` is set to `1` (flag)
- **AND** `StringLenDecoder.decodeIntoInline` writes a 2-byte LE length prefix at `data[offset+1..offset+2]` followed by UTF-8 bytes at `data[offset+3..]`
- **AND** no `String` object is allocated during decode
- **AND** the written byte layout matches `StringLenDecoder.decode(bs)` followed by a length-prefix + UTF-8 write

#### Scenario: decodeInto on REF leaf throws

- **WHEN** `decodeInto(fp, arrayDecoder, bs)` is called on a prop with `kinds()[idx] == REF`
- **THEN** `IllegalStateException` is thrown with a message identifying the idx
- **AND** no state is mutated

### Requirement: S1FlatEntityState.write covers all three leaf kinds

`S1FlatEntityState.write(FieldPath fp, Object decoded)` SHALL dispatch on `layout.kinds()[idx]`:

- `PRIMITIVE` → set flag to 1, `layout.primTypes()[idx].write(data, offset + 1, decoded)`.
- `INLINE_STRING` → set flag to 1, UTF-8 encode the `String`, write a 2-byte LE length prefix + bytes at `data[offset + 1 ..]`, truncating at `maxLength` if the encoded form exceeds it.
- `REF` — if the flag is 0: allocate a slot via `allocateRefSlot()` (pop from `freeSlots` or append-and-grow `refs`), write the slot index to `data[offset + 1]` via `INT_VH.set`, set the flag, store `decoded` at `refs[slot]`. If the flag is 1: read the existing slot index, overwrite `refs[slot]`.

The method SHALL return `false` unconditionally.

#### Scenario: write on PRIMITIVE leaf matches decodeInto byte layout

- **WHEN** `write(fp, boxedInt)` is called on a `PRIMITIVE(INT)` leaf
- **THEN** `data[offset]` is set to `1`
- **AND** `PrimitiveType.Scalar.INT.write(data, offset + 1, boxedInt)` is invoked
- **AND** the resulting byte layout is identical to calling `decodeInto(fp, intSignedDecoder, bs)` on a bitstream seeded with the same int value

#### Scenario: write on INLINE_STRING leaf truncates at maxLength

- **WHEN** `write(fp, "hello")` is called on an `INLINE_STRING(maxLength=512)` leaf
- **THEN** `data[offset]` is set to `1`
- **AND** `data[offset+1..offset+2]` contains `0x05, 0x00` (LE length)
- **AND** `data[offset+3..offset+7]` contains the UTF-8 bytes `h e l l o`
- **WHEN** `write(fp, s)` is called with an encoded length > `maxLength`
- **THEN** only the first `maxLength` UTF-8 bytes are written
- **AND** the stored length prefix equals `maxLength`

#### Scenario: write on REF leaf allocates on first write, overwrites on subsequent

- **WHEN** `write(fp, obj1)` is called on a `REF` leaf with `data[offset] == 0`
- **THEN** a new slot is allocated (via freelist pop or `refs` append)
- **AND** `data[offset + 1..offset + 4]` stores the slot index via `INT_VH.set`
- **AND** `data[offset]` is set to `1`
- **AND** `refs[slot] == obj1`
- **WHEN** `write(fp, obj2)` is subsequently called with `data[offset] == 1`
- **THEN** no new slot is allocated
- **AND** `refs[slot] == obj2` (in-place overwrite)

#### Scenario: Refs slab grows via Arrays.copyOf

- **WHEN** `allocateRefSlot()` is called with `freeSlotsTop == 0` and `refs` is full
- **THEN** `refs` is grown via `Arrays.copyOf(refs, refs.length * 2)`
- **AND** the returned slot is the previous `refs.length`

### Requirement: S1FlatEntityState.applyMutation delegates to write

`S1FlatEntityState.applyMutation(FieldPath fp, StateMutation mutation)` SHALL cast `mutation` to `StateMutation.WriteValue` and delegate to `write(fp, wv.value())`. S1 has no `ResizeVector` or `SwitchPointer` mutations — any non-`WriteValue` input is a protocol violation and SHALL throw via the cast.

#### Scenario: applyMutation extracts value and delegates

- **WHEN** `applyMutation(fp, new WriteValue(v))` is called
- **THEN** `write(fp, v)` is invoked
- **AND** the return value of `write` is returned

#### Scenario: applyMutation rejects non-WriteValue

- **WHEN** `applyMutation(fp, new ResizeVector(0))` or `new SwitchPointer(null)` is called
- **THEN** `ClassCastException` is thrown
- **AND** no state is mutated

### Requirement: S1FlatEntityState.getValueForFieldPath reads via leaf kind

`S1FlatEntityState.getValueForFieldPath(FieldPath fp)` SHALL return `null` when the flag byte at `offset` is 0. Otherwise:

- `PRIMITIVE` → return `layout.primTypes()[idx].read(data, offset + 1)`.
- `INLINE_STRING` → read the 2-byte LE length prefix at `data[offset + 1 ..offset + 2]`, then allocate and return `new String(data, offset + 3, len, StandardCharsets.UTF_8)`.
- `REF` → read the slot index at `data[offset + 1]` via `INT_VH.get`, return `refs[slot]`.

The flag-gated `null` return matches the semantic of the current `ObjectArrayEntityState` (unset `Object[]` slots read as `null`).

#### Scenario: Unset slot returns null

- **WHEN** `getValueForFieldPath(fp)` is called on a prop whose flag byte is 0
- **THEN** `null` is returned
- **AND** no per-leaf-kind decoding is attempted

#### Scenario: Inline-string read allocates String from byte[]

- **WHEN** `getValueForFieldPath(fp)` is called on a populated `INLINE_STRING` leaf
- **THEN** the returned value is `new String(data, offset + 3, len, UTF_8)`
- **AND** subsequent reads allocate a fresh `String` each time (no caching in this change)

### Requirement: S1FlatEntityState.fieldPathIterator yields every prop

`S1FlatEntityState.fieldPathIterator()` SHALL yield `new S1FieldPath(i)` for every `i` in `0 .. layout.kinds.length - 1`, regardless of the flag byte. This preserves the semantic of `ObjectArrayEntityState.fieldPathIterator` so downstream consumers (name lookups, `dtinspector` dumps) observe no behavioural diff.

#### Scenario: Iterator yields every prop unconditionally

- **WHEN** `fieldPathIterator()` is called on a freshly-constructed (zero-written) state
- **THEN** it yields `S1FieldPath(0), S1FieldPath(1), ..., S1FieldPath(n-1)` where `n = layout.kinds.length`
- **AND** consumers that call `getValueForFieldPath` on each yielded path receive `null` for unset slots

### Requirement: S1FlatEntityState.copy is an eager deep copy

`S1FlatEntityState.copy()` SHALL return a fully-independent `S1FlatEntityState` instance. The new instance SHALL share the `S1FlatLayout` (always immutable) by reference, clone `data` via `Arrays.copyOf(data, data.length)`, and, if present, clone `refs` and `freeSlots` via `Arrays.copyOf`. `freeSlotsTop` SHALL be copied by value.

No owner pointers, writable flags, or lazy-clone machinery SHALL be used. This matches the post-`strip-entity-state-cow` discipline of `FlatEntityState` and `NestedArrayEntityState`.

#### Scenario: Copy is independent

- **WHEN** `copy` is called on a populated `S1FlatEntityState`
- **AND** the returned state is subsequently mutated via `write` or `decodeInto`
- **THEN** the original state is unchanged

- **WHEN** the original state is subsequently mutated
- **THEN** the returned copy is unchanged

#### Scenario: Copy allocates byte[] plus optional refs clone

- **WHEN** `copy` is called on a state with `refs == null`
- **THEN** exactly one `S1FlatEntityState` wrapper and one `byte[]` are allocated
- **AND** no `Object[]` or `int[]` is allocated

- **WHEN** `copy` is called on a state with `refs != null`
- **THEN** one `S1FlatEntityState` wrapper, one `byte[]`, one `Object[]`, and one `int[]` are allocated

### Requirement: S1FieldReader fast path is state-agnostic and uses decodeInto / write

`S1FieldReader.readFields(bs, dtClass, state, debug=false, materialize=false)` SHALL route each decoded field per `S1FlatLayout.kinds()`, which is built by `S1DTClass` and available regardless of which state variant is in use:

- `PRIMITIVE` or `INLINE_STRING` → `state.decodeInto(fieldPaths[ci], decoder, bs)`.
- `REF` → `state.write(fieldPaths[ci], DecoderDispatch.decode(bs, decoder))`.

No `StateMutation` SHALL be produced on this path. `FieldChanges` SHALL be constructed via the fast-path constructor `new FieldChanges(fieldPaths, n, false)` — `mutations == null`, `capacityChanged == false`.

Both `S1FlatEntityState` and `ObjectArrayEntityState` SHALL implement `decodeInto(fp, decoder, bs)` so the reader's call shape is uniform. `S1FlatEntityState.decodeInto` writes primitive / inline-string bytes directly (zero allocation; this capability, earlier Requirements). `ObjectArrayEntityState.decodeInto` SHALL fall back to `state[fp.s1().idx()] = DecoderDispatch.decode(bs, decoder); return false` — the legacy variant still pays the full boxing cost, but the reader path is unified.

`readFields(bs, dtClass, state, debug=true)` and `readFields(bs, dtClass, state, debug=false, materialize=true)` SHALL retain the boxed `DecoderDispatch.decode` + `new StateMutation.WriteValue(...)` staging path, populating `FieldChanges.mutations[]` for debug inspection and for `MutationRecorder` / baseline materialization respectively. Neither is on the hot path. Both apply identically across state variants.

#### Scenario: Fast-path field dispatch on S1FlatEntityState

- **WHEN** `readFields` processes a `PRIMITIVE` or `INLINE_STRING` prop on `S1FlatEntityState`
- **THEN** `state.decodeInto(fp, decoder, bs)` is invoked
- **AND** no `StateMutation` record is allocated
- **AND** no `Integer`/`Float`/`Long`/`Boolean`/`String` wrapper is allocated by the decoder

- **WHEN** `readFields` processes a `REF` prop on `S1FlatEntityState`
- **THEN** `DecoderDispatch.decode(bs, decoder)` produces a boxed value (currently an `Object[]` from `ArrayDecoder`)
- **AND** `state.write(fp, decoded)` is invoked
- **AND** no `StateMutation` record is allocated

#### Scenario: Fast-path field dispatch on ObjectArrayEntityState

- **WHEN** `readFields` processes any prop on `ObjectArrayEntityState`
- **THEN** `state.decodeInto(fp, decoder, bs)` or `state.write(fp, decoded)` is invoked (identical routing to FLAT)
- **AND** the state's `decodeInto` implementation falls back to `DecoderDispatch.decode + state[idx] = decoded`
- **AND** the decoder still allocates boxed values (primitive wrappers, `String`, etc.)
- **AND** no `StateMutation` record is allocated on the reader side
- **AND** the reader contract remains uniform across variants — only the state's per-op cost differs

#### Scenario: Fast-path FieldChanges shape

- **WHEN** `readFields` returns from the fast path
- **THEN** `fieldChanges.getFieldPaths()` yields the decoded paths
- **AND** `fieldChanges.mutations` is `null`
- **AND** `fieldChanges.capacityChanged` is `false`
- **AND** `fieldChanges.applyTo(state)` is a no-op returning `false`

#### Scenario: Debug path retains staging

- **WHEN** `readFields` is invoked with `debug=true`
- **THEN** each decoded value is wrapped in `new StateMutation.WriteValue(...)` and stored in `mutations[]`
- **AND** the `TextTable` debug output is printed
- **AND** `fieldChanges.applyTo(state)` iterates `mutations[]` via `state.applyMutation`

#### Scenario: Materialize path produces mutations

- **WHEN** `readFields` is invoked with `debug=false, materialize=true`
- **THEN** each decoded value is wrapped in `new StateMutation.WriteValue(...)` and stored in `mutations[]`
- **AND** no debug printing is performed
- **AND** `MutationRecorder` (or the caller) may capture the mutations for trace replay or baseline materialization
