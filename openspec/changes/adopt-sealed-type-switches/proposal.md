## Why

Several internal hierarchies still use pre-Java-21 idioms that sealed types + pattern switches now obsolete on JDK 21: the `Decoder` dispatch carries a synthetic `int id` with a static `IDS` registry and generated `DecoderIds`; `S1FlatLayout` encodes leaf kind as a parallel-array `LeafKind` enum; and `Field`, `UsagePoint`, plus the already-sealed `FieldLayout` / `StateMutation` are still read via long `instanceof` chains whose exhaustiveness the compiler cannot check.

Sealing these bases and rewriting the dispatch sites as exhaustive `switch` expressions removes boilerplate (the ID registry, parallel arrays, per-case casts), makes "did you forget a case?" a compile error, and — on hot paths — lets the JIT collapse dispatch to a `tableswitch` on class ID with no per-instance map lookup.

## What Changes

Each stage is an isolated commit, verified by compiling `clarity`, `clarity-analyzer`, and `clarity-examples` before moving on. Order is chosen so each commit independently compiles.

- **Stage 1 — Seal `UsagePoint`** (`processor/runner/ExecutionModel.java`): mark `UsagePoint` sealed permitting its direct subclasses, rewrite the 3-branch `instanceof` chain as an exhaustive switch. Smallest, lowest-risk stage; exercises the workflow.
- **Stage 2 — Seal `Field`** (`model/s2/Field.java` + 5 subclasses): `sealed permits ArrayField, PointerField, SerializerField, ValueField, VectorField`; mark each subclass `final`. No call-site changes in this commit.
- **Stage 3 — Convert `Field` / `FieldLayout` / `StateMutation` `instanceof` chains to exhaustive switches** across `state/FieldLayoutBuilder.java`, `state/s2/S2FlatEntityState.java`, `state/s2/S2NestedArrayEntityState.java`, `state/s2/S2TreeMapEntityState.java`. Unlocked by Stage 2 for `Field`; the others rely on already-sealed bases.
- **Stage 4 — Migrate `S1FlatLayout` onto `FieldLayout[]`**: drop the `LeafKind` enum and the parallel `kinds` / `primTypes` / `maxLengths` / `offsets` arrays; reuse `FieldLayout.Primitive` / `InlineString` / `Ref`. Rewrite `S1FlatEntityState` read/write as exhaustive switches on the sealed `FieldLayout`.
- **Stage 5 — **BREAKING** replace int-id dispatch with type-pattern dispatch**: `Decoder` stays non-sealed (the `@RegisterDecoder` annotation set is the source of truth for concrete decoders; hand-maintaining a `permits` clause alongside it would defeat the "drop a file, rebuild" ergonomic — the processor effectively provides sealing-by-construction). Retarget the annotation processor to emit `DecoderDispatch.decode` / `decodeInto` as type-pattern switches with an explicit `case XDecoder x -> …` arm per `@RegisterDecoder` class and a terminal `default -> throw` arm. Remove `Decoder.id`, the static `IDS` map, the `Class.forName("DecoderIds")` bootstrap, and the generated `DecoderIds` class. Pattern variables replace the per-case cast.

## Capabilities

### New Capabilities

(None — all work modifies existing behaviour.)

### Modified Capabilities

- `static-decoder-dispatch`: the "Automatic ID assignment" requirement and the `Decoder.id` field are removed. `DecoderDispatch.decode` / `decodeInto` dispatch via type-pattern switch on `Decoder`, not on `d.id`. The generated `DecoderIds` class is no longer part of the contract. All other requirements (static decode methods, `DecoderDispatch` as the single call site, nested composition) remain.
- `decoder-codegen`: the annotation processor is retargeted to emit type-pattern switches in `DecoderDispatch` (both `decode` and `decodeInto`) using pattern variables. The `DecoderIds` generation step and the `Decoder.register` / `IDS` / `Class.forName` machinery are removed. `Decoder` remains a non-sealed abstract class — exhaustiveness is enforced by the generator covering every `@RegisterDecoder` class; a `default -> throw` arm closes the switch. `@RegisterDecoder` validation, `decodeInto` validation, and the "primitive decoders implement decodeInto" coverage requirement are unchanged.

Stages 1–4 touch only internal implementation (class sealing, `instanceof`→`switch` rewrites, parallel-array→record-array replacement in `S1FlatLayout`) without altering any spec-level requirement — no delta spec files are emitted for those stages.

## Impact

- **Build**: no toolchain changes (JDK 21 already required).
- **clarity internals** (~15 files):
  - New `sealed`: `Field`, `UsagePoint` + their subclasses marked `final`. `Decoder` stays non-sealed (see Stage 5 rationale).
  - Touched dispatch sites: `DecoderDispatch.java` (generated → hand-written or regenerated), `ExecutionModel.java`, `FieldLayoutBuilder.java`, `S1FlatLayout.java`, `S1FlatEntityState.java`, `S2FlatEntityState.java`, `S2NestedArrayEntityState.java`, `S2TreeMapEntityState.java`.
  - Removed: `Decoder.id` field, `Decoder.IDS` map, `Decoder.register`, the generated `DecoderIds` class.
  - Retargeted: the annotation processor that emits `DecoderDispatch` now produces a sealed-type pattern switch instead of an int-id switch (the `DecoderIds` template arm is deleted).
- **clarity-analyzer**: verified no subclasses of `Field` or `UsagePoint`, and no consumers of `Decoder.id`. Expected zero changes.
- **clarity-examples**: zero changes (no example touches decoder IDs, field subtype introspection, or usage points).
- **Performance**: Stage 5 removes one `HashMap.get(Class)` per decoder construction and one cast per dispatched decode; pattern switch dispatch is ≈ equivalent to the current int-switch after JIT. Stages 1–4 are neutral on hot paths but improve maintainability.
- **Downstream API**: no public API is removed. The `Decoder` class was abstract and its `id` field was `protected` — downstream code subclassing `Decoder` would break, but no such consumer exists.
