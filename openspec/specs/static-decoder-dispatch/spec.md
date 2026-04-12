## Requirements

### Requirement: Decoder classes extend Decoder
Each `@RegisterDecoder`-annotated decoder class SHALL be `final` and extend the handwritten `Decoder` abstract class. The class SHALL keep its existing fields, constructors, and initialization logic unchanged.

#### Scenario: Stateless decoder
- **WHEN** `BoolDecoder` has no constructor parameters
- **THEN** it SHALL be `public final class BoolDecoder extends Decoder`

#### Scenario: Stateful decoder
- **WHEN** `FloatQuantizedDecoder` has constructor parameters and init logic
- **THEN** it SHALL be `public final class FloatQuantizedDecoder extends Decoder` with constructor and init logic unchanged

### Requirement: Decoder classes provide static decode methods
Each `@RegisterDecoder`-annotated decoder class SHALL provide a `public static` decode method. Stateless decoders SHALL have signature `decode(BitStream bs)`. Stateful decoders SHALL have signature `decode(BitStream bs, XDecoder d)` where `XDecoder` is the concrete decoder class. The second parameter SHALL be named `d`.

#### Scenario: Stateless decoder
- **WHEN** `BoolDecoder` has no instance state needed for decoding
- **THEN** it SHALL provide `public static Boolean decode(BitStream bs)` that returns `bs.readBitFlag()`

#### Scenario: Stateful decoder
- **WHEN** `FloatQuantizedDecoder` requires pre-computed parameters (bitCount, encodeFlags, minValue, maxValue, decodeMultiplier)
- **THEN** it SHALL provide `public static Float decode(BitStream bs, FloatQuantizedDecoder d)` that reads from `d`

### Requirement: Automatic ID assignment
Decoder classes SHALL NOT reference generated code. The int ID is assigned automatically by the `Decoder` base class constructor, which looks up the class in a map populated by the generated `DecoderIds` static initializer. Decoder constructors require no changes for ID assignment.

#### Scenario: Stateless decoder
- **WHEN** `BoolDecoder` extends `Decoder` and is annotated with `@RegisterDecoder`
- **THEN** its constructor (implicit or explicit) SHALL call `super()` and receive its ID automatically

#### Scenario: Stateful decoder
- **WHEN** `FloatQuantizedDecoder` extends `Decoder` and is annotated with `@RegisterDecoder`
- **THEN** its constructor SHALL call `super()` (explicitly or implicitly) and receive its ID automatically, then initialize its fields

### Requirement: Nested decoder composition via Decoder
Compound decoders (VectorDefaultDecoder, VectorDecoder, VectorXYDecoder, ArrayDecoder) SHALL store the inner decoder as a `Decoder` field. Their static decode method SHALL call `DecoderDispatch.decode(bs, d.innerDecoder)` for inner decoding. `ArrayDecoder` loses its type parameter and becomes non-generic.

#### Scenario: VectorDefaultDecoder composes float decoder
- **WHEN** `VectorDefaultDecoder` is created with `dim=3` and an inner `FloatNoScaleDecoder`
- **THEN** `VectorDefaultDecoder.decode(bs, d)` SHALL call `DecoderDispatch.decode(bs, d.innerDecoder)` three times and return a `Vector`

#### Scenario: VectorDecoder with normal flag
- **WHEN** `VectorDecoder` has `normal=true`
- **THEN** `VectorDecoder.decode(bs, d)` SHALL decode only X and Y via `DecoderDispatch.decode(bs, d.floatDecoder)`, then compute Z from the normal

#### Scenario: ArrayDecoder becomes non-generic
- **WHEN** `ArrayDecoder` (formerly `ArrayDecoder<T>`) decodes an array
- **THEN** it SHALL call `DecoderDispatch.decode(bs, d.decoder)` for each element and return `Object`

### Requirement: S2FieldReader uses DecoderDispatch
`S2FieldReader.readFieldsFast()` SHALL call `DecoderDispatch.decode(bs, decoder)` instead of `decoder.decode(bs)`. The `Decoder` SHALL be obtained from the field via the DTClass.

#### Scenario: S2 hot path dispatch
- **WHEN** `readFieldsFast()` processes a field path
- **THEN** it SHALL retrieve the `Decoder` for that field path and call `DecoderDispatch.decode(bs, decoder)`
- **THEN** no `Decoder<T>` interface call SHALL occur in the decode loop

### Requirement: S1 decode path uses DecoderDispatch
`ReceiveProp.decode()` SHALL call `DecoderDispatch.decode(bs, decoder)` instead of `sendProp.getDecoder().decode(bs)`. `SendProp` SHALL store the `Decoder` class instead of the old `Decoder<T>` interface.

#### Scenario: S1 hot path dispatch
- **WHEN** `S1FieldReader.readFields()` processes a receive prop
- **THEN** the decode call SHALL go through `DecoderDispatch.decode(bs, decoder)` with no `Decoder<T>` interface call

### Requirement: S2 Field hierarchy stores Decoder and SerializerProperties
`ValueField`, `VectorField`, and `PointerField` SHALL store `Decoder` and `SerializerProperties` as separate fields instead of `DecoderHolder`. The `Field` base class SHALL provide `getDecoder()` returning `Decoder` (default `null`) and `getSerializerProperties()` returning `SerializerProperties` (default `SerializerProperties.DEFAULT`). `VectorField` SHALL use `SerializerProperties.DEFAULT` (the existing default instance, renamed from `DecoderProperties.DEFAULT`).

#### Scenario: ValueField stores Decoder and SerializerProperties
- **WHEN** a `ValueField` is constructed
- **THEN** it SHALL accept and store a `Decoder` instance and a `SerializerProperties` instance as separate fields

### Requirement: S2DecoderFactory returns Decoder with lambda factories
`S2DecoderFactory` SHALL return `Decoder` instead of `DecoderHolder`. The static `DECODERS` map SHALL contain decoder instances directly (as `Decoder` singletons). The `FACTORIES` map SHALL use `Function<SerializerProperties, Decoder>` as value type, with per-type factory classes providing static method references.

#### Scenario: Static decoder lookup
- **WHEN** `S2DecoderFactory` is asked for type `"bool"`
- **THEN** it SHALL return a `BoolDecoder` singleton (which is a `Decoder`)

#### Scenario: Factory-based decoder creation
- **WHEN** `S2DecoderFactory` is asked for type `"float32"` with properties having `bitCount=12`, `lowValue=0`, `highValue=100`
- **THEN** `FloatDecoderFactory.createDecoder(properties)` SHALL return a `FloatQuantizedDecoder` instance (which is a `Decoder`)

### Requirement: S1DecoderFactory returns Decoder with lambda factories
`S1DecoderFactory.createDecoder()` SHALL return the `Decoder` class instead of the old `Decoder<T>` interface. The `FACTORIES` map SHALL use `Function<SendProp, Decoder>` as value type. The `DECODERS` map SHALL contain decoder instances directly as `Decoder`.

#### Scenario: S1 factory returns Decoder
- **WHEN** `S1DecoderFactory` is asked for `PropType.INT` with a `SendProp`
- **THEN** `IntDecoderFactory.createDecoder(sendProp)` SHALL return the appropriate decoder instance as `Decoder`

### Requirement: DecoderHolder removed
The `DecoderHolder` class SHALL be removed. All references to `DecoderHolder` SHALL be replaced with direct `Decoder` usage.

#### Scenario: No DecoderHolder references remain
- **WHEN** the change is complete
- **THEN** no source file SHALL import or reference `DecoderHolder`

### Requirement: Decoder interface removed
The `Decoder<T>` interface SHALL be removed. All decoder classes SHALL no longer implement it. All instance `decode()` methods SHALL be removed, replaced by static decode methods.

#### Scenario: No Decoder interface references remain
- **WHEN** the change is complete
- **THEN** no source file SHALL import or reference the `Decoder<T>` interface
- **THEN** all decode dispatch SHALL go through `DecoderDispatch`

### Requirement: Factory interfaces removed
The `DecoderFactory<T>` interface (S2, in `factory/s2/`) and `DecoderFactory` interface (S1, in `factory/s1/`) SHALL be removed. Per-type factory classes SHALL retain their static `createDecoder` methods but no longer implement a shared interface.

#### Scenario: No factory interface references remain
- **WHEN** the change is complete
- **THEN** no source file SHALL import or reference `DecoderFactory<T>` or `DecoderFactory` interfaces

### Requirement: DecoderProperties renamed to SerializerProperties
The `DecoderProperties` interface SHALL be renamed to `SerializerProperties`. The `ProtoDecoderProperties` class SHALL be renamed to `ProtoSerializerProperties`. All references throughout the codebase SHALL be updated. The interface and implementation remain otherwise unchanged — they continue to serve as init-time parameter bundles from serializer parsing to decoder factories.

#### Scenario: SerializerProperties still used by factories
- **WHEN** `S2DecoderFactory` creates a decoder
- **THEN** it SHALL accept `SerializerProperties` as input for factory-based decoder creation

#### Scenario: No DecoderProperties references remain
- **WHEN** the change is complete
- **THEN** no source file SHALL import or reference `DecoderProperties` or `ProtoDecoderProperties`
