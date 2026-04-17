## 1. Stage 1 — Seal UsagePoint

- [x] 1.1 Mark `UsagePoint` (`processor/runner/UsagePoint.java` — or wherever it lives) as `abstract sealed class UsagePoint permits EventListener, InitializerMethod` (or equivalent based on actual subclasses via `AbstractInvocationPoint`). Mark each concrete subclass `final` or `non-sealed` as appropriate.
- [x] 1.2 In `processor/runner/ExecutionModel.java:78`, rewrite the `if (up instanceof EventListener) … else if (up instanceof InitializerMethod) … else requireProvider()` chain as an exhaustive pattern `switch (up)` producing the same side effects.
- [x] 1.3 Leave the single-branch `instanceof` checks at lines 201 and 288 as-is (they're not chains — the rewrite gives no benefit).
- [x] 1.4 Build: `./gradlew clarity:build`.
- [x] 1.5 Build clarity-analyzer (in its own repo dir: `/home/spheenik/projects/clarity/clarity-analyzer`).
- [x] 1.6 Build clarity-examples (in `/home/spheenik/projects/clarity/clarity-examples`).
- [x] 1.7 Commit: `refactor(processor): seal UsagePoint and pattern-switch ExecutionModel dispatch`.

## 2. Stage 2 — Seal Field

- [x] 2.1 Declare `public abstract sealed class Field permits ArrayField, PointerField, SerializerField, ValueField, VectorField` in `model/s2/Field.java`.
- [x] 2.2 Mark each subclass `final`: `ArrayField`, `PointerField`, `SerializerField`, `ValueField`, `VectorField`.
- [x] 2.3 Do not touch any call site in this commit — sealing alone is the deliverable. If the compiler flags an existing `instanceof` chain as newly-exhaustive-eligible, leave it for Stage 3.
- [x] 2.4 Build clarity + clarity-analyzer + clarity-examples.
- [x] 2.5 Commit: `refactor(model): seal Field and mark its subclasses final`.

## 3. Stage 3 — Convert Field / FieldLayout / StateMutation instanceof chains

- [x] 3.1 `state/FieldLayoutBuilder.java` (lines ~46, 56, 63, 69, 82): rewrite the `Field`-chain into an exhaustive pattern switch over the now-sealed `Field`. Keep the `ValueField`-specific decoder-kind sub-branch (lines ~88) as a nested switch on decoder kind only where it reads cleaner; do not force it into a single switch if the inner logic is asymmetric.
- [x] 3.2 `state/s2/S2FlatEntityState.java` — convert every `FieldLayout` / `StateMutation` / `SubStateKind` chain to exhaustive pattern switches. Sites: line 76 (layout walk), 126 (copy), 152 (lookup/read), 191 (iteration), 217 (write dispatch), 355 (deepclone Entry), 404 (sub-Entry iteration), 453 (release walk), 568 (transitive ref release). Also line 106 (`StateMutation` dispatch in `applyMutation`) and nested `SubStateKind` sites at 92 / 138 / 203 / 288 / 325 / 504.
- [x] 3.3 `state/s2/S2NestedArrayEntityState.java` — convert the `StateMutation` chain at line 133 and the `Field` chains at lines 98 / 161 / 171 to exhaustive pattern switches.
- [x] 3.4 `state/s2/S2TreeMapEntityState.java` — convert the `StateMutation` chain at line 71 and the `Field` chains at lines 54 / 57 / 90 to exhaustive pattern switches.
- [x] 3.5 Delete the terminal `throw new IllegalStateException(...)` branches that existed only to satisfy the compiler — exhaustive switches make them unreachable.
- [x] 3.6 Build clarity + clarity-analyzer + clarity-examples.
- [x] 3.7 Run `./gradlew clarity:test` (full test suite in the clarity repo).
- [ ] 3.8 Commit: `refactor(state): convert FieldLayout / StateMutation / Field dispatch to sealed pattern switches`.

## 4. Stage 4 — Migrate S1FlatLayout onto FieldLayout[]

- [x] 4.1 Check whether `accelerate-s1-flat-state` has landed. If it is still an open change, pause this stage and coordinate — fold the cleanup into that change instead of forking S1FlatLayout twice.
- [x] 4.2 In `state/s1/S1FlatLayout.java`: delete the `LeafKind` enum and the parallel `kinds` / `primTypes` / `maxLengths` / `offsets` arrays; replace with a single `FieldLayout[] leaves` field (leaves drawn from `FieldLayout.Primitive` / `InlineString` / `Ref`). Keep `dataBytes` and `refSlots` as aggregate fields.
- [x] 4.3 Update `S1FlatLayout.build(ReceiveProp[])` to construct `FieldLayout` records directly. Replace the `instanceof StringLenDecoder` predicate with the inline-string leaf construction (`FieldLayout.InlineString(offset, INLINE_STRING_MAX_LENGTH)`).
- [x] 4.4 In `state/s1/S1FlatEntityState.java` (lines ~71–78 and the corresponding write site): rewrite the `LeafKind` switch as an exhaustive pattern switch on `FieldLayout` (`case Primitive p -> …`, `case InlineString is -> …`, `case Ref r -> …`). The `Composite` / `Array` / `SubState` variants SHALL be unreachable for S1 leaves — rely on the sealed hierarchy's compile-time check (the compiler will force you to include them in the switch unless you constrain the array's static type to only the leaf variants; choose one: either a narrower sealed sub-hierarchy for leaves, or a `default -> throw` arm guarded by a comment explaining S1 doesn't produce those variants).
- [x] 4.5 Audit all call sites that previously called `layout.kinds()`, `layout.primTypes()`, `layout.maxLengths()`, `layout.offsets()`; replace with direct `leaves[idx]` access and pattern-match as needed.
- [x] 4.6 Build clarity + clarity-analyzer + clarity-examples.
- [x] 4.7 Run `./gradlew clarity:test`.
- [ ] 4.8 Commit: `refactor(state): migrate S1FlatLayout to sealed FieldLayout array`.

## 5. Stage 5 — Retarget the decoder dispatch generator (ATTEMPTED, REVERTED)

**Outcome: reverted after benchmarking.** A back-to-back JMH A/B (3 warmup + 10 measurement, same hardware state, Dota 2 S2 replay) showed Stage 5 consistently ~1–3% slower across all entity-state impls vs the stages 1–4 baseline. Hotspot.log inspection attributed the regression to the `typeSwitch` invokedynamic: 5 levels of LambdaForm/MethodHandle indirection, a ~25% larger C2 nmethod (~14KB vs ~11KB), and extra uncommon_traps during warmup that the generated int-switch tableswitch avoided. Sealing `Decoder` did not change this — `SwitchBootstraps.typeSwitch` does not special-case sealed hierarchies, so the dispatch cost was identical with or without the `permits` clause. See the revert commit message for numbers.

Tasks left as a record of what was attempted. Stage 5 specs deltas (`static-decoder-dispatch`, `decoder-codegen`) were dropped since the change no longer modifies those capabilities.


- [x] 5.1 Mark every `@RegisterDecoder` decoder class `final`. Do NOT seal `Decoder` — the `@RegisterDecoder` set is the source of truth; the generator provides coverage-by-construction; a `permits` clause would duplicate that and break the single-file-per-new-decoder ergonomic.
- [x] 5.2 Delete `Decoder.id`, `Decoder.IDS`, `Decoder.register(Class, int)`, the static `Class.forName("DecoderIds")` bootstrap, and the exception handling around it. `Decoder`'s constructor becomes `protected Decoder() {}` with an empty body.
- [x] 5.3 Retarget the annotation processor that currently emits `DecoderIds.java` and the `DecoderDispatch.java` int-switch: it SHALL now emit **only** `DecoderDispatch.java`, using a type-pattern switch on `Decoder`. The emitted `decode` body SHALL be `return switch (d) { case XDecoder x -> XDecoder.decode(bs, x); … default -> throw new IllegalStateException("Unknown decoder: " + d.getClass()); }`. Stateless decoders use the ignore-the-pattern-variable form `case XDecoder x -> XDecoder.decode(bs);`.
- [x] 5.4 Retarget the `decodeInto` generator arm similarly: type-pattern switch with explicit cases only for decoders declaring `decodeInto`, plus the `default -> throw` arm (which absorbs both unknown subclasses and registered decoders without a `decodeInto` method).
- [x] 5.5 Delete the `DecoderIds.java` generator code path and any `DecoderIds`-referencing templates from the processor module.
- [x] 5.6 Build clarity — verify the new `DecoderDispatch` is regenerated correctly by inspecting `build/generated/sources/annotationProcessor/java/main/skadistats/clarity/io/decoder/DecoderDispatch.java`. Confirm pattern cases + `default -> throw`; confirm `DecoderIds.java` is no longer emitted.
- [x] 5.7 Build clarity-analyzer + clarity-examples.
- [x] 5.8 Run `./gradlew clarity:test`.
- [ ] 5.9 Run the JMH benchmark harness (`./gradlew bench`) on a representative Dota 2 S2 replay; compare parse-time against the pre-Stage-5 baseline.
- [ ] 5.10 Commit: `refactor(decoder): dispatch via generated type-pattern switch, drop DecoderIds`.

## 6. Verification

- [ ] 6.1 Full repo-wide check: `./gradlew clean build` in clarity, clarity-analyzer, and clarity-examples. All three SHALL be green.
- [ ] 6.2 Confirm no `instanceof` chains on `FieldLayout`, `StateMutation`, `SubStateKind`, `Field`, or `UsagePoint` remain in `src/main/java` (grep sweep; single-branch `instanceof` is fine). `Decoder` is not checked here — its dispatch is generated.
- [ ] 6.3 Confirm `DecoderIds.java` is not regenerated and `Decoder.java` contains no `id` / `IDS` / `register` references.
- [ ] 6.4 Run `openspec verify --change adopt-sealed-type-switches` (or the `opsx:verify` skill) before archiving.
