## Why

The `decoder.decode(bs)` call in `S2FieldReader.readFieldsFast()` is a megamorphic call site — ~30 different `Decoder<T>` implementations dispatch through a single interface method in the hottest loop of the parser. The JVM cannot inline or speculate on the target, causing significant overhead. The same pattern was already fixed for FieldOps (switch on int constants). Decoders are the remaining megamorphic bottleneck.

## What Changes

- **Decoder classes become their own parameter carriers** — each extends a handwritten `Decoder` abstract class, adds `@RegisterDecoder`, and provides a `public static` decode method. The int ID is assigned automatically at class-loading time via a generated registry — decoder source code never references generated code. Existing fields and init logic stay unchanged.
- **Generated dispatch** — A new `DecoderAnnotationProcessor` (using JavaPoet) scans `@RegisterDecoder`-annotated classes and generates:
  - `DecoderIds` — int constants for each decoder type
  - `DecoderDispatch` — tableswitch-based static dispatch method
- **Annotation Processor modularization** — The existing `EventAnnotationProcessor` (3 tasks in one class) is split into separate processors: `ListenerValidationProcessor`, `ProvidesIndexProcessor`, `EventGenerationProcessor`, plus the new `DecoderAnnotationProcessor`.
- **`Decoder<T>` interface removed entirely** — Replaced by `extends Decoder` + static decode methods. No instance decode methods remain.
- **`DecoderHolder` removed** — Fields store `Decoder` and `SerializerProperties` directly as separate fields (which were previously bundled in `DecoderHolder`).
- **`DecoderProperties` renamed to `SerializerProperties`** — `DecoderProperties` and `ProtoDecoderProperties` renamed to `SerializerProperties` and `ProtoSerializerProperties`. The data describes serializer field metadata (from the protobuf), not decoder properties — the new name reflects where it conceptually belongs.
- **Factory interfaces removed** — `DecoderFactory<T>` (S2) and `DecoderFactory` (S1) interfaces removed. Per-type factory classes become static methods. `FACTORIES` maps use `Function<SerializerProperties, Decoder>` (S2) and `Function<SendProp, Decoder>` (S1) as value types.
- **Factories return `Decoder`** — `S2DecoderFactory`, `S1DecoderFactory` return `Decoder` instead of `Decoder<T>` / `DecoderHolder`. No behavioral changes to factory logic.
- **S1 path migrated** — `SendProp` stores `Decoder` instead of `Decoder<T>`. `ReceiveProp.decode()` uses `DecoderDispatch`. `S1DecoderFactory` returns `Decoder`.

## Capabilities

### New Capabilities
- `decoder-codegen`: Annotation processor that scans `@RegisterDecoder` classes and generates `DecoderIds` (int constants) and `DecoderDispatch` (static tableswitch dispatch). The `Decoder` abstract class is handwritten.
- `static-decoder-dispatch`: `Decoder` abstract class hierarchy where decoder classes carry their own parameters, static decode methods, and generated dispatch replacing interface-based polymorphic decode calls.

### Modified Capabilities
- `event-codegen`: Modularization of the existing `EventAnnotationProcessor` into three separate processors (listener validation, provides indexing, event generation). No behavioral changes, only structural split.

## Impact

- **Hot path** (`S2FieldReader.readFieldsFast`): `decoder.decode(bs)` replaced by `DecoderDispatch.decode(bs, decoder)` — tableswitch + `invokestatic`, no virtual dispatch.
- **Decoder classes**: Add `extends Decoder`, `@RegisterDecoder`, `final`, convert `decode()` from instance to static with decoder as second parameter `d`. ID assigned automatically — no constructor changes needed. Fields, init logic unchanged.
- **Field hierarchy**: `ValueField`, `VectorField`, `PointerField` store `Decoder` and `SerializerProperties` as separate fields instead of `DecoderHolder`.
- **`S2DTClass`**: `getDecoderForFieldPath()` returns `Decoder` (unchanged method name, new return type).
- **`FieldGenerator`**: Calls to `S2DecoderFactory` now receive `Decoder` instead of `DecoderHolder`. Passes `SerializerProperties` separately to Field constructors.
- **Factory classes**: Per-type factory classes (`FloatDecoderFactory`, etc.) become static methods. `DecoderFactory<T>` (S2) and `DecoderFactory` (S1) interfaces removed.
- **Annotation processor source set**: New processor class, updated `META-INF/services` registration, existing processor split into three classes.
- **Rename**: `DecoderProperties` → `SerializerProperties`, `ProtoDecoderProperties` → `ProtoSerializerProperties` throughout the codebase.
- **S1 path**: `SendProp`, `ReceiveProp`, `S1DecoderFactory`, and S1 per-type factory static methods all migrated to `Decoder`.
