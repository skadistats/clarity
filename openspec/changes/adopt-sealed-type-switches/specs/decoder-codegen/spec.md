## MODIFIED Requirements

### Requirement: Handwritten Decoder abstract class
The `Decoder` abstract class in package `skadistats.clarity.io.decoder` SHALL be handwritten (not generated). It SHALL NOT be sealed — the `@RegisterDecoder` annotation set is the authoritative list of concrete decoders, and the annotation processor's generated switch provides coverage-by-construction; adding a `permits` clause would duplicate that contract and force every new decoder to edit `Decoder.java`. It SHALL NOT declare any abstract methods. It SHALL have:
- No `id` field
- No `register` / `IDS` / `Class.forName("DecoderIds")` machinery
- A `protected` no-arg constructor with an empty body

Decoder subclasses SHALL be `public final class XDecoder extends Decoder`. Extending `Decoder` without `@RegisterDecoder` is a misuse; the annotation processor SHALL emit a compile warning or error if such a class is detected in the compilation unit (implementation detail — either behaviour is acceptable).

#### Scenario: Decoder class exists in source
- **WHEN** the project is compiled
- **THEN** `Decoder` SHALL already exist as a source file, not as generated output
- **AND** `Decoder` SHALL be declared `public abstract class Decoder` (no `sealed`, no `permits` clause)

### Requirement: DecoderAnnotationProcessor generates DecoderDispatch
The processor SHALL generate a `DecoderDispatch` class in package `skadistats.clarity.io.decoder` with a `public static Object decode(BitStream bs, Decoder d)` method. The method body SHALL be a type-pattern `switch (d)` with one `case XDecoder x -> …` arm per `@RegisterDecoder` class and a terminal `default -> throw new IllegalStateException("Unknown decoder: " + d.getClass())` arm. The `default` arm is required because `Decoder` is not sealed and `javac` therefore cannot prove exhaustiveness; it is unreachable at runtime so long as every concrete `Decoder` subclass carries `@RegisterDecoder`.

#### Scenario: Switch dispatches to stateless static decode method
- **WHEN** `DecoderDispatch.decode(bs, d)` is called with `d` being a `BoolDecoder`
- **THEN** the switch SHALL match `case BoolDecoder b -> BoolDecoder.decode(bs)`

#### Scenario: Stateful decoder receives typed instance
- **WHEN** `DecoderDispatch.decode(bs, d)` is called with `d` being a `FloatQuantizedDecoder`
- **THEN** the switch SHALL match `case FloatQuantizedDecoder x -> FloatQuantizedDecoder.decode(bs, x)` with no explicit cast — the pattern variable `x` has the concrete static type

#### Scenario: Unregistered subclass hits default
- **WHEN** a subclass of `Decoder` is passed to `DecoderDispatch.decode` without carrying `@RegisterDecoder` (i.e. misuse)
- **THEN** the `default` arm SHALL throw `IllegalStateException`

### Requirement: Stateless vs stateful dispatch detection
The processor SHALL inspect the static `decode` method of each `@RegisterDecoder`-annotated class to determine whether it takes a second parameter (the decoder instance). If the method has only `BitStream` as parameter, the generated pattern arm SHALL call it ignoring the pattern variable. If it has a second parameter typed as the decoder class, the pattern arm SHALL pass the pattern variable directly (no cast needed).

#### Scenario: Stateless decoder dispatch
- **WHEN** `BoolDecoder` has `public static Boolean decode(BitStream bs)`
- **THEN** the generated case SHALL be `case BoolDecoder b -> BoolDecoder.decode(bs);`

#### Scenario: Stateful decoder dispatch
- **WHEN** `FloatQuantizedDecoder` has `public static Float decode(BitStream bs, FloatQuantizedDecoder d)`
- **THEN** the generated case SHALL be `case FloatQuantizedDecoder x -> FloatQuantizedDecoder.decode(bs, x);`

### Requirement: DecoderAnnotationProcessor generates DecoderDispatch.decodeInto

The processor SHALL generate a `public static void decodeInto(BitStream bs, Decoder d, byte[] data, int offset)` method on `DecoderDispatch` alongside `decode`. The method body SHALL be a type-pattern `switch (d)` with one `case XDecoder x -> XDecoder.decodeInto(bs, x, data, offset)` (or the stateless variant) arm per `@RegisterDecoder` class that declares an eligible `decodeInto` static method, plus a terminal `default -> throw new IllegalStateException("Decoder " + d.getClass().getSimpleName() + " has no decodeInto")` arm.

Decoders without a `decodeInto` method SHALL NOT appear as explicit cases — their dispatch falls to the `default` arm and throws, matching today's behaviour for non-eligible decoders.

`decodeInto` writes the decoded primitive (or composite-of-primitives) directly to the given `byte[]` at the given `offset` via `PrimitiveType.*_VH.set(...)` or direct byte writes. It performs no boxing and no intermediate object allocation.

#### Scenario: Decoder declares decodeInto

- **WHEN** `IntSignedDecoder` declares `public static void decodeInto(BitStream bs, IntSignedDecoder d, byte[] data, int offset)`
- **THEN** the generated switch case SHALL be `case IntSignedDecoder x -> IntSignedDecoder.decodeInto(bs, x, data, offset);`

#### Scenario: Stateless decoder with decodeInto

- **WHEN** `BoolDecoder` declares `public static void decodeInto(BitStream bs, byte[] data, int offset)` (no decoder-instance parameter)
- **THEN** the generated switch case SHALL be `case BoolDecoder b -> BoolDecoder.decodeInto(bs, data, offset);`

#### Scenario: Decoder has no decodeInto

- **WHEN** `ArrayDecoder` declares only `decode` (no `decodeInto`)
- **THEN** the generated switch SHALL NOT include an explicit case arm for `ArrayDecoder` under `decodeInto`
- **AND** calling `DecoderDispatch.decodeInto(bs, arrayDecoder, data, offset)` SHALL hit the `default` arm and throw `IllegalStateException`

#### Scenario: Dispatch method signature is void

- **WHEN** `DecoderDispatch.decodeInto` is generated
- **THEN** its return type SHALL be `void` (callers mutate the provided `byte[]`)
- **AND** there SHALL be no `return` statement

## REMOVED Requirements

### Requirement: DecoderAnnotationProcessor generates DecoderIds
**Reason**: The generated `DecoderIds` class is deleted together with `Decoder.id`, `Decoder.IDS`, and `Decoder.register`. Dispatch runs on `Decoder` via type-pattern switch; no integer discriminator is needed. Coverage is guaranteed by the processor emitting one case per `@RegisterDecoder` class — sealing `Decoder` would merely duplicate that guarantee at the language level.

**Migration**: The processor is retargeted — it no longer emits `DecoderIds`. Consumers of `DecoderIds.*` constants (there are none outside `DecoderDispatch`) have no migration to perform. Build configuration that referenced `DecoderIds` in annotation-processor output paths SHALL be cleaned up at Stage 5.
