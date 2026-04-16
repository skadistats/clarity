# Results — `strip-entity-state-cow`

Baseline from `accelerate-flat-entity-state/RESULTS.md` (post-CP-6 HEAD ≈ `ebe8d2f`). Post-change measurement on branch `next` (current sha `513a8e4`), same Dota 2 replay (`replays/dota/s2/340/8168882574_1198277651.dem`), same JMH config (3 warmup + 10 measurement iterations, single-shot timing, `-prof gc`). Full raw output: `bench-results/2026-04-16_170748_next-513a8e4/`.

## Headline: `EntityStateParseBench`

| Impl | Baseline wall-clock | Post wall-clock | Δ | Baseline alloc/op | Post alloc/op | Δ |
|---|---|---|---|---|---|---|
| **FLAT** | 1646 ms | **1619.1 ms** | **-1.6%** | 9.11 GB | **8.86 GB** | **-2.7%** |
| NESTED_ARRAY | 1730 ms | 1734.1 ms | +0.2% (noise) | — | 9.14 GB | — |
| TREE_MAP | 2057 ms | 2055.6 ms | -0.1% (noise) | — | 9.27 GB | — |

FLAT is the only impl that was decisively affected by this change — which aligns with the design hypothesis. FLAT had the heaviest COW bookkeeping: three independent owner axes (per-Entry, refs slab, pointerSerializers), `makeWritable` with slot-setter `Consumer<Entry>` closures on every SubState descent, `ensureRefsOwned`/`ensureRefsModifiable` guards on every ref-slab mutation. Removing all of that shortens the decode inner loop across every primitive, inline-string, ref, and substate write — measurable as a ~1.6% full-replay-parse improvement and ~2.7% alloc reduction.

NESTED_ARRAY had a lighter COW surface (per-Entry `owner` + slab-level `entriesOwner`, no slot-setter closures). Stripping it was neutral on this bench, which matches intuition — the per-descent check was cheap enough that its removal doesn't show.

TREE_MAP was never COW'd. Its delta is pure noise.

## Write-hot-path simplification (`FlatEntityState`)

Removed from every descent of `applyMutation` / `write` / `decodeInto`:

- `rootEntryWritable()` (one owner check + conditional clone at root entry per call).
- `makeWritable(sub, e -> refs[slot] = e)` at every SubState descent (one owner check + conditional clone + closure).
- `ensureRefsOwned()` before every `allocateRefSlot` / `freeRefSlot`.
- `ensurePointerSerializersOwned()` before every `pointerSerializers[pointerId] = …`.

Deleted helpers: `rootEntryWritable()`, `makeWritable(Entry, Consumer<Entry>)`, `ensureRefsOwned()`, `ensurePointerSerializersOwned()` (on `AbstractS2EntityState`). Deleted fields: `Entry.owner`, `refsOwner`, `pointerSerializersOwner`. Import `java.util.function.Consumer` removed.

## Eager `copy()` construction

`FlatEntityState` — `copy()` now eagerly allocates:
- `pointerSerializers.clone()` via super.
- `refs.clone()` + inline deep-clone pass: for each `Entry` slot in `refs[0..refsSize-1]`, a fresh `Entry(rootLayout, data.clone())` replaces the shared reference. Non-Entry slot values (defensive for any `FieldLayout.Ref` holding non-String objects — currently not produced after inline-string migration) remain shared by reference (they are immutable user values).
- `freeSlots.clone()`.
- `rootEntry` → `new Entry(rootLayout, data.clone())`.

`NestedArrayEntityState` — `copy()` now eagerly allocates:
- `pointerSerializers.clone()` via super.
- New `ArrayList<Entry>(size)`; for each non-null `Entry e` in `other.entries`, appends `new Entry(e.state.clone())` (fresh Entry wrapper bound to `this` via non-static inner-class semantics with cloned `state[]`). Null slots preserved.
- `freeEntries` cloned only if non-empty (see below).

## Bonus simplification — lazy `freeEntries` (NestedArrayEntityState)

Applied during implementation review (user-initiated). `freeEntries` is now lazy-allocated rather than eagerly allocated in the main constructor:

- Main constructor no longer calls `freeEntries = new ArrayDeque<>()`.
- Copy constructor skips cloning if `other.freeEntries` is null or empty: `freeEntries = other.freeEntries == null || other.freeEntries.isEmpty() ? null : new ArrayDeque<>(other.freeEntries)`.
- `clearEntryRef` routes through `ensureFreeEntries()` helper which lazy-allocates on first release.
- `createEntryRef` null-guards: `if (freeEntries == null || freeEntries.isEmpty()) append` else reuse.
- `freeSlotCount()` returns 0 when freeEntries is null.

Most entities in a replay parse never have a slot freed (never shrink vectors, never swap pointers mid-lifetime). They now carry a null `freeEntries` throughout their lifetime — one `ArrayDeque` allocation avoided per main-construction and per copy.

Correctness verified: `entries` and `freeEntries` are per-copy under eager copy, so slot index N is a local name in each state. Cloning freeEntries between A and B (when non-empty) is safe — each state's "free slot N" corresponds to its own `entries.get(N) == null`. Added regression test `freedSlotReuseAfterCopyIsIndependent` in `NestedArrayEntityStateCopyTest` that shrinks a vector on the original, copies, then grows back on both sides with distinct values and verifies no cross-contamination.

## Test churn

| File | Action |
|---|---|
| `FlatEntityStateCowTest` | **Deleted**; replaced by `FlatEntityStateCopyTest` (4 value-correctness tests: primitive, deep-nested, inline-string, switch-pointer independence). |
| `NestedArrayEntityStateCowTest` | **Deleted**; replaced by `NestedArrayEntityStateCopyTest` (5 value-correctness tests including the kept `tracedWritesOnCopyMatchApplyingToFresh` and new `freedSlotReuseAfterCopyIsIndependent`). |
| `FlatEntityStateDecodeIntoTest` | 3 tests (decodeInto + inline-string + pointerSerializers) had their `assertSame`/`assertNotSame` COW-identity checks stripped, renamed to `*IsIndependent` / `*LeavesOriginalUnchanged`. |
| `FlatEntityStateInlineStringTest` | 1 test (`copyThenInlineStringWriteClonesRootOnly`) had COW-identity checks stripped, renamed to `inlineStringWriteAfterCopyIsIndependent`. |
| `Entities.java` and related non-state processor code | Unchanged. |

## Scope notes

`FlatWriteBench`, `FlatCopyBench`, `MutationTraceBench`, `DecodeIntoBench`, and `InlineStringBench` were **not** re-run for this change. Their expected directions follow mechanically from the deletions:

- `FlatWriteBench` — expected to improve (fewer owner checks per write); the ~2.7% alloc reduction visible in `EntityStateParseBench` already bundles this effect.
- `FlatCopyBench` — expected to regress (eager deep copy > aliasing). The cost side of the trade; end-to-end bench confirms the wall-clock still comes out ahead.
- `MutationTraceBench` — no semantic change (mutations still produce the same state graph); skipped.
- `DecodeIntoBench` / `InlineStringBench` — no change in the decode-into path itself; skipped.

`:repro:issue289Run` and `:repro:issue350Run` both pass on the modified tree.

## Acceptance checklist

- [x] `./gradlew build` — all tests green.
- [x] `./gradlew test` — green after test churn (COW-identity assertions stripped or deleted where they no longer hold).
- [x] `./gradlew :repro:issue289Run` — 5000 iterations completed cleanly.
- [x] `./gradlew :repro:issue350Run` — 107358 ticks processed cleanly.
- [x] `./gradlew :dev:compileJava` — clean compile.
- [x] Downstream `clarity-analyzer` (`next` branch, composite build) — clean compile.
- [x] `openspec validate strip-entity-state-cow --strict` — passes.
- [x] `EntityStateParseBench` FLAT ≥ neutral gate — passed (-1.6% wall-clock, -2.7% alloc).

## Follow-ups scoped to successor changes

- **`accelerate-s1-flat-state`** — port `ObjectArrayEntityState` / `S1FieldReader` to a flat byte[] layout with inline strings + `decodeInto`, built directly on the eager-copy model established here. The S1 refs-slab path audit (§0.6 of the archived `accelerate-flat-entity-state`) remains valid.
