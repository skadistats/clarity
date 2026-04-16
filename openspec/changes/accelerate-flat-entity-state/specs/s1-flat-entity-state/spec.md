## ADDED Requirements

### Requirement: S1FlatEntityState replaces ObjectArrayEntityState

The `ObjectArrayEntityState` class SHALL be removed. `S1FlatEntityState` SHALL be the sole S1 entity state implementation, providing byte[]-backed primitive storage, optional small refs slab for any non-inline references, and owner-pointer copy-on-write semantics consistent with `FlatEntityState`.

The S1 entity-state structure reflects the fully static, single-level Source 1 DT schema: no nesting, no SubState, no polymorphic pointers, no dynamic vectors. Every field's byte[] offset and leaf kind is fixed at `S1DTClass` compile time.

`S1FlatEntityState` SHALL hold:
- `S1FieldLayout layout` — an immutable per-DTClass object computed at DTClass compile time, mapping each `S1FieldPath.idx()` to `(leafKind, offset, maxLength)`. Shared across all `S1FlatEntityState` instances for that DTClass. No copy or traversal at runtime.
- `byte[] data` — composite primitive storage (sized at construction from `layout.totalBytes()`)
- `S1FlatEntityState dataOwner` — owner pointer gating writes to `data`
- *Optional* `Object[] refs` — tiny slab for any non-inline references (if any survive audit 0.6 / the inline-string threshold)
- *Optional* `S1FlatEntityState refsOwner` — owner pointer gating writes to `refs`

If the audit confirms no Ref leaves are needed, the `refs` and `refsOwner` fields are absent; the entire state is `byte[] data + S1FlatEntityState dataOwner + S1FieldLayout layout`.

#### Scenario: S1DTClass precomputes layout once

- **WHEN** `S1DTClass` compiles a DT
- **THEN** it produces an `S1FieldLayout` table mapping each prop index to `(leafKind, offset, maxLength)`
- **AND** the layout is immutable and shared across all `S1FlatEntityState` instances for that DTClass

#### Scenario: ObjectArrayEntityState is deleted

- **WHEN** the codebase is inspected after this change
- **THEN** no `ObjectArrayEntityState` class exists
- **AND** no references to it remain in source or configuration

### Requirement: S1FlatEntityState copy is O(1) via owner-pointer COW

`S1FlatEntityState.copy()` SHALL:
1. Share `layout` by reference (always immutable, no owner needed)
2. Share `data` by reference
3. Set `dataOwner = null` on the new state (not owned until first write)
4. Share `refs` by reference (if present); set `refsOwner = null`
5. NOT allocate any byte[]; NOT traverse any layout; NOT touch any reference except the four above

On the first write through `data` where `dataOwner != this`: clone `data` via `Arrays.copyOf`, set `dataOwner = this`. On the first write through `refs` where `refsOwner != this`: clone `refs` via `Arrays.copyOf`, set `refsOwner = this`.

#### Scenario: copy() is constant-time

- **WHEN** `S1FlatEntityState.copy()` is invoked
- **THEN** the method completes in constant time regardless of entity size or prop count
- **AND** no `byte[]` is allocated
- **AND** no layout traversal occurs

#### Scenario: First write clones byte[] once

- **WHEN** an `S1FlatEntityState` is copied and the copy performs its first write to `data`
- **THEN** `data` is cloned via `Arrays.copyOf`, `dataOwner` is set to the copy
- **AND** subsequent writes to the copy's `data` proceed in place without further cloning
- **AND** the original's `data` is unchanged

### Requirement: S1FlatEntityState provides decodeInto and write

`S1FlatEntityState` SHALL provide:

- `decodeInto(FieldPath fp, Decoder decoder, BitStream bs)` — for primitive leaves (and inline-string leaves), invoke `DecoderDispatch.decodeInto(bs, decoder, data, layout.offsetOf(fp))` after ensuring data ownership. Returns the capacity-change signal (null→value transition at the leaf).
- `write(FieldPath fp, Object decoded)` — for any leaf kind. Dispatches on `layout.kindOf(fp)`: Primitive → `PrimitiveType.write(data, offset, decoded)` with flag-byte update; inline-String → encode UTF-8 bytes, write length + bytes inline; Ref (if any) → refs slot write. No `StateMutation` allocated.

`S1FlatEntityState` SHALL also provide `applyMutation(FieldPath, StateMutation)` for compatibility with debug / baseline / programmatic callers — dispatching the mutation variant to the equivalent `write` behavior.

#### Scenario: decodeInto on S1 primitive leaf

- **WHEN** `s1State.decodeInto(fp, intDecoder, bs)` is called on a primitive int leaf
- **THEN** `dataOwner` is ensured to be the writing state (cloning `data` if needed)
- **AND** `DecoderDispatch.decodeInto(bs, intDecoder, data, layout.offsetOf(fp))` writes the bytes
- **AND** no `Integer` is allocated
- **AND** no `StateMutation` is allocated

#### Scenario: write on S1 inline-string leaf

- **WHEN** `s1State.write(fp, "hello")` is called on an inline-string leaf
- **THEN** the UTF-8 bytes of "hello" are encoded and written inline with a length prefix at `layout.offsetOf(fp)`
- **AND** no `StateMutation` is allocated

### Requirement: S1 decoders provide decodeInto

Each S1 decoder produced by `S1DecoderFactory` whose output is primitive or inline-string SHALL provide a static `decodeInto(BitStream, byte[], int offset)` method (or `decodeInto(BitStream, <Self>, byte[], int offset)` for stateful decoders). These decoders SHALL be added to the generated `DecoderDispatch.decodeInto` switch.

Decoders covered (derived from `S1DecoderFactory`):
- `IntDecoderFactory` outputs (int primitive decoders)
- `LongDecoderFactory` outputs
- `FloatDecoderFactory` outputs
- `VectorDecoderFactory` outputs (dim 2 and 3 — inlined as 2×float or 3×float)
- `ArrayDecoderFactory` outputs — behavior determined by audit 0.6 (pre-flattened → primitive `decodeInto` composition; single-slot Object[] → refs slot write via `write`)
- `StringLenDecoder` — inline-string `decodeInto` per the `flat-entity-state` capability's inline-string requirement (reused across S1 and S2)

#### Scenario: Every S1 primitive decoder has decodeInto parity with decode

- **WHEN** a ported S1 decoder's `decodeInto(bs, data, offset)` is compared to `PrimitiveType.write(data, offset, decode(bs))` across a synthetic bitstream corpus
- **THEN** the resulting bytes at `data[offset..]` are byte-identical

### Requirement: S1FieldReader writes state directly

`S1FieldReader.readFields` SHALL route per field:

- **Primitive or inline-string leaf**: invoke `state.decodeInto(fp, decoder, bs)`.
- **Ref leaf** (if any survive audit 0.6): invoke `state.write(fp, DecoderDispatch.decode(bs, decoder))`.

`S1FieldReader` SHALL NOT allocate any `StateMutation.WriteValue` record on the hot path. `S1FieldReader` SHALL NOT populate `FieldChanges.mutations[]` on the hot path — returning a fast-path `FieldChanges` with `mutations == null` and the accumulated `capacityChanged` flag.

#### Scenario: S1FieldReader decode-direct

- **WHEN** `S1FieldReader.readFields` processes a packet of S1 updates
- **THEN** each primitive/inline-string field is decoded via `state.decodeInto`
- **AND** each ref-leaf field (if any) is decoded via `DecoderDispatch.decode` + `state.write`
- **AND** no `StateMutation.WriteValue` is allocated
- **AND** `FieldChanges.mutations` is null in the returned `FieldChanges`
