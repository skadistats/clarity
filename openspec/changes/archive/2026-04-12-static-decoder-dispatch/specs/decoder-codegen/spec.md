## ADDED Requirements

### Requirement: @RegisterDecoder annotation
The system SHALL provide a `@RegisterDecoder` annotation with `@Target(TYPE)` and `@Retention(SOURCE)` in package `skadistats.clarity.io.decoder`. When placed on a decoder class, it SHALL mark that class for processing by the `DecoderAnnotationProcessor`.

#### Scenario: Annotated class is picked up
- **WHEN** a class is annotated with `@RegisterDecoder` and extends `Decoder`
- **THEN** the `DecoderAnnotationProcessor` SHALL include it in the generated dispatch

#### Scenario: Unannotated decoder class is ignored
- **WHEN** a class extends `Decoder` but is NOT annotated with `@RegisterDecoder`
- **THEN** the processor SHALL NOT include it in the generated dispatch

### Requirement: Handwritten Decoder abstract class
The `Decoder` abstract class in package `skadistats.clarity.io.decoder` SHALL be handwritten (not generated). It SHALL NOT be sealed. It SHALL NOT declare any abstract methods. It SHALL have:
- A `protected final int id` field
- A package-private `static void register(Class<?> clazz, int id)` method that stores the mapping in an internal `Map<Class<?>, Integer>`
- A static initializer that loads the generated `DecoderIds` class via `Class.forName("skadistats.clarity.io.decoder.DecoderIds")`, triggering its static initializer which calls `register` for all decoders. If `Class.forName` fails, the `ExceptionInInitializerError` SHALL propagate — no silent fallback. A missing `DecoderIds` is a build defect, not a recoverable state.
- A `protected` no-arg constructor that sets `this.id = IDS.get(getClass())`

#### Scenario: Decoder class exists in source
- **WHEN** the project is compiled
- **THEN** `Decoder` SHALL already exist as a source file, not as generated output

#### Scenario: ID assignment is automatic
- **WHEN** a new `FloatQuantizedDecoder` is constructed
- **THEN** the `Decoder()` constructor SHALL look up its ID from the internal map populated by `DecoderIds`
- **THEN** the decoder source code SHALL NOT reference any generated class

### Requirement: DecoderAnnotationProcessor generates DecoderIds
The `DecoderAnnotationProcessor` SHALL generate a `DecoderIds` class in package `skadistats.clarity.io.decoder` containing `public static final int` constants, one per `@RegisterDecoder`-annotated class. IDs SHALL be assigned in alphabetical order by simple class name, starting at 0. A `COUNT` constant SHALL hold the total number of decoders. The class SHALL have a static initializer that calls `Decoder.register(XDecoder.class, ID)` for each annotated decoder class.

#### Scenario: ID assignment is alphabetical
- **WHEN** decoder classes `BoolDecoder`, `ArrayDecoder`, and `FloatNoScaleDecoder` are annotated
- **THEN** `DecoderIds` SHALL contain `ARRAY = 0`, `BOOL = 1`, `FLOAT_NO_SCALE = 2`, `COUNT = 3`

#### Scenario: Static initializer registers all decoders
- **WHEN** `DecoderIds` is loaded (triggered by `Decoder`'s `Class.forName` call)
- **THEN** its static initializer SHALL call `Decoder.register(ArrayDecoder.class, 0)`, `Decoder.register(BoolDecoder.class, 1)`, etc.

#### Scenario: ID naming convention
- **WHEN** a class is named `FloatQuantizedDecoder`
- **THEN** its constant SHALL be named `FLOAT_QUANTIZED` (class name with `Decoder` suffix removed, converted to UPPER_SNAKE_CASE)

#### Scenario: New decoder added
- **WHEN** a new `@RegisterDecoder` class is added and the project is rebuilt
- **THEN** `DecoderIds` SHALL be regenerated with all IDs re-assigned alphabetically and all registrations updated

### Requirement: DecoderAnnotationProcessor generates DecoderDispatch
The processor SHALL generate a `DecoderDispatch` class in package `skadistats.clarity.io.decoder` with a `public static Object decode(BitStream bs, Decoder d)` method. The method SHALL contain a `switch(d.id)` statement (field access, no method call) with one case per decoder, each calling the decoder's static `decode` method.

#### Scenario: Switch dispatches to stateless static decode method
- **WHEN** `DecoderDispatch.decode(bs, d)` is called with `d` being a `BoolDecoder`
- **THEN** the switch SHALL match on `DecoderIds.BOOL` and call `BoolDecoder.decode(bs)`

#### Scenario: Stateful decoder receives typed instance
- **WHEN** `DecoderDispatch.decode(bs, d)` is called with `d` being a `FloatQuantizedDecoder`
- **THEN** the switch SHALL cast `d` to `FloatQuantizedDecoder` and call `FloatQuantizedDecoder.decode(bs, (FloatQuantizedDecoder) d)`

#### Scenario: Unknown ID throws
- **WHEN** `d.id` returns a value not matching any generated case
- **THEN** the switch SHALL throw an `IllegalArgumentException`

### Requirement: DecoderAnnotationProcessor validation
The processor SHALL emit compile errors for the following violations on `@RegisterDecoder`-annotated classes:

1. The class does not extend the handwritten `Decoder` class
2. The class does not have exactly one `public static` method named `decode`
3. The `decode` method's first parameter is not `BitStream`
4. The `decode` method has more than 2 parameters
5. The `decode` method has 2 parameters but the second parameter type does not match the declaring class

#### Scenario: Class does not extend Decoder
- **WHEN** a class annotated with `@RegisterDecoder` does not extend `Decoder`
- **THEN** the processor SHALL emit a compile error

#### Scenario: Missing decode method
- **WHEN** a class annotated with `@RegisterDecoder` has no `public static` method named `decode`
- **THEN** the processor SHALL emit a compile error

#### Scenario: Wrong first parameter type
- **WHEN** a `@RegisterDecoder` class has `public static Object decode(int x)`
- **THEN** the processor SHALL emit a compile error indicating the first parameter must be `BitStream`

#### Scenario: Too many parameters
- **WHEN** a `@RegisterDecoder` class has `public static Object decode(BitStream bs, FooDecoder d, int extra)`
- **THEN** the processor SHALL emit a compile error indicating 1 or 2 parameters expected

#### Scenario: Wrong second parameter type
- **WHEN** a `@RegisterDecoder` class `FooDecoder` has `public static Object decode(BitStream bs, BarDecoder d)`
- **THEN** the processor SHALL emit a compile error indicating the second parameter must match the declaring class

### Requirement: Stateless vs stateful dispatch detection
The processor SHALL inspect the static `decode` method of each `@RegisterDecoder`-annotated class to determine whether it takes a second parameter (the decoder instance). If the method has only `BitStream` as parameter, the generated switch case SHALL call it without casting. If it has a second parameter typed as the decoder class, the switch case SHALL cast `d` and pass it.

#### Scenario: Stateless decoder dispatch
- **WHEN** `BoolDecoder` has `public static Boolean decode(BitStream bs)`
- **THEN** the generated case SHALL be `case DecoderIds.BOOL -> BoolDecoder.decode(bs);`

#### Scenario: Stateful decoder dispatch
- **WHEN** `FloatQuantizedDecoder` has `public static Float decode(BitStream bs, FloatQuantizedDecoder d)`
- **THEN** the generated case SHALL be `case DecoderIds.FLOAT_QUANTIZED -> FloatQuantizedDecoder.decode(bs, (FloatQuantizedDecoder) d);`

### Requirement: DecoderAnnotationProcessor is independently registered
The `DecoderAnnotationProcessor` SHALL be registered as a separate entry in `META-INF/services/javax.annotation.processing.Processor`, independent of other processors.

#### Scenario: Processor registration
- **WHEN** the project is compiled
- **THEN** `META-INF/services/javax.annotation.processing.Processor` SHALL list `DecoderAnnotationProcessor` as a separate line
