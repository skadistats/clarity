## MODIFIED Requirements

### Requirement: Field readers write state directly without StateMutation

No field reader on the hot path SHALL produce a `StateMutation` record. Both `S2FieldReader.readFieldsFast` and `S1FieldReader.readFields` SHALL write decoded values directly into state via `EntityState.decodeInto(FieldPath, Decoder, BitStream)` (zero-box, primitive leaves on FLAT-family states) or `EntityState.write(FieldPath, Object)` (single direct-write method for everything else).

`S2FieldReader.readFieldsFast` SHALL route per field:

- **FLAT-family state + primitive leaf** (`state instanceof S2FlatEntityState && field.isPrimitiveLeaf()`): invoke `flatState.decodeInto(fp, decoder, bs)`. The `isFlat` check is hoisted to the top of the method; the per-field check collapses to `isFlat && field.isPrimitiveLeaf()`. No `StateMutation` is allocated; no `Object` is boxed by the decoder.
- **Everything else** (FLAT + non-primitive leaf, non-FLAT state, any leaf shape): decode via `DecoderDispatch.decode(bs, decoder)` to produce a value; invoke `state.write(fp, decoded)` immediately. The state's own leaf traversal dispatches on layout shape — no `StateMutation` wrapper.

`S1FieldReader.readFields` SHALL route per field:

- **Primitive or inline-string leaf**: invoke `state.decodeInto(fp, decoder, bs)`.
- **Ref leaf** (if any survive the string-inlining design): invoke `state.write(fp, DecoderDispatch.decode(bs, decoder))`.

The `pointerOverrides[]` shadow map in `S2FieldReader` SHALL be removed. Under immediate-write semantics, the state's own `pointerSerializers[]` is updated the moment a pointer field is written; subsequent `resolveField` calls read current serializers from state directly.

Readers SHALL NOT stage mutations into `FieldChanges.mutations[]` on the fast path. State is mutated in-place as each field is decoded. Packet-level atomicity is preserved by the caller (`Entities.processAndRunPacketEntities`) via snapshot-and-rollback — see the `entity-update-commit` capability.

#### Scenario: S2 fast path + primitive leaf uses decodeInto

- **WHEN** `readFieldsFast` processes a field whose state is `S2FlatEntityState` and whose leaf is Primitive
- **THEN** it calls `flatState.decodeInto(fp, decoder, bs)`
- **AND** no `StateMutation` is allocated
- **AND** no `Integer`/`Float`/`Long`/`Boolean`/`Vector`/`String` object is allocated by the decoder

#### Scenario: S2 fast path + non-primitive leaf uses write

- **WHEN** `readFieldsFast` processes a field whose leaf is Ref, SubState-Pointer, or SubState-Vector, on any state
- **THEN** it calls `DecoderDispatch.decode` to obtain the decoded value
- **AND** calls `state.write(fp, decoded)` immediately
- **AND** no `StateMutation` is allocated

#### Scenario: S2 fast path — pointer switch mutates state immediately

- **WHEN** `readFieldsFast` processes a pointer field whose decoded serializer differs from the current one
- **THEN** `state.write(fp, newSerializer)` updates `state.pointerSerializers[pointerId]` in place
- **AND** subsequent `resolveField` calls within the same packet read the new serializer directly from state
- **AND** no `pointerOverrides[]` shadow map is used

#### Scenario: S1 reader decode-direct

- **WHEN** `S1FieldReader.readFields` processes a primitive or inline-string field
- **THEN** it calls `state.decodeInto(fp, decoder, bs)`
- **AND** no `WriteValue` is allocated
- **AND** no entry is written to `FieldChanges.mutations[]`

#### Scenario: S1FieldReader wraps all values via write (refs only)

- **WHEN** `S1FieldReader.readFields` processes a field whose leaf is Ref (if any non-inline refs exist)
- **THEN** it calls `state.write(fp, DecoderDispatch.decode(bs, decoder))` immediately

### Requirement: Field classes lose state-manipulation methods

The methods `setValue(S2NestedEntityState, int, int, Object)`, `getValue(S2NestedEntityState, int)`, `ensureCapacity(S2NestedEntityState, int)`, and `isHiddenFieldPath()` SHALL be removed from the `Field` class and all subclasses. `createMutation(Object)` SHALL be the only state-related method on Field.

#### Scenario: Field has no S2NestedEntityState dependency

- **WHEN** the Field class is inspected after this change
- **THEN** it has no import of or reference to `S2NestedEntityState`
- **AND** the methods `setValue`, `getValue`, `ensureCapacity`, `isHiddenFieldPath` do not exist
- **AND** `createMutation(Object)` is the only method that relates to state operations
