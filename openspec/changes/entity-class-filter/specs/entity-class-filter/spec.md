## ADDED Requirements

### Requirement: Opt-in per-class entity filter on the runner

The runner SHALL accept an optional `Predicate<DTClass>` filter set before parse starts. The filter SHALL be consulted exactly once per entity CREATE — when an entity_id transitions from "not present" to "present" on the wire — and SHALL determine whether a Java-side `Entity` is materialized for that id. Once parse starts, the filter SHALL be immutable for the duration of the run. The default behavior (no filter set) SHALL be byte-identical to behavior before this change: every transmitted entity gets a Java-side representation.

#### Scenario: Filter accepts the class

- **WHEN** the runner has a filter set and an entity CREATE arrives for a `DTClass` for which `filter.test(dtClass)` returns `true`
- **THEN** a Java-side `Entity` SHALL be created, state SHALL be populated by decoding the CREATE's fields, and subsequent UPDATEs SHALL flow through the existing full-decode path. Consumer-visible behavior for this entity SHALL be identical to running without a filter.

#### Scenario: Filter rejects the class

- **WHEN** the runner has a filter set and an entity CREATE arrives for a `DTClass` for which `filter.test(dtClass)` returns `false`
- **THEN** no Java-side `Entity` SHALL be created for that entity_id, no entity-create or entity-update event SHALL fire for that id, and `entities.getById(id)` SHALL return `null` for that id.

#### Scenario: No filter set

- **WHEN** the runner has no filter set (the default)
- **THEN** every transmitted entity SHALL be materialized as a Java-side `Entity`, exactly as before this change.

#### Scenario: Filter cannot be changed mid-parse

- **WHEN** a caller attempts to set or modify the filter after parse has started
- **THEN** the call SHALL throw an `IllegalStateException`.

### Requirement: Wire-stream consumption of filtered entities

Even when an entity is filtered, the parser SHALL consume the wire-format bits for that entity's CREATE and UPDATE messages exactly as a non-filtered parser would, so that the bitstream cursor remains aligned for the next entity in the packet. No allocation of decoded values, no state writes, no Java-side `Entity` allocation, and no consumer-side dispatch SHALL occur for filtered entities.

#### Scenario: Filtered CREATE consumes the right bit count

- **WHEN** an entity CREATE for a filtered class arrives in `CSVCMsg_PacketEntities`
- **THEN** the parser SHALL advance the bitstream past the CREATE's fields by exactly the bit count that a full-decode CREATE would consume, and SHALL NOT allocate a decoded value, write entity state, or invoke consumer listeners.

#### Scenario: Filtered UPDATE consumes the right bit count

- **WHEN** an entity UPDATE arrives for an entity_id whose class was filtered at CREATE time
- **THEN** the parser SHALL execute every FieldOp in the UPDATE's FieldOp section, resolve each affected field via the class's serializer, and call the field decoder's `skip` operation. The bitstream cursor SHALL advance by the same bit count a full-decode UPDATE would consume. No decoded value SHALL be allocated, no state SHALL be written, and no consumer listener SHALL fire.

#### Scenario: Filtered DELETE has no consumer effect

- **WHEN** an entity DELETE arrives for an entity_id whose class was filtered at CREATE time
- **THEN** no consumer-side delete event SHALL fire (the consumer never saw the entity exist), but the parser's internal "this id is filtered" tracking for that id SHALL be cleared so that a future CREATE at the same id is re-evaluated against the filter.

#### Scenario: Filtered CREATE does not update the baseline registry

- **WHEN** a CREATE for a filtered class arrives with the `updateBaseline` flag set
- **THEN** the parser SHALL NOT call `baselineRegistry.updateEntityBaseline(...)` for that entity, and the per-entity baseline registry slot for that `eIdx` SHALL remain unchanged. This is safe because per-entity baselines at a given `eIdx` are only consumed by future CREATEs at that same `eIdx`: a future CREATE of the same (filtered) class is itself filtered, and a future CREATE of a different class at that `eIdx` (entity-id reuse) draws from the per-class baseline in the `instancebaseline` stringtable, not from the per-entity slot.

### Requirement: Skip-parity invariant across decoders

For every concrete decoder, the `skip(BitStream)` operation SHALL advance the bitstream cursor by the same number of bits that `decode(BitStream)` would for the same input state. Any deviation corrupts the wire-format cursor for downstream fields and is a correctness bug.

#### Scenario: Static decoder API surface

- **WHEN** the codebase contains a class annotated with `@RegisterDecoder`
- **THEN** that class SHALL expose both a `decode` and a `skip` static method with matching argument shapes. The annotation processor SHALL fail the build if either is missing.

#### Scenario: Per-decoder bit-count equivalence

- **GIVEN** a decoder instance `d` and a BitStream `bs` positioned at some pos `P`
- **WHEN** `decode(bs, d)` is invoked, recording the new pos `P_decode`, and the bitstream is then rewound to `P` and `skip(bs, d)` is invoked, recording the new pos `P_skip`
- **THEN** `P_decode` and `P_skip` SHALL be equal for every input that `decode` accepts without throwing.

#### Scenario: End-to-end parity for any replay

- **GIVEN** a parseable replay and a filter that rejects every class (full-skip mode)
- **WHEN** that replay is parsed once with no filter and again with the reject-everything filter
- **THEN** both runs SHALL consume the bitstream identically; specifically, every per-packet tick boundary SHALL be reached at the same logical position, and the post-parse summary (tick count, byte offset of the final packet) SHALL match between the two runs.

### Requirement: Generated decoder skip dispatch

A generated `DecoderDispatch.skip(BitStream, Decoder)` SHALL provide an int-tableswitch on `decoder.id` matching the existing structure of `DecoderDispatch.decode`. Each case SHALL dispatch to the corresponding decoder class's static `skip` method.

#### Scenario: Skip-dispatch covers every registered decoder

- **WHEN** any class annotated `@RegisterDecoder` exists in the codebase
- **THEN** the generated `DecoderDispatch.skip` SHALL contain a `case DecoderIds.<NAME>` arm that calls that class's `skip` static method, regardless of whether the decoder has a `decodeInto` variant.

#### Scenario: Default case is a hard failure

- **WHEN** `DecoderDispatch.skip` is invoked with a decoder whose id does not match any registered case
- **THEN** the call SHALL throw `IllegalArgumentException`, matching the existing `decode` dispatch's behavior.

### Requirement: New skipFields method on FieldReader

`FieldReader` SHALL expose a `skipFields(BitStream, DTClass, EntityState)` method that walks the FieldOp section of an entity update and advances the bitstream past every field's value without allocating decoded results or writing to entity state. A default implementation that delegates to `readFields` (discarding the result) SHALL be provided for `FieldReader` implementations that have not been specialized for skipping.

#### Scenario: S2 skipFields advances bitstream identically to readFieldsFast

- **GIVEN** a `CSVCMsg_PacketEntities` UPDATE delta and an `S2FieldReader`
- **WHEN** `skipFields` is invoked at bitstream pos `P` for that update
- **THEN** the bitstream pos after the call SHALL equal the pos that `readFieldsFast` would have produced for the same input, and no `EntityState` write SHALL have occurred during the call.

#### Scenario: S1 skipFields advances bitstream identically to readFields

- **GIVEN** an S1 entity update delta and an `S1FieldReader` implementation (`CsgoFieldReader` or `DotaS1FieldReader`)
- **WHEN** `skipFields` is invoked at bitstream pos `P` for that update
- **THEN** the bitstream pos after the call SHALL equal the pos that `readFields` (no-debug, no-onMutation) would have produced for the same input, and no `EntityState` write SHALL have occurred during the call.

#### Scenario: Default fallback for unspecialized FieldReader

- **WHEN** `skipFields` is invoked on a `FieldReader` implementation that has not overridden it
- **THEN** the default implementation SHALL delegate to `readFields(bs, dtClass, state, false, null)` and discard the returned `FieldChanges`. Behavior SHALL be correct (bitstream advances by the right amount) but SHALL NOT realize the performance benefit of a specialized skip path.

### Requirement: Filter-rejected entities are absent from queries

For every consumer-visible entity-collection query, an entity_id whose class was filtered at CREATE time SHALL be indistinguishable from an entity_id that was never transmitted on the wire.

#### Scenario: getById returns null

- **WHEN** the consumer calls `entities.getById(id)` for an id whose class was filtered
- **THEN** the call SHALL return `null`.

#### Scenario: Predicate queries do not include filtered entities

- **WHEN** the consumer calls `entities.getByPredicate(predicate)` or any other collection-walking API
- **THEN** filtered entities SHALL NOT appear in the returned collection, regardless of whether the predicate would match them.

#### Scenario: No CREATE/UPDATE/DELETE events for filtered entities

- **WHEN** the wire stream contains CREATE, UPDATE, or DELETE messages for an entity_id whose class was filtered at CREATE
- **THEN** no `@OnEntityCreated`, `@OnEntityUpdated`, `@OnEntityDeleted`, `@OnEntityPropertyChanged`, or related listener SHALL fire for that entity, even if a listener's class pattern would otherwise have matched.
