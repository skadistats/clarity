## Context

The S2 entity decode path calls `Decoder<T>.decode(BitStream)` through an interface with ~30 implementations. This is the last remaining megamorphic call site in the hot path after the FieldOp switch refactor. The project already has an annotation processor infrastructure using JavaPoet for event code generation.

Current decode call chain:
```
S2FieldReader.readFieldsFast()
  → S2DTClass.getDecoderForFieldPath(fp)
    → Field.getDecoder()            // virtual: Field hierarchy
      → DecoderHolder.getDecoder()  // returns Decoder<T>
  → decoder.decode(bs)              // MEGAMORPHIC: 30 implementations
```

## Goals / Non-Goals

**Goals:**
- Eliminate the megamorphic `decoder.decode(bs)` call site in `S2FieldReader.readFieldsFast()`
- All decode dispatch via `tableswitch` on int IDs + `invokestatic` calls
- Decoder logic stays in decoder classes (single source of truth) — the switch is generated
- Modularize the annotation processor into focused, single-responsibility classes
- Clean removal of `DecoderHolder` and `Decoder<T>` interface

**Non-Goals:**
- Changing individual decode algorithms (just eliminating dispatch overhead)
- Changing the `SerializerProperties` (formerly `DecoderProperties`) / `ProtoSerializerProperties` init-time data flow (stays as init-time parameter bundle, only renamed)
- Performance optimization of individual decode methods
- Changing the Field hierarchy (`ValueField`, `VectorField`, etc.) beyond swapping `DecoderHolder` for `Decoder`

## Decisions

### 1. Decoder class IS the parameter carrier — no nested Params

Each decoder class extends a `Decoder` abstract class directly, keeping its existing fields and constructors. There are no separate nested `Params` records.

```java
@RegisterDecoder
public final class FloatQuantizedDecoder extends Decoder {
    private final int bitCount;
    private final int encodeFlags;
    // ... existing fields, init logic unchanged ...

    public FloatQuantizedDecoder(...) {
        // ... existing init logic ...
    }

    public static Float decode(BitStream bs, FloatQuantizedDecoder d) {
        var v = bs.readUBitInt(d.bitCount) * d.decodeMultiplier;
        return d.minValue + (d.maxValue - d.minValue) * v;
    }
}
```

**Why over nested Params records:** Records cannot extend classes, so they'd require a sealed interface + pattern matching switch for dispatch — which is only stable in Java 21 (Clarity targets Java 17). More importantly, nested records would duplicate all fields from the decoder class into the record, and require `createParams()` factory methods to replace constructors. With "decoder IS params", constructors stay, fields stay, init logic stays. The migration per decoder is minimal: add `extends Decoder`, `@RegisterDecoder`, `final`, and convert `decode()` from instance to static. No generated code is referenced in the decoder source.

**Why `extends` (class) over `implements` (interface):** Abstract class with a `final int id` field allows `tableswitch` on `d.id` with Java 17. An interface with `instanceof` pattern matching switch would require Java 21. The `id` field is a direct field access — no virtual call overhead.

**Why handwritten, not generated:** Source code extending a generated class creates a circular compile dependency — the annotation processor needs to discover the annotated classes to generate `Decoder`, but the annotated classes need `Decoder` to compile. Handwriting `Decoder` avoids this entirely. The class manages automatic ID assignment: it has a `protected final int id` field, a package-private `static void register(Class, int)` method that populates an internal `Map<Class, Integer>`, and a static initializer that loads the generated `DecoderIds` class via `Class.forName` (no compile-time dependency on generated code). The no-arg `protected Decoder()` constructor looks up `this.id` from the map via `getClass()`. This runs once per decoder instance at init time.

**Why not sealed:** `sealed` with `permits` would need the permits clause to list all decoder subclasses, which reintroduces the generation problem. Since dispatch goes through `tableswitch` on int IDs (not pattern matching on sealed types), `sealed` provides no runtime benefit. The annotation processor already enforces that all `@RegisterDecoder` classes extend `Decoder`.

### 2. Static decode methods — differentiated signature

Stateful decoders provide `public static T decode(BitStream bs, XDecoder d)`. The second parameter is typed as the concrete decoder class, giving direct field access without casts. Stateless decoders provide `public static T decode(BitStream bs)` — no decoder parameter, since there is no state to access.

The `DecoderAnnotationProcessor` determines which variant to generate by inspecting the static `decode` method. Validation rules (compile errors on violation):
- The class MUST have exactly one `public static` method named `decode`
- The first parameter MUST be `BitStream`
- Total parameters MUST be 1 (stateless) or 2 (stateful)
- If 2 parameters, the second parameter type MUST match the declaring class

One parameter → stateless dispatch (`XDecoder.decode(bs)`), two parameters → stateful dispatch (`XDecoder.decode(bs, (XDecoder) decoder)`).

**Why static:** Eliminates the `invokeinterface` / `invokevirtual` megamorphic call. The generated `DecoderDispatch` switch casts once (stateful) or calls directly (stateless) via `invokestatic` — monomorphic and trivially inlineable.

**Why `d` not `p`:** The parameter IS the decoder instance, not a separate params object. `d` for decoder.

**Why differentiated (not uniform with unused `d`):** Stateless decoders gain nothing from receiving a parameter they ignore. The processor distinguishes the two cases with a single `getParameters().size()` check — trivial to implement. The generated switch has two call patterns but each case is still a single `invokestatic`.

### 3. DecoderIds with auto-assigned int constants

A generated `DecoderIds` class provides `public static final int` constants, one per `@RegisterDecoder`-annotated class, auto-assigned alphabetically starting at 0. A `COUNT` constant holds the total number of decoders. The class has a static initializer that calls `Decoder.register(XDecoder.class, ID)` for each decoder, populating the ID map in the handwritten `Decoder` base class.

```java
// Generated:
public final class DecoderIds {
    public static final int ARRAY = 0;
    public static final int BOOL = 1;
    public static final int FLOAT_QUANTIZED = 2;
    // ...
    public static final int COUNT = 30;

    static {
        Decoder.register(ArrayDecoder.class, ARRAY);
        Decoder.register(BoolDecoder.class, BOOL);
        Decoder.register(FloatQuantizedDecoder.class, FLOAT_QUANTIZED);
        // ...
    }
}
```

**Why int IDs over instanceof:** Java 17 compatibility. `switch` on `int` compiles to `tableswitch` bytecode — O(1) dispatch. Pattern matching switch on sealed types is Java 21+.

**Why auto-assign alphabetically:** IDs are never persisted, only used within a single build. Alphabetical assignment is deterministic and avoids manual ID management. Adding a new decoder = annotate with `@RegisterDecoder` and `extends Decoder`, done. The ID is assigned automatically — no generated code referenced in the decoder source.

**ID naming convention:** Class name with `Decoder` suffix removed, converted to UPPER_SNAKE_CASE. `FloatQuantizedDecoder` → `FLOAT_QUANTIZED`.

### 4. Nested decoder resolution via recursive dispatch

Compound decoders (`VectorDefaultDecoder`, `VectorDecoder`, `ArrayDecoder`) store the inner decoder as a `Decoder` reference and call `DecoderDispatch.decode()` recursively:

```java
@RegisterDecoder
public final class VectorDefaultDecoder extends Decoder {
    private final int dim;
    private final Decoder innerDecoder;

    public VectorDefaultDecoder(int dim, Decoder innerDecoder) {
        this.dim = dim;
        this.innerDecoder = innerDecoder;
    }

    public static Vector decode(BitStream bs, VectorDefaultDecoder d) {
        var result = new float[d.dim];
        for (var i = 0; i < d.dim; i++) {
            result[i] = (Float) DecoderDispatch.decode(bs, d.innerDecoder);
        }
        return new Vector(result);
    }
}
```

**Why over flattened combinations:** Flattening (VECTOR_3D_NO_SCALE, VECTOR_3D_CELL_COORD, ...) causes combinatorial explosion. Recursive dispatch via `DecoderDispatch.decode()` is composable, and the inner call is the same `tableswitch` → `invokestatic` path — no virtual dispatch.

### 5. Pre-computation stays in constructors

`FloatQuantizedDecoder` has non-trivial init logic (`computeEncodeFlags`, `initialize`, `assignRangeMultiplier`). This stays in the constructor — the decoder instance IS the params, so the constructor creates the fully initialized, immutable-after-construction object directly.

**Why:** No `createParams()` factory method needed. The existing constructor already does exactly what's needed. The instance is created once at init time and reused for all decode calls.

### 6. DecoderProperties renamed to SerializerProperties and stored on Field

`DecoderProperties` is renamed to `SerializerProperties` (and `ProtoDecoderProperties` to `ProtoSerializerProperties`). The data describes serializer field metadata from the protobuf (encodeFlags, bitCount, lowValue, highValue, encoderType, polymorphicTypes) — not decoder properties. The rename reflects where it conceptually belongs.

`SerializerProperties` continues to serve as init-time parameter bundle from serializer parsing to decoder factories. Additionally, it is now stored directly on Field subclasses (`ValueField`, `PointerField`) as its own field alongside the `Decoder`. Previously it was bundled in `DecoderHolder` — with `DecoderHolder` removed, the Field stores both pieces separately.

**VectorField exception:** `VectorField` does not store `SerializerProperties`. Its length decoder is always `IntVarUnsignedDecoder` (hardcoded "uint32", never configurable). After the refactor it holds a `private static final IntVarUnsignedDecoder LENGTH_DECODER = new IntVarUnsignedDecoder()` singleton and returns it from `getDecoder()`. No `SerializerProperties` needed — `getSerializerProperties()` returns `SerializerProperties.DEFAULT`.

**Why rename:** The data originates from `ProtoFlattenedSerializerField_t` and describes how a field is serialized on the wire. Calling it "decoder properties" was misleading — decoders consume this data at init time, but the data belongs to the serializer field definition.

**Why store on Field:** The debug path in `S2FieldReader.readFieldsDebug()` accesses `getDecoderProperties()` (now `getSerializerProperties()`) via the Field. With `DecoderHolder` removed, the Field is the natural home — it already has the FieldType and decoder, and the serializer properties are per-field metadata.

### 7. Processor modularization

Split `EventAnnotationProcessor` into:

| New Processor | Annotation | Output |
|---|---|---|
| `ListenerValidationProcessor` | `@UsagePointMarker(EVENT_LISTENER)` | Compile errors on signature mismatch |
| `ProvidesIndexProcessor` | `@Provides` | `META-INF/clarity/providers.txt` |
| `EventGenerationProcessor` | `@GenerateEvent` | `*_Event` classes |
| `DecoderAnnotationProcessor` | `@RegisterDecoder` | `DecoderIds`, `DecoderDispatch` |

Each registered separately in `META-INF/services/javax.annotation.processing.Processor`.

**Why now:** We're adding a 4th processor concern. Splitting now avoids a monolith and each processor is independently testable.

### 8. Factory interfaces replaced by lambdas

The `DecoderFactory<T>` interface (S2) and `DecoderFactory` interface (S1) are removed. Per-type factory classes (`FloatDecoderFactory`, `QAngleDecoderFactory`, etc.) become static methods. The `FACTORIES` maps use functional types:

```java
// S2DecoderFactory:
private static final Map<String, Function<SerializerProperties, Decoder>> FACTORIES = Map.of(
    "float32", FloatDecoderFactory::createDecoder,
    "QAngle",  QAngleDecoderFactory::createDecoder,
    ...
);

// S1DecoderFactory:
private static final Map<PropType, Function<SendProp, Decoder>> FACTORIES = Map.of(
    INT,   IntDecoderFactory::createDecoder,
    FLOAT, FloatDecoderFactory::createDecoder,
    ...
);
```

**Why lambdas over keeping the interface:** The factory interfaces exist only to type the map values. With one method each, `Function<>` is a direct replacement with no custom type needed. The per-type factory classes stay as static method containers for organization.

**Note on VectorDecoderFactory:** This factory takes a `dim` constructor parameter that varies per map entry (2, 3, 4). Since `createDecoderStatic(dim, props)` already exists, the map entries become lambdas capturing `dim`: `props -> VectorDecoderFactory.createDecoderStatic(3, props)` rather than simple method references.

## Risks / Trade-offs

**[Risk] `d.id` field access in the switch** → The `id` field is `protected final` on the `Decoder` base class and accessed directly — no virtual call. This is a simple field read, not a dispatch.

**[Risk] Inner dispatch for Vector/Array adds a second switch per field** → Only affects compound types (Vector, Array), which are a minority of fields. The alternative (combinatorial flattening) is worse for maintainability. If profiling shows this matters, specific hot combinations can be flattened later.

**[Trade-off] Generated code in build output** → Developers must build before `DecoderIds` and `DecoderDispatch` exist. This is already the case for generated `*_Event` classes, so the workflow is established. IntelliJ navigates to generated sources under `build/generated/sources/annotationProcessor/`.

**[Trade-off] `Class.forName` for ID registration** → The handwritten `Decoder` class loads `DecoderIds` via `Class.forName` in its static initializer to trigger ID registration. This is a well-known pattern for breaking compile-time dependencies on generated code. The lookup happens once (class loading), not per decode. If `Class.forName` fails (e.g. a broken build that didn't run annotation processing), the `ExceptionInInitializerError` propagates — no silent fallback. A missing `DecoderIds` is a build defect, not a recoverable state.

**[Trade-off] ArrayDecoder loses generics** → `ArrayDecoder<T>` becomes non-generic `ArrayDecoder extends Decoder`. Its static decode method returns `Object` (array). This is fine because the entity system already stores all values as `Object` — `S2DTClass.getDecoderForFieldPath()` returns raw `Decoder` and erasure applies.
