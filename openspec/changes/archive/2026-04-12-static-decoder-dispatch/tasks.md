## 1. Annotation Processor Modularization

- [x] 1.1 Split `EventAnnotationProcessor` Task A into `ListenerValidationProcessor` (listener signature validation)
- [x] 1.2 Split `EventAnnotationProcessor` Task B into `ProvidesIndexProcessor` (writes `providers.txt`)
- [x] 1.3 Split `EventAnnotationProcessor` Task C into `EventGenerationProcessor` (generates `*_Event` classes with JavaPoet)
- [x] 1.4 Remove the monolithic `EventAnnotationProcessor` class
- [x] 1.5 Update `META-INF/services/javax.annotation.processing.Processor` to list the three new processors
- [x] 1.6 Build and verify all existing event generation, listener validation, and provides indexing still works

## 2. Decoder Abstract Class and Annotation Processor

- [x] 2.1 Replace the existing `Decoder<T>` interface file with the handwritten `Decoder` abstract class (same file path in `skadistats.clarity.io.decoder`). The class has: `protected final int id` field, package-private `static void register(Class, int)` method, static initializer loading `DecoderIds` via `Class.forName`, no-arg `protected` constructor that sets `id` from the map. Not sealed, not generated. This intentionally breaks compilation — subsequent tasks fix all references.
- [x] 2.2 Create `@RegisterDecoder` annotation in `skadistats.clarity.io.decoder`
- [x] 2.3 Create `DecoderAnnotationProcessor` that scans `@RegisterDecoder` classes and generates `DecoderIds` (int constants + COUNT + static initializer calling `Decoder.register` for each decoder) and `DecoderDispatch` (tableswitch static dispatch) with JavaPoet
- [x] 2.4 Add validation: compile error if `@RegisterDecoder` class does not extend handwritten `Decoder`
- [x] 2.5 Register `DecoderAnnotationProcessor` in `META-INF/services`
- [x] 2.6 Build and verify the processor generates correct source files in `build/generated/`

## 3. Migrate Decoder Classes

- [x] 3.1 Migrate stateless decoders (BoolDecoder, IntVarUnsignedDecoder, IntVarSignedDecoder, IntMinusOneDecoder, LongVarUnsignedDecoder, LongVarSignedDecoder, FloatNoScaleDecoder, FloatNormalDecoder, FloatCoordDecoder, VectorNormalDecoder, QAngleNoBitCountDecoder, QAnglePreciseDecoder, QAngleNoScaleDecoder, StringZeroTerminatedDecoder, StringLenDecoder): add `@RegisterDecoder`, `final`, `extends Decoder`, convert `decode()` to `public static T decode(BitStream bs)` (no decoder parameter — stateless). Remove `implements Decoder<T>`.
- [x] 3.2 Migrate simple stateful decoders (IntUnsignedDecoder, IntSignedDecoder, LongUnsignedDecoder, LongSignedDecoder, FloatDefaultDecoder, FloatCellCoordDecoder, FloatCoordMpDecoder, QAngleBitCountDecoder, QAnglePitchYawOnlyDecoder): add `@RegisterDecoder`, `final`, `extends Decoder`, convert `decode()` to `public static T decode(BitStream bs, XDecoder d)` with concrete decoder type as second parameter. Remove `implements Decoder<T>`.
- [x] 3.3 Migrate `FloatQuantizedDecoder`: add `@RegisterDecoder`, `final`, `extends Decoder`, convert `decode()` to static with `d` parameter. Constructor and init logic unchanged. Remove `implements Decoder<T>`.
- [x] 3.4 Migrate `PointerDecoder`: add `@RegisterDecoder`, `final`, `extends Decoder`, convert `decode()` to static with `d` parameter. Remove `implements Decoder<T>`.
- [x] 3.5 Migrate compound decoders (VectorDecoder, VectorDefaultDecoder, VectorXYDecoder, ArrayDecoder): add `@RegisterDecoder`, `final`, `extends Decoder`, convert `decode()` to static with `d` parameter. Change inner decoder field type from `Decoder<T>` to `Decoder`, inner decode calls use `DecoderDispatch.decode(bs, d.innerDecoder)`. `ArrayDecoder` loses its type parameter (becomes non-generic). Remove `implements Decoder<T>`.
- [x] 3.6 Build and verify the generated `DecoderDispatch` switch compiles with all decoder cases

## 4. Rename DecoderProperties → SerializerProperties

- [x] 4.1 Rename `DecoderProperties` interface to `SerializerProperties` (same package `skadistats.clarity.io.s2`)
- [x] 4.2 Rename `ProtoDecoderProperties` to `ProtoSerializerProperties` (in `skadistats.clarity.processor.sendtables`)
- [x] 4.3 Update all references: `S2DecoderFactory`, `FieldGenerator`, `Field` subclasses, `S2FieldReader`, per-type S2 factory classes, and any other usages

## 5. Migrate S2 Decode Path

- [x] 5.1 Change `S2DecoderFactory`: return `Decoder` instead of `DecoderHolder`. Update `DECODERS` map to hold `Decoder` instances directly. Update `FACTORIES` map type to `Function<SerializerProperties, Decoder>`, convert per-type factory classes to static method references (note: `VectorDecoderFactory` needs lambdas capturing `dim`, not simple method references)
- [x] 5.2 Change S2 per-type factories (`FloatDecoderFactory`, `QAngleDecoderFactory`, `VectorDecoderFactory`, `LongUnsignedDecoderFactory`, `PointerFactory`): return `Decoder` instead of `Decoder<T>`, make `createDecoder` static, remove `implements DecoderFactory<T>`
- [x] 5.3 Change `Field` base class: `getDecoder()` returns `Decoder` instead of `Decoder<?>`, `getSerializerProperties()` returns `SerializerProperties` (renamed from `getDecoderProperties()`)
- [x] 5.4 Change `ValueField`, `PointerField`: store `Decoder` and `SerializerProperties` as separate fields instead of `DecoderHolder`. Change `VectorField`: replace static `DecoderHolder` with `private static final IntVarUnsignedDecoder LENGTH_DECODER = new IntVarUnsignedDecoder()`, return from `getDecoder()`; `getSerializerProperties()` returns `SerializerProperties.DEFAULT` (length decoder is always uint32, never configurable)
- [x] 5.5 Change `S2DTClass.getDecoderForFieldPath()` to return `Decoder`
- [x] 5.6 Change `S2FieldReader.readFieldsFast()`: call `DecoderDispatch.decode(bs, decoder)` instead of `decoder.decode(bs)`. Change `readFieldsDebug()`: same dispatch change, update `getDecoderProperties()` call to `getSerializerProperties()`
- [x] 5.7 Change `FieldGenerator.createField()`: receive `Decoder` from factory calls, pass `SerializerProperties` separately to Field constructors
- [x] 5.8 Remove `DecoderHolder` class

## 6. Migrate S1 Decode Path

- [x] 6.1 Change `S1DecoderFactory`: return `Decoder` instead of raw `Decoder<T>`. Update `FACTORIES` map type to `Function<SendProp, Decoder>`, convert per-type factory classes to static method references (note: `VectorDecoderFactory` needs lambdas capturing `dim`)
- [x] 6.2 Change S1 per-type factories (`IntDecoderFactory`, `LongDecoderFactory`, `FloatDecoderFactory`, `VectorDecoderFactory`, `ArrayDecoderFactory`): return `Decoder`, make `createDecoder` static, remove `implements DecoderFactory`
- [x] 6.3 Change `SendProp`: store `Decoder` instead of raw `Decoder<T>` reference
- [x] 6.4 Change `ReceiveProp.decode()`: call `DecoderDispatch.decode(bs, decoder)`
- [x] 6.5 Update `S1FieldReader.readFields()` debug path to get decoder info from `Decoder` (e.g. `decoder.getClass().getSimpleName()`)

## 7. Cleanup

- [x] 7.1 Remove S2 `DecoderFactory<T>` interface from `factory/s2/`
- [x] 7.2 Remove S1 `DecoderFactory` interface from `factory/s1/`
- [x] 7.3 Full build and verify all examples in clarity-examples run against a real replay
