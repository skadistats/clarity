## MODIFIED Requirements

### Requirement: Field readers write state directly without StateMutation

No field reader on the hot path SHALL produce a `StateMutation` record. Both `S2FieldReader.readFieldsFast` and `S1FieldReader.readFields` SHALL write decoded values directly into state via `EntityState.decodeInto(FieldPath, Decoder, BitStream)` (zero-box, primitive leaves on FLAT-family states) or `EntityState.write(FieldPath, Object)` (single direct-write method for everything else).

`S2FieldReader.readFieldsFast` SHALL route per field:

- **FLAT-family state + primitive leaf** (`state instanceof FlatEntityState && field.isPrimitiveLeaf()`): invoke `flatState.decodeInto(fp, decoder, bs)`. The `isFlat` check is hoisted to the top of the method; the per-field check collapses to `isFlat && field.isPrimitiveLeaf()`. No `StateMutation` is allocated; no `Object` is boxed by the decoder.
- **Everything else** (FLAT + non-primitive leaf, non-FLAT state, any leaf shape): decode via `DecoderDispatch.decode(bs, decoder)` to produce a value; invoke `state.write(fp, decoded)` immediately. The state's own leaf traversal dispatches on layout shape — no `StateMutation` wrapper.

`S1FieldReader.readFields` SHALL route per field:

- **Primitive or inline-string leaf**: invoke `state.decodeInto(fp, decoder, bs)`.
- **Ref leaf** (if any survive the string-inlining design): invoke `state.write(fp, DecoderDispatch.decode(bs, decoder))`.

The `pointerOverrides[]` shadow map in `S2FieldReader` SHALL be removed. Under immediate-write semantics, the state's own `pointerSerializers[]` is updated the moment a pointer field is written; subsequent `resolveField` calls read current serializers from state directly.

Readers SHALL NOT stage mutations into `FieldChanges.mutations[]` on the fast path. State is mutated in-place as each field is decoded. Packet-level atomicity is preserved by the caller (`Entities.processAndRunPacketEntities`) via snapshot-and-rollback — see the `entity-update-commit` capability.

#### Scenario: S2 fast path + primitive leaf uses decodeInto

- **WHEN** `readFieldsFast` processes a field whose state is `FlatEntityState` and whose leaf is Primitive
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

### Requirement: StateMutation retained for non-reader paths

The `StateMutation` class and `EntityState.applyMutation(FieldPath, StateMutation)` SHALL be retained for the following callers:

- **`S2FieldReader.readFieldsDebug`**: produces and applies materialized `StateMutation` records for full inspection fidelity by `dtinspector` and similar tools.
- **Baseline materialization**: `readFields` invoked with `debug=false` for baseline construction MAY populate `FieldChanges.mutations[]` via the legacy staging semantics. Baseline construction is not on the hot path.
- **`MutationRecorder`** (trace capture in `src/jmh/java/skadistats/clarity/bench/trace/`): records decode events as serializable `StateMutation` sequences. Reads the value via `state.getValueForFieldPath(fp)` after `onUpdateWrite` callback invocation (see `entity-update-commit` capability).
- **Programmatic writes**: any internal code that wishes to apply a declarative mutation to state MAY call `state.applyMutation(fp, mutation)` directly.

The class itself — `StateMutation`, `StateMutation.WriteValue`, `StateMutation.ResizeVector`, `StateMutation.SwitchPointer` — SHALL be unchanged in shape. No methods are added or removed.

#### Scenario: readFieldsDebug produces materialized StateMutations

- **WHEN** `S2FieldReader.readFieldsDebug` processes a packet
- **THEN** it produces `StateMutation` records via `field.createMutation(decoded, ...)` for each decoded field
- **AND** the records are stored in `FieldChanges.mutations[]`
- **AND** `fieldChanges.applyTo(state)` applies them via `state.applyMutation`

#### Scenario: MutationRecorder trace capture uses StateMutation

- **WHEN** `MutationRecorder` captures an update-path write
- **THEN** it receives the `onUpdateWrite(state, fp)` callback after decode completes
- **AND** it reads the written value via `state.getValueForFieldPath(fp)`
- **AND** records it as a `StateMutation.WriteValue` in the captured trace

#### Scenario: Programmatic applyMutation still works

- **WHEN** internal code calls `state.applyMutation(fp, new StateMutation.WriteValue(value))` outside the reader
- **THEN** the mutation is applied to state as before
- **AND** returns the capacity-change signal per the existing contract

### Requirement: FieldChanges carries paths and capacity flag

`FieldChanges` SHALL store:
- `FieldPath[] fieldPaths` — always populated
- `int length`
- `StateMutation[] mutations` — **null** on the fast path; populated by `readFieldsDebug` and baseline materialization
- `boolean capacityChanged` — populated by `readFieldsFast`, `S1FieldReader.readFields`, and `readFieldsDebug` during decode

A fast-path constructor `FieldChanges(FieldPath[] paths, int length, boolean capacityChanged)` SHALL create instances with `mutations == null`. A legacy constructor retaining the `StateMutation[] mutations` parameter SHALL exist for the debug/baseline paths.

`FieldChanges.applyTo(EntityState state)` SHALL:
- Return `capacityChanged` directly (with no side effects) when `mutations == null`
- Otherwise, for each entry call `state.applyMutation(fieldPaths[i], mutations[i])` and return the OR of their capacity-change signals

#### Scenario: FieldChanges on fast path carries only paths and capacity flag

- **WHEN** `S2FieldReader.readFieldsFast` or `S1FieldReader.readFields` returns
- **THEN** `FieldChanges.fieldPaths[]` is populated with decoded paths
- **AND** `FieldChanges.mutations` is null
- **AND** `FieldChanges.capacityChanged` reflects accumulated capacity-change signals
- **AND** `fieldChanges.applyTo(state)` is a no-op returning `capacityChanged`

#### Scenario: FieldChanges on debug path carries staged mutations

- **WHEN** `S2FieldReader.readFieldsDebug` returns
- **THEN** both `fieldPaths[]` and `mutations[]` are populated
- **AND** `fieldChanges.applyTo(state)` applies each mutation via `applyMutation`

#### Scenario: Baseline materialization uses staged path

- **WHEN** `Entities` materializes a baseline via the reader for an empty state
- **THEN** the reader MAY populate `mutations[]` via the legacy staging semantics
- **AND** the baseline is applied via `fieldChanges.applyTo(state)`
