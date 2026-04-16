## ADDED Requirements

### Requirement: DecoderAnnotationProcessor generates DecoderDispatch.decodeInto

The processor SHALL generate a `public static void decodeInto(BitStream bs, Decoder d, byte[] data, int offset)` method on `DecoderDispatch` alongside `decode`. The method body SHALL be a `switch(d.id)` statement with one case per `@RegisterDecoder` class that declares an eligible `decodeInto` static method. Decoders without a `decodeInto` method SHALL fall through to the default case, which throws `IllegalArgumentException("Decoder id <id> has no decodeInto")`.

`decodeInto` writes the decoded primitive (or composite-of-primitives) directly to the given `byte[]` at the given `offset` via `PrimitiveType.*_VH.set(...)` or direct byte writes. It performs no boxing and no intermediate object allocation.

#### Scenario: Decoder declares decodeInto

- **WHEN** `IntSignedDecoder` declares `public static void decodeInto(BitStream bs, IntSignedDecoder d, byte[] data, int offset)`
- **THEN** the generated switch case SHALL be `case DecoderIds.INT_SIGNED -> IntSignedDecoder.decodeInto(bs, (IntSignedDecoder) d, data, offset);`

#### Scenario: Stateless decoder with decodeInto

- **WHEN** `BoolDecoder` declares `public static void decodeInto(BitStream bs, byte[] data, int offset)` (no decoder-instance parameter)
- **THEN** the generated switch case SHALL be `case DecoderIds.BOOL -> BoolDecoder.decodeInto(bs, data, offset);`

#### Scenario: Decoder has no decodeInto

- **WHEN** `ArrayDecoder` declares only `decode` (no `decodeInto`)
- **THEN** the generated switch SHALL NOT include a case for `DecoderIds.ARRAY` under `decodeInto`
- **AND** calling `DecoderDispatch.decodeInto(bs, arrayDecoder, data, offset)` SHALL throw `IllegalArgumentException`

#### Scenario: Dispatch method signature is void

- **WHEN** `DecoderDispatch.decodeInto` is generated
- **THEN** its return type SHALL be `void` (callers mutate the provided `byte[]`)
- **AND** there SHALL be no `return` statement before the final default case

### Requirement: decodeInto validation

The processor SHALL emit compile errors for the following violations on `decodeInto` methods of `@RegisterDecoder`-annotated classes:

1. More than one `public static` method named `decodeInto`
2. The method has fewer than 3 or more than 4 parameters
3. The first parameter is not `BitStream`
4. The penultimate parameter is not `byte[]`
5. The last parameter is not `int`
6. For 4-parameter methods, the second parameter type does not match the declaring class

A missing `decodeInto` method SHALL NOT be a compile error — the decoder is simply not eligible for direct-write dispatch and falls through to the default throw.

#### Scenario: Absent decodeInto is allowed

- **WHEN** a `@RegisterDecoder` class declares `decode` but not `decodeInto`
- **THEN** the processor SHALL NOT emit an error
- **AND** the decoder SHALL be registered in `DecoderIds` and `DecoderDispatch.decode` as normal

#### Scenario: Wrong byte[] parameter

- **WHEN** a decoder declares `decodeInto(BitStream bs, int offset, int len)` (no byte[] parameter)
- **THEN** the processor SHALL emit a compile error indicating the penultimate parameter must be `byte[]`

### Requirement: Primitive decoders implement decodeInto

Each `@RegisterDecoder` decoder whose `getPrimitiveType()` returns non-null (i.e. scalar INT/LONG/FLOAT/BOOL or a `VectorType`) SHALL declare a `decodeInto` method. This covers:

- Int decoders: `IntSignedDecoder`, `IntUnsignedDecoder`, `IntMinusOneDecoder`, `IntVarSignedDecoder`, `IntVarUnsignedDecoder`
- Long decoders: `LongSignedDecoder`, `LongUnsignedDecoder`, `LongVarSignedDecoder`, `LongVarUnsignedDecoder`
- Float decoders: `FloatNoScaleDecoder`, `FloatCoordDecoder`, `FloatCoordMpDecoder`, `FloatCellCoordDecoder`, `FloatNormalDecoder`, `FloatDefaultDecoder`, `FloatQuantizedDecoder`
- Bool: `BoolDecoder`
- Vector: `VectorDecoder`, `VectorDefaultDecoder`, `VectorNormalDecoder`, `VectorXYDecoder`
- QAngle: `QAngleBitCountDecoder`, `QAngleNoBitCountDecoder`, `QAngleNoScaleDecoder`, `QAnglePitchYawOnlyDecoder`, `QAnglePreciseDecoder`

`ArrayDecoder`, `PointerDecoder`, `StringLenDecoder`, and `StringZeroTerminatedDecoder` SHALL NOT declare `decodeInto` in this change. `StringLenDecoder` gains `decodeInto` in a later change (inline strings). `ArrayDecoder` remains on the boxing path (future optimization per audit 0.6).

#### Scenario: All primitive decoders covered

- **WHEN** the project is compiled
- **THEN** `DecoderDispatch.decodeInto` SHALL contain cases for at least the 26 decoders listed above
- **AND** calling `decodeInto` with any of them SHALL produce the same byte layout as `PrimitiveType.write(data, offset, DecoderDispatch.decode(bs, d))` on a parallel bitstream

### Requirement: decodeInto byte-for-byte parity with decode + PrimitiveType.write

For every decoder that declares `decodeInto`, running `decodeInto(bs, d, buf, 0)` on one bitstream SHALL produce the same `buf` contents as running `PrimitiveType.write(buf, 0, decode(bs))` on an identical bitstream, where `PrimitiveType` is chosen to match `d.getPrimitiveType()`. Both paths SHALL consume the same number of bits from the bitstream.

#### Scenario: Parity check on all primitive decoders

- **WHEN** the parity test suite runs
- **THEN** every decoder in the primitive decoder list SHALL pass byte-for-byte equality AND bit-consumption equality
