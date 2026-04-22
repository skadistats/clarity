## Why

S1 is the last state type still wrapping every decoded field in a `StateMutation.WriteValue` on the hot path. `S1FieldReader.readFields` allocates one `WriteValue` record per field and hands the boxed `Integer`/`Float`/`Long`/`String` produced by `DecoderDispatch.decode` to `ObjectArrayEntityState.write`, which stores it as an `Object` in an `Object[]`. For a Dota 2 S1 replay this is 458 × 512-byte-capable `String` allocations, ~35K `Integer`/`Float` boxings per entity update, plus a `WriteValue` record per field — everything the S2 hot path eliminated in `accelerate-flat-entity-state`.

The prerequisites for a one-shot S1 port now all hold:

- **Decoder `decodeInto` coverage is complete.** Every primitive and vector decoder used by S1 (`Int*`, `Long*`, `Float*`, `Vector*`) already declares `decodeInto` via the static-dispatch codegen pattern, landed in the S2 change.
- **Inline-string infrastructure exists.** `StringLenDecoder.decodeIntoInline` (S1 uses the same shared decoder) writes length-prefix + UTF-8 directly into the target `byte[]`. `FieldLayout.InlineString` leaf shape is already defined and consumed by `FlatEntityState.decodeInto`.
- **COW is gone.** `strip-entity-state-cow` removed owner-pointer machinery from `FlatEntityState` and `NestedArrayEntityState`. The eager-copy model is the target baseline for this port — S1 can adopt it directly without adding COW only to strip it.
- **Direct-write contract is in place.** `EntityState` interface already exposes `decodeInto(FieldPath, Decoder, BitStream)` and `write(FieldPath, Object)`; the `state-mutation` and `entity-update-commit` specs already describe S1 behaviour — the current code simply lags the spec. This change is the catch-up.

**S1 ARRAY stays on the boxing path.** Audit `0.6` from the archived `accelerate-flat-entity-state` identified 11 ARRAY props in the full Dota S1 DT (all with `numElements` ∈ {2, 10, 16, 32, 33}) whose inner-element types were not audited. `ArrayDecoder.decode` produces a runtime-sized `Object[]`; inlining it is deferred. S1 keeps a small refs slab to hold these `Object[]` values (≤11 slots per entity class worst case).

## What Changes

### S1FlatEntityState added alongside ObjectArrayEntityState
- **New class** `S1FlatEntityState` in `skadistats.clarity.model.state` implementing `EntityState`. Single-level layout (S1 has no sub-serializers, no nested arrays beyond ARRAY props, no polymorphism). Owns: `S1FlatLayout layout` (shared per-DTClass), `byte[] data`, optional `Object[] refs` (only if the DTClass has ARRAY props). No COW, no owner pointers — mirrors the post-`strip-entity-state-cow` eager-copy model.
- **Keep `ObjectArrayEntityState`** as a parallel variant, matching how S2 retains `NESTED_ARRAY` / `TREE_MAP` alongside `FLAT`. Both variants are selectable at runtime via `withS1EntityState(S1EntityStateType)` for benchmarking, regression investigation, and as an escape hatch.
- **Extend `S1EntityStateType`** with a new `FLAT` variant alongside the existing `OBJECT_ARRAY`. The runner default flips from `OBJECT_ARRAY` to `FLAT` once CP-2's parity + perf gates pass; `OBJECT_ARRAY` remains opt-in.

### S1FlatLayout — per-DTClass immutable layout
- **New class** `S1FlatLayout` built once per `S1DTClass` and cached on it. Maps each receive-prop index to a leaf kind (Primitive / InlineString / Ref) plus a `byte[]` offset and, for strings, `maxLength = 512` (uniform, grounded in `StringLenDecoder`'s 9-bit wire cap). Total `byte[]` size is `Σ (1 + slotSize)` across all props (1 flag byte + value bytes). Refs are dynamically allocated from a small free-list for ARRAY leaves.
- Leaf kind derived from `decoder.getPrimitiveType()` (non-null → Primitive) or `decoder instanceof StringLenDecoder` (→ InlineString). Everything else → Ref.
- `S1DTClass` gains `getFlatLayout()` returning the cached layout; built lazily on first use.

### S1FieldReader.readFields — decode-direct fast path for both variants
- **Fast path is state-agnostic.** `S1FieldReader.readFields` calls `state.decodeInto(fp, decoder, bs)` for primitive / inline-string leaves and `state.write(fp, DecoderDispatch.decode(bs, decoder))` for REF leaves. `S1FlatEntityState` implements both zero-alloc; `ObjectArrayEntityState` implements `write` (already present) and adds a minimal `decodeInto` that falls back to `write(fp, DecoderDispatch.decode(bs, decoder))` so the reader has a uniform call shape. The boxing cost stays on `OBJECT_ARRAY`; `FLAT` avoids it.
- **Routing hint comes from the layout.** Whether a leaf is PRIMITIVE / INLINE_STRING / REF is stored on `S1FlatLayout` (the FLAT variant's layout); under OBJECT_ARRAY the reader still needs a per-prop classification, so the classification is hoisted to `S1DTClass.getFlatLayout()` and shared — the layout is cheap to build and always safe to reference even when the chosen state is `OBJECT_ARRAY`.
- **No `StateMutation.WriteValue`** allocated on the hot path for either variant. `FieldChanges` carries `fieldPaths[]` only; `mutations == null`; `capacityChanged = false` (S1 has no structural capacity changes).
- **Debug path** (`debug=true`): unchanged — retains the TextTable + boxed-value dump for inspection.
- **Materialize path** (`materialize=true`): produces `StateMutation.WriteValue` records for `MutationRecorder` / baseline materialization via `DecoderDispatch.decode` + `new WriteValue(...)`. Not on the hot path.

### S1 copy() is eager deep copy (no COW)
- `S1FlatEntityState.copy()` allocates a fresh `byte[]` via `Arrays.copyOf(data, data.length)` and, if present, `Arrays.copyOf(refs, refs.length)` + `int[] freeSlots` clone. Same discipline as the post-`strip-entity-state-cow` FLAT/NESTED_ARRAY. No owner pointers, no writable flags.

### Non-goals
- **Deleting `ObjectArrayEntityState`.** Kept as a parallel, selectable variant — mirrors S2's pattern of retaining `NESTED_ARRAY` / `TREE_MAP` alongside `FLAT`. Useful for benchmarking, regression investigation, and as a bailout if a downstream consumer relies on `Object[]`-backed storage semantics.
- **Inline S1 ARRAY props.** Deferred — tracked as a future follow-up. Needs the `s1sendtables` dumper extended to print `SendProp.template.type` and an inner-type audit to decide whether `ArrayDecoder.decodeInto` is worth building. Current 11 ARRAY props account for ~0.02% of all S1 props; 458 inlined STRINGs already dominate the savings.
- **Cached-String companion.** Read-time `new String(data, off, len, UTF_8)` on inline-string reads is accepted (same trade-off S2 made); add a companion cache only if profiles show it regressing.
- **Typed S1 read API** (`getInt`, `getFloat`, etc.). Out of scope; same phase-3 direction as S2.

## Dependencies

- **accelerate-flat-entity-state** (ARCHIVED 2026-04-16) — introduced `EntityState.decodeInto` / `write`, `InlineString` leaf shape, `DecoderDispatch.decodeInto`, inline-string decoders. All S1-relevant primitive decoders already have `decodeInto`.
- **strip-entity-state-cow** (ARCHIVED 2026-04-16) — eliminated owner-pointer COW. This port adopts the eager-copy model directly, avoiding the temporary-COW detour called out in the original deferral note.

## Capabilities

### New Capabilities
- `s1-flat-entity-state`: byte[]-backed S1 entity state with flat per-DTClass layout, `decodeInto`/`write` direct-write, eager deep `copy()`, optional small refs slab for ARRAY props.

### Modified Capabilities
None. Existing `state-mutation` and `entity-update-commit` specs (authored during `accelerate-flat-entity-state`) already describe the S1 decode-direct behaviour this change delivers; the implementation catches up to the spec.

## Impact

### Code
- `skadistats.clarity.model.state`:
  - **New**: `S1FlatEntityState`, `S1FlatLayout` (and its builder).
  - **Unchanged**: `ObjectArrayEntityState` stays as a parallel variant; gains a `decodeInto` implementation that falls back to `write(fp, DecoderDispatch.decode(bs, decoder))` so both state types share the reader call shape.
  - **Modified**: `S1EntityStateType` — `FLAT` variant added alongside existing `OBJECT_ARRAY`. `FLAT.createState(...)` allocates `S1FlatEntityState`. `OBJECT_ARRAY.createState(...)` unchanged.
  - Renaming the existing S2 state classes (`FlatEntityState`, `NestedArrayEntityState`, `TreeMapEntityState`) to pick up an explicit `S2` prefix for full symmetry with `S1FlatEntityState` is **out of scope here**; it is a purely mechanical rename sweep and will ship as a separate small follow-up change.
- `skadistats.clarity.io.s1`:
  - `S1FieldReader.readFields` — rewritten per routing above. Debug/materialize branches preserved. Uses `EntityState.decodeInto` / `write` uniformly; the state implementation decides whether that's zero-alloc (FLAT) or falls back to the boxing path (OBJECT_ARRAY).
  - `S1DTClass` — gains `S1FlatLayout` build + cache (used by both variants for per-prop leaf-kind classification on the reader side).
  - `ReceiveProp.decode` — unchanged; still used by non-hot callers (if any survive the audit) and by the debug path.
- `skadistats.clarity.processor.runner`:
  - `AbstractFileRunner.s1EntityStateType` default → `S1EntityStateType.FLAT` (after CP-2 perf/parity gates). `OBJECT_ARRAY` remains opt-in via `withS1EntityState(OBJECT_ARRAY)`.
  - `withS1EntityState(S1EntityStateType)` signature unchanged.

### Performance expectations

**Targets** (pre-implementation):
- Wall-clock: ≥ -20% on the new `S1EntityStateParseBench`.
- Allocation: ≥ -50% byte allocation per S1 parse under `-prof gc`.

**Actuals** (from `bench-results/2026-04-16_184644_s1_next-590d52f/`, 3 warmup × 10 measurement single-shot iterations):
- Dota S1: -2.6% wall (393.5 → 383.4 ms median), -3.7% alloc (4.38 → 4.22 GB).
- CSGO S1: -4.5% wall (1204.7 → 1150.0 ms median), -3.9% alloc (15.56 → 14.96 GB).

The targets were missed by ~4×. Root cause: the design's state-agnostic reader (D8) eliminates `WriteValue` allocation for **both** state variants via the fast-path `FieldChanges` constructor — the biggest projected saving accrues to OBJECT_ARRAY too, leaving only the primitive-boxing + `String`-alloc delta as the FLAT-vs-OBJECT_ARRAY differential. Full breakdown in `RESULTS.md`. Architecture and parity are correct; the headline numbers reflect the unified reader, not a defective port.

### Benches / tests
- **New**: `S1EntityStateParseBench` (JMH single-shot) — parametrized on `@Param({"OBJECT_ARRAY", "FLAT"})`. Keeps `OBJECT_ARRAY` in the matrix long-term as the baseline measurement side.
- **New**: `S1FlatEntityStateTest` — copy independence, fieldPathIterator coverage, inline-string round-trip parity vs `StringLenDecoder.decode`.
- `:repro:issue289Run`, `:repro:issue350Run` — green (both are S1/S2 replay reproducers).
- `:dev:dtinspectorRun` on an S1 replay — compiles + parses cleanly (GUI-compile, don't launch, per session preference).
- Downstream `clarity-analyzer` — compiles against modified clarity sources (composite build). Interactive-run gate is user-verified.
- `./gradlew build` — full suite green.

### Consumers
- No user-visible API changes. `Entity.getProperty(String)` / `@OnEntityUpdated` etc. continue to work — they flow through `EntityState.getValueForFieldPath`, which `S1FlatEntityState` implements by reading back via the per-leaf `PrimitiveType.read` / `new String(data, off, len, UTF_8)` / refs-slab lookup.
- Example code in `clarity-examples` that handles S1 replays: unaffected.

### Successor / follow-ups
- **Inline S1 ARRAY props** — extend `s1sendtables` dumper with `template.type`, audit the 11 inner types. If uniformly primitive, add `ArrayDecoder.decodeInto` that reserves `numElements × innerSize + countPrefix` bytes and composes inner `decodeInto` per element. Drops the S1 refs slab entirely, shrinks `S1FlatEntityState` to `byte[] data + layout`.
- **S1 cached-String companion** — only if read-time `new String` allocation surfaces in profiles on S1-heavy workloads.
- **Retire `ObjectArrayEntityState`** — if `FLAT` proves strictly superior across benches and no consumer depends on the boxed-`Object[]` semantics, the legacy variant can be deleted in a follow-up cleanup change. Not part of this change.
- **Prefix S2 state classes with `S2`** — `FlatEntityState` → `S2FlatEntityState`, `NestedArrayEntityState` → `S2NestedArrayEntityState` (+ iterator), `TreeMapEntityState` → `S2TreeMapEntityState`. Purely mechanical naming sweep for engine-prefix symmetry with `S1FlatEntityState`. Tracked as a separate small follow-up change to keep this one focused on behaviour.
- **Rename the openspec capabilities** — `flat-entity-state` → `s2-flat-entity-state`, `nested-entity-state` → `s2-nested-entity-state`, `treemap-nested-state` → `s2-treemap-entity-state`. Capability rename is a larger openspec operation (moves the spec folder); can be batched with the S2 class rename follow-up if desired.
