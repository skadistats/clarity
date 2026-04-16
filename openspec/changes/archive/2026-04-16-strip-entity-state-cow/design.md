## Context

The just-archived `accelerate-flat-entity-state` change introduced two-axis owner-pointer COW on `FlatEntityState` (per-Entry + global refs + pointerSerializers) and a symmetric single-axis variant on `NestedArrayEntityState` (per-Entry + slab). Motivation at the time: packet-atomicity was believed to require per-entity snapshot-before-decode, and CP-1/CP-2 implemented COW to make that snapshot cheap.

CP-6 of the same change then pivoted to eager in-place mutation with `throw-aborts-replay` as the effective atomicity contract (see `entity-update-commit` capability). After CP-6, rollback is no longer a correctness requirement. The COW machinery still compiles, passes tests, and performs well on `FlatCopyBench`, but its raison d'être — cheap packet snapshots — no longer exists in the codebase.

What remains is a perf argument about deferring clone work on paths where `copy()` is followed by no mutation on the clone. Examination of actual `copy()` call sites shows that argument does not hold:

| Call site | Post-copy mutation on the clone? | Consequence for COW |
|---|---|---|
| `Entities.queueEntityCreate` / `queueEntityRecreate` (baseline → state) | Always, immediately (decode writes fill the state) | COW does 0 savings; clone happens in `readFieldsFast` |
| `ClientFrame.Capsule` (reset snapshot) | Never on the copy; live-side always mutates | COW relocates the clone into live-side `readFieldsFast` (same total work, worse location) |
| `clarity-analyzer ObservableEntityList.onUpdate/onCreate/onPropertyCountChange` | Never on the copy; live-side always mutates next tick | Same as Capsule, but at extreme frequency (every entity event) |

In all three cases, the "not mutated after copy" side is the snapshot (consumer), and the "always mutated after copy" side is the live state. COW places the clone cost on the live side's next write, inside the parser's inner loop, plus a fixed per-write owner-check tax across all writes.

## Goals / Non-Goals

**Goals:**
- Remove all owner-pointer COW machinery from `FlatEntityState` and `NestedArrayEntityState`.
- Preserve observable behavior of every method on both classes, in particular `copy()` returning a state fully independent from the original.
- Keep the hot-path decode sites (`applyMutation`, `write`, `decodeInto`, `readFieldsFast`) strictly simpler than before — every deletion is a reduction in code.
- Leave the `entity-update-commit` capability (throw-aborts-replay atomicity) unchanged.
- Leave the `state-mutation` capability unchanged — the `write` / `decodeInto` direct-write path is orthogonal to copy semantics.

**Non-Goals:**
- No changes to `TreeMapEntityState` (already eager), `ObjectArrayEntityState` (already eager; S1 handled by `accelerate-s1-flat-state`).
- No changes to the `EntityState` interface or any user-visible API.
- No changes to decode algorithms. Only owner-checks and owner-maintenance deletions.
- No new benchmarks beyond what `accelerate-flat-entity-state` already built. The existing `FlatCopyBench`, `EntityStateParseBench`, `FlatWriteBench`, and `-prof gc` runs cover the cost/benefit axes.

## Decisions

### D1. `copy()` walks the full state graph once at copy time

Both states gain a fully-eager `copy()` that:

- **FlatEntityState**: clones `pointerSerializers` via `Arrays.copyOf`, clones `refs` via `Arrays.copyOf(refs, refsSize)`, clones `freeSlots` via `Arrays.copyOf(freeSlots, freeSlotsTop)`, clones `rootEntry` (new `Entry(rootLayout, Arrays.copyOf(data, data.length))`), then walks every non-null slot in the cloned `refs` that currently holds an `Entry` and recursively replaces it with a fresh clone.

  Rationale: sub-Entries are reachable from the `refs` slab only (the flag+slot-index in `byte[]` is the only back-reference). A single slab walk covers the whole state graph — no FieldLayout traversal needed.

- **NestedArrayEntityState**: clones `pointerSerializers`, allocates a new `ArrayList<Entry>(entries.size())`, iterates; each non-null `Entry` is cloned with `new Entry(Arrays.copyOf(state, state.length))`; clones `freeEntries` deque.

  Rationale: slot indices remain valid across the clone; no further bookkeeping.

**Alternative considered**: keep COW but remove only the owner-check on the hot write path (pay the clone lazily on a sentinel). Rejected — the owner field still exists and each state carries baggage; no motivation survives after the analysis above.

**Alternative considered**: generation counter on the slab instead of owner pointer. Rejected — same category as owner pointer, same bookkeeping tax, no savings.

### D2. Delete all owner-maintenance helpers rather than stubbing

`rootEntryWritable()`, `makeWritable(Entry, Consumer<Entry>)`, `ensureRefsOwned()`, `ensureRefsModifiable()`, and their callers are removed outright. Replacement at each call site is direct pointer-chase or direct slab mutation.

The `Consumer<Entry>` slot-setter closures at SubState descent sites (`applyMutation`, `write`, `decodeInto`) vanish entirely — descent is `sub = (Entry) refs[slot]; current = sub; layout = sub.rootLayout; base = 0; continue;`.

Rationale: stubbing (e.g., `makeWritable` returning the entry unchanged) would preserve the call shape but keep compile-time baggage, JIT inlining noise, and a wrong mental model. Deletion is the test of done.

### D3. Transitive refs-slot release simplifies to straight mutation

Current spec for `FlatEntityState.Refs slot release is transitive` reads *"The walk SHALL only read `data` byte arrays (which may be shared under COW) and SHALL only mutate `this.refs` and `this.freeSlots` (per-copy after `ensureRefsModifiable`). It SHALL NOT modify any shared `data` content."*

Under eager copy, `data` is never shared — the walk reads and mutates `this.data`, `this.refs`, `this.freeSlots` freely. `ensureRefsModifiable` is deleted; the pre-condition vanishes.

Equivalent simplification applies to `NestedArrayEntityState.EntryRef slot release is transitive`.

### D4. COW scenarios in tests are deleted, not rewritten

`FlatEntityStateCowTest.deepNestedWriteClonesOnlyTouchedPath` and any NESTED_ARRAY equivalent describe COW semantics that no longer hold. Under eager copy there is no "touched path" — every path is eagerly cloned. The tests are deleted, not rewritten with weakened assertions.

Replacement coverage: the existing `Copy and modify independently` / `First write to a sub-Entry clones only that Entry` scenarios in the modified specs reduce to `Copy and modify independently` (no "only touched" qualifier) — covered by straightforward `copy()` + mutate + check-original-unchanged tests. These should already exist as non-COW-specific regression tests; any gap is filled with new plain-vanilla tests.

### D5. `FlatCopyBench` stays as the cost-side measurement

`FlatCopyBench` will regress (eager deep copy is strictly more work than aliasing). This is expected and welcome — the bench captures the trade being made. Keep it running and report the regression in RESULTS.md. The pass gate is `EntityStateParseBench`, not `FlatCopyBench`.

### D6. Analyzer workload does not need a dedicated bench

Per the `Why` analysis: analyzer copy-per-event has identical total clone work under eager vs. COW, just relocated. `EntityStateParseBench` (which does not mimic analyzer-style copying) is the right gate for the decode-side improvement; a dedicated analyzer-workload bench is not needed for this change. (An analyzer sim was considered and rejected because the algebra is sufficient — user agreed explicitly.)

The mechanical smoke test — downstream `clarity-analyzer` compiles and runs — remains in the acceptance checklist.

## Risks / Trade-offs

### R1. Per-tick allocation burst in high-snapshot workloads
- **Risk**: clarity-analyzer processes N entity updates per tick and calls `.copy()` on each. Under COW, the clone cost was spread across subsequent ticks; under eager, all N clones happen synchronously inside the `@OnEntityUpdated` dispatch loop. Larger per-tick alloc bursts may cause longer young-gen GC pauses in the JavaFX-bound analyzer.
- **Mitigation**: acceptable trade for parser-smooth inner loop. If the analyzer reports perceptible stutter post-change, revisit; current state of the analyzer is smooth under COW, and the *total* alloc is the same — the burst should be absorbed by young-gen capacity without promotion.

### R2. Parse bench regression
- **Risk**: `EntityStateParseBench` FLAT wall-clock regresses vs. archived-HEAD post-CP-6 (ebe8d2f). This would falsify the hypothesis that write-path simplification dominates copy-path cost increase.
- **Mitigation**: RESULTS.md reports the number. If the regression is > 2%, the change is reconsidered — options: revert, or find a narrower deletion (e.g. keep per-Entry owner but drop refsOwner). The hypothesis is strong enough that this outcome is unlikely; the plan is to land, measure, and accept a small regression if it appears, on the grounds of code simplicity alone (up to ~3%).

### R3. Capacity-change signaling correctness
- **Risk**: the current `applyMutation` / `write` / `decodeInto` paths compute `capacityChanged` based on old-flag vs new-flag transitions. Removing COW should not change these computations, but the deletion could accidentally drop a flag check.
- **Mitigation**: mechanical audit. Each call site's `capacityChanged` return value is computed from `data[flagPos]` or subroutine return values that are independent of ownership state. Removing ownership branches should not touch any flag logic. Test coverage via existing `FieldChanges` / `applyTo` round-trip tests and `MutationTraceBench` replay.

### R4. Slot-stability contract under eager copy
- **Risk**: current specs assert "slot stability across COW" (an Entry keeps its refs slot index when cloned by COW). Under eager copy, the clone is placed at the same slot index in the cloned `refs` array, so slot stability still holds. But the phrasing in specs changes: it's no longer "across COW", it's "across copy" (trivially, because the cloned array is a `copyOf` of the original).
- **Mitigation**: update spec wording. Slot stability is preserved; the invariant holds trivially under eager copy (indices map 1:1 from original to clone).

## Migration Plan

1. **`AbstractS2EntityState`**: delete `pointerSerializersOwner` field. Copy constructor clones `pointerSerializers` unconditionally (`Arrays.copyOf`).
2. **`FlatEntityState`**:
   - Delete `Entry.owner` field; delete `refsOwner`, `pointerSerializersOwner` fields on the outer class.
   - Delete `rootEntryWritable()`, `makeWritable(Entry, Consumer<Entry>)`, `ensureRefsOwned()` helpers.
   - Rewrite `copy()` as eager deep copy (see D1).
   - Remove `makeWritable` / `rootEntryWritable()` / `Consumer<Entry>` slot-setter calls from `applyMutation`, `write`, `decodeInto`. Descent becomes direct pointer-chase.
   - Remove `ensureRefsModifiable()` / `ensureRefsOwned()` calls from `allocateRefSlot`, `freeRefSlot`, ref-release walk.
3. **`NestedArrayEntityState`**:
   - Delete `Entry.owner`, `entriesOwner` fields.
   - Delete owner-enforcing helpers.
   - Rewrite `copy()` as eager deep copy.
   - Remove owner checks from `set`, `capacity`, `createEntryRef`, `clearEntryRef`, `releaseEntryRef`.
4. **Tests**:
   - Delete `FlatEntityStateCowTest.deepNestedWriteClonesOnlyTouchedPath` and any NESTED_ARRAY-equivalent owner-pointer-specific test.
   - Ensure `FlatEntityStateCowTest` (if the class stays) is renamed or pruned; COW-specific naming is misleading after this change.
5. **Specs**: apply delta per `specs/flat-entity-state/spec.md` and `specs/nested-entity-state/spec.md` (this change's delta files).
6. **Benches**: run `EntityStateParseBench`, `FlatWriteBench`, `FlatCopyBench`, all with `-prof gc`. Also run `MutationTraceBench`. Record in `RESULTS.md`.
7. **Acceptance**:
   - `./gradlew build` — green.
   - `openspec validate strip-entity-state-cow --strict` — passes.
   - `:dev:dtinspectorRun` — compiles (no runtime GUI needed).
   - `clarity-analyzer` (composite-build) — compiles (no runtime GUI needed unless user requests).
   - `:repro:issue289Run`, `:repro:issue350Run` — pass.

**Rollback strategy**: the change is a pure deletion; if `EntityStateParseBench` shows an unacceptable regression (> 2-3%), revert the commit range. No data migration or consumer-code change is involved, so rollback is clean.

## Open Questions

**Q1. Do we rename `FlatEntityStateCowTest`?**
The class may contain non-COW tests that are still valuable. Plan: inspect at implementation time; if most tests survive deletion of the COW-specific scenario, rename to `FlatEntityStateCopyTest` or `FlatEntityStateIndependenceTest`. If the class collapses to one or two tests after pruning, fold them into an existing test class.

**Q2. Should `EntityState.copy()` Javadoc tighten the contract?**
Current (implicit): "returns a copy whose mutations do not affect the original." Under COW that was post-first-write-true. Under eager it's immediately-true. Either interpretation satisfies the same Javadoc, so no change required — but a one-line clarification ("the returned state is fully independent; no aliasing with the original") is welcome if the file is touched.

**Q3. Any listener implications?**
`MutationListener.onBirthCopy(newState, sourceState)` is called by `Entities.copyState`. Listeners currently receive a reference to a COW-shared state; post-change they receive a reference to a fully independent state. No listener (checked: `MutationRecorder`, `WriteCounterListener`) relies on the aliasing — they only use the state reference to key metadata or dump values. No listener changes needed.
