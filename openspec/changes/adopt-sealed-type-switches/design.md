## Context

Clarity's core dispatch sites predate the Java 21 adoption landed alongside `seal-engine-types`. They still use pre-21 idioms:

- **`Decoder.id` + `DecoderIds` int-switch**: a bespoke discriminator (static `IDS` map populated by generated code) that exists purely so `DecoderDispatch.decode` / `decodeInto` can `switch` on an int. Each case downcasts `Decoder d` back to its concrete type. Pattern switches on `Decoder` remove the cast and the id, whether or not `Decoder` is sealed.
- **`S1FlatLayout.LeafKind` + parallel arrays**: `kinds[]`, `primTypes[]`, `maxLengths[]`, `offsets[]` instead of a `FieldLayout[]` drawn from the existing sealed `FieldLayout` hierarchy used by S2.
- **`instanceof` ladders on sealed bases**: `FieldLayout`, `StateMutation`, and the already-sealed `SubStateKind` are matched by `if (x instanceof A) … else if (x instanceof B) …` chains that the compiler cannot check for exhaustiveness. Missed branches fall through to a terminal `throw new IllegalStateException(...)` at runtime.
- **Non-sealed bases** for `Field` (5 subclasses) and `UsagePoint` (2 subclasses) prevent exhaustive pattern-matching on those hierarchies even though no external extension exists.

With the project now on `--release 21`, sealed interfaces + pattern switch expressions replace every one of these. The `typeSwitch` indy JITs to the same tableswitch as the hand-rolled int-switch, and every call-site gains compiler-enforced exhaustiveness.

## Goals / Non-Goals

**Goals:**
- Remove the `Decoder.id` discriminator and the `DecoderIds` generated class entirely.
- Seal `Field` and `UsagePoint` (subclasses `final`, `permits` lists exhaustive).
- Migrate `S1FlatLayout` to a single `FieldLayout[]`, reusing `FieldLayout.Primitive` / `InlineString` / `Ref`.
- Convert every internal `instanceof` ladder on `FieldLayout`, `StateMutation`, `SubStateKind`, `Field`, and `UsagePoint` into an exhaustive pattern `switch`.
- Each stage compiles clarity + clarity-analyzer + clarity-examples on its own and is landed as a separate commit.

**Non-Goals:**
- No behavioural changes (no new features, no bug fixes, no performance tuning beyond what falls out of `IDS.get(getClass())` disappearing).
- No refactoring of unrelated `instanceof` sites on non-sealable bases (e.g. `Field` decoder-kind predicates inside `FieldLayoutBuilder` that inspect decoder capabilities rather than decoder class).
- No changes to the annotation-processor module's build wiring — only its output product (`DecoderIds`) is retired; whether the `@RegisterDecoder` processor gets retired entirely or rewritten to enumerate `permits` is a Stage-5 implementation decision, not a design-level commitment.

## Decisions

### Stage ordering: cheapest first, dispatch-critical last

Order is chosen so every intermediate commit compiles and each stage is independently reviewable/revertable:

1. **`UsagePoint`** — 3-branch chain, tiny surface, non-hot-path. Exercises the workflow.
2. **Seal `Field`** — purely a declaration change; zero call-site churn in this commit.
3. **Convert `Field` / `FieldLayout` / `StateMutation` ladders** — mechanical rewrite unlocked by Stage 2.
4. **Migrate `S1FlatLayout`** — larger internal rewrite (S1 read/write loops rewritten to sealed-switch on `FieldLayout`).
5. **Retarget the decoder dispatch generator + retire `DecoderIds`** — biggest blast radius; benefits from the earlier stages landing first so any regression is bisectable against a stable base. `Decoder` stays non-sealed — see decision below.

Alternative considered: doing everything in one commit. Rejected — the Decoder stage alone touches generated code, the annotation processor, the decoder base class, and every dispatch site; bundling it with the seal-only stages makes review and bisection needlessly painful.

### `Decoder` stays non-sealed; generator emits type-pattern switch

The annotation processor is kept and retargeted to emit pattern switches on `Decoder`. Stage 5 changes only what the processor produces:

- **Before**: processor emits `DecoderIds` (int constants + `Decoder.register(Class, int)` calls in a static initializer) + `DecoderDispatch` (switch on `decoder.id` with per-case `(XDecoder) d` cast).
- **After**: processor emits only `DecoderDispatch` with `switch (d) { case XDecoder x -> XDecoder.decode(bs, x); … default -> throw new IllegalStateException("Unknown decoder: " + d.getClass()); }`. `DecoderIds`, the `IDS` map, and the `register`/`id` machinery all disappear. The pattern variable `x` has the concrete static type, so no explicit cast is needed in any case.

**Why not also seal `Decoder`?** Considered and rejected. Sealing requires a `permits` clause listing every concrete decoder. But the concrete-decoder set is discovered by the annotation processor (from `@RegisterDecoder`) and is only known at compile time — which creates a tail-eats-itself problem for a hand-written `Decoder.java`. Three ways to resolve it:

1. **Generate `Decoder.java` entirely.** Works, but puts the base class in `build/generated/` — IDE "go to definition" from a decoder subclass lands in generated output, and the tiny hand-written logic in `Decoder` gets templated into the processor.
2. **Hand-maintain `permits`, processor cross-checks.** Turns "drop a new `@RegisterDecoder` file and rebuild" into "edit two files per decoder." Degrades the ergonomic that justifies keeping the generator at all.
3. **Don't seal.** Generator emits a type-pattern switch with `default -> throw`. The generator is already the source of truth for the `@RegisterDecoder` set; it is *also* responsible for emitting one case per member of that set. Coverage is guaranteed by construction — the generator cannot forget to emit a case, because the set it enumerates is precisely the set of cases. Language-level exhaustiveness would be redundant with generator-level exhaustiveness.

Option 3 is chosen. Think of the generator as providing "sealing by construction": `@RegisterDecoder` is effectively the `permits` clause, enforced by codegen rather than by `javac`. The `default -> throw` arm exists only to satisfy `javac` (which doesn't know about `@RegisterDecoder`) and is dead code at runtime — if it ever fires, it means someone subclassed `Decoder` without `@RegisterDecoder`, which is already a misuse.

Consequence for other sealed hierarchies in this change: the argument doesn't apply. `Field`, `UsagePoint`, `FieldLayout`, `StateMutation`, `SubStateKind` all have hand-maintained subclass sets with hand-written switches. For those, compiler-enforced exhaustiveness is the whole point and has no codegen substitute.

### Keep `FieldLayout` and `StateMutation` unchanged

Both are already sealed and their record shapes are load-bearing (offsets, bytes, mutation payloads). This change only rewrites the *call sites* that match against them.

### `S1FlatLayout` reuses S2's `FieldLayout` hierarchy

The S2 sealed `FieldLayout` hierarchy (`Primitive(int offset, PrimitiveType type)`, `InlineString(int offset, int maxLength)`, `Ref(int offset)`, plus the non-leaf `Composite` / `Array` / `SubState`) already covers every S1 leaf shape. S1 uses only the leaf records (`Primitive`, `InlineString`, `Ref`) because S1's flat state is a single flat array of leaves — no nesting. Reusing the hierarchy keeps S1 and S2 speaking the same vocabulary and lets future shared read/write helpers be written once.

Alternative considered: keep a separate `S1Leaf` sealed interface distinct from `FieldLayout`. Rejected — the shapes are identical; splitting them is duplication without information gain.

### `S1FlatLayout` stores `FieldLayout[]` + `int dataBytes` + `int refSlots`

The migration drops `kinds[]`, `primTypes[]`, `maxLengths[]`, and `offsets[]`. Offsets move into the `FieldLayout.Primitive.offset()` / `InlineString.offset()` / `Ref.offset()` record components — each leaf now carries its own offset, same as S2. `dataBytes` and `refSlots` remain on `S1FlatLayout` itself since they are per-layout aggregates, not per-leaf.

## Risks / Trade-offs

- **[Stage 5 regression risk] Decoder dispatch is the single hottest call site in the parse loop.** → Mitigation: benchmark before/after with the existing JMH harness (`./gradlew bench`) on a representative Dota S2 replay; require zero measurable regression before landing.
- **[Stage 5 downstream breakage] Removing `Decoder.id` breaks any downstream consumer that reads `d.id`.** → Verified mitigation: `d.id` is `protected` + unreferenced outside `DecoderDispatch`. Confirmed by grep. (Subclassing `Decoder` externally was already discouraged — `@RegisterDecoder` is internal to clarity.)
- **[Stage 4 conflict] `S1FlatLayout` is also touched by the pending `accelerate-s1-flat-state` change.** → Mitigation: sequence this change's Stage 4 to land *after* `accelerate-s1-flat-state` if that one is still open when we reach Stage 4, or fold the layout-representation cleanup into that change instead. Decide at Stage-4 commit time based on the state of the other change.
- **[Reviewer load] Five commits touching overlapping subsystems.** → Mitigation: each commit compiles the full tree (clarity + clarity-analyzer + clarity-examples) and is independently revertable. Tasks.md enforces this discipline.

## Migration Plan

Per-stage: land commit → build clarity → build clarity-analyzer → build clarity-examples → run full test suite on clarity → if Stage 5, also run JMH bench harness and compare. If any step fails, revert the stage commit and fix before proceeding.

No external migration steps — all changes are internal. Downstream (clarity-analyzer, user code) see no API surface changes.

## Open Questions

- **Stage 4 timing vs. `accelerate-s1-flat-state`:** decided at commit time for Stage 4, based on whether that change has landed.
