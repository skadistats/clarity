## MODIFIED Requirements

### Requirement: Decoder classes extend Decoder
Each `@RegisterDecoder`-annotated decoder class SHALL be `final` and extend the handwritten `Decoder` abstract class. `Decoder` itself SHALL remain non-sealed — the `@RegisterDecoder` annotation set is the authoritative enumeration of concrete decoders; the annotation processor's generated `DecoderDispatch` switch covers every member of that set by construction, so a `permits` clause would be redundant and would degrade the "drop a file, rebuild" ergonomic. Each concrete decoder SHALL keep its existing fields, constructors, and initialization logic unchanged (save for the added `final` modifier).

#### Scenario: Stateless decoder
- **WHEN** `BoolDecoder` has no constructor parameters
- **THEN** it SHALL be `public final class BoolDecoder extends Decoder`

#### Scenario: Stateful decoder
- **WHEN** `FloatQuantizedDecoder` has constructor parameters and init logic
- **THEN** it SHALL be `public final class FloatQuantizedDecoder extends Decoder` with constructor and init logic unchanged

## REMOVED Requirements

### Requirement: Automatic ID assignment
**Reason**: `Decoder.id` is removed. Dispatch now runs via type-pattern switch on `Decoder`; no synthetic integer discriminator is required. The `IDS` static map, the `Decoder.register` method, the `Class.forName("DecoderIds")` bootstrap in `Decoder`'s static initializer, and the generated `DecoderIds` class are all deleted together with the field.

**Migration**: No downstream migration is required — `Decoder.id` was `protected` and not read outside `DecoderDispatch`. Internal dispatch is rewritten as a type-pattern `switch (d) { case XDecoder x -> …; default -> throw … }`; pattern variables replace the per-case cast.

## MODIFIED Requirements

### Requirement: S2FieldReader uses DecoderDispatch
`S2FieldReader.readFieldsFast()` SHALL call `DecoderDispatch.decode(bs, decoder)` instead of `decoder.decode(bs)`. The `Decoder` SHALL be obtained from the field via the DTClass. `DecoderDispatch.decode` internally dispatches via a type-pattern switch on `Decoder` — the caller SHALL NOT see this change.

#### Scenario: S2 hot path dispatch
- **WHEN** `readFieldsFast()` processes a field path
- **THEN** it SHALL retrieve the `Decoder` for that field path and call `DecoderDispatch.decode(bs, decoder)`
- **THEN** no `Decoder<T>` interface call SHALL occur in the decode loop
- **AND** the dispatch inside `DecoderDispatch.decode` SHALL use a type-pattern switch generated from the `@RegisterDecoder` set

### Requirement: DecoderDispatch.decodeInto provides static dispatch
`DecoderDispatch` SHALL provide a `public static void decodeInto(BitStream bs, Decoder decoder, byte[] data, int offset)` method that dispatches to the corresponding concrete decoder's static `decodeInto` via a type-pattern switch on `Decoder`.

Only primitive decoders (those whose `getPrimitiveType()` returns non-null) and the inline-string decoders SHALL appear as explicit case arms. Reference-producing decoders without a `decodeInto` path SHALL NOT appear as case arms and SHALL fall through to a terminal `default -> throw new IllegalStateException(...)` — callers must pre-filter using `decoder.getPrimitiveType() != null` (or the equivalent inline-string predicate) before invoking `decodeInto`.

#### Scenario: Primitive decoder dispatch
- **WHEN** `DecoderDispatch.decodeInto(bs, decoder, data, offset)` is called and `decoder` is a primitive decoder
- **THEN** the pattern switch routes to the concrete decoder's static `decodeInto`
- **AND** the call is resolved via the `typeSwitch` indy with no virtual dispatch and no cast inside the case

#### Scenario: Non-primitive decoder throws
- **WHEN** `DecoderDispatch.decodeInto(bs, decoder, data, offset)` is called and `decoder` is a reference-producing decoder with no `decodeInto` path
- **THEN** `IllegalStateException` is thrown from the `default` arm
- **AND** the caller is expected to have used `decoder.getPrimitiveType() != null || isInlineStringDecoder(decoder)` as a routing predicate
