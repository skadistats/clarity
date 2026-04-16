## Why

The owner-pointer copy-on-write (COW) machinery introduced in `accelerate-flat-entity-state` (archived 2026-04-16) was justified by a packet-atomicity requirement that the same change then eliminated: CP-6 pivoted to eager in-place mutation with throw-aborts-replay as the effective atomicity contract. Rollback is no longer a correctness requirement, so COW survives only on its perf story — deferring clone work until first write after `copy()`.

That perf story does not hold up under the dominant `copy()` workloads:

- **Baseline CREATE/RECREATE** (parse hot path) always mutates the fresh copy — the clone is paid in the parser's inner loop anyway, just one write later.
- **`ClientFrame.Capsule` (reset snapshot)** is read-only from the consumer side, but the live entity side always mutates next — same story.
- **clarity-analyzer snapshots** — `ObservableEntityList.onUpdate`/`onCreate`/`onPropertyCountChange` call `entity.getState().copy()` on **every** entity event. Under COW, every snapshot evicts live-state ownership and pushes the next write's clone into `readFieldsFast`. Total clone work is unchanged vs. eager; COW just relocates it into the hot path and charges a per-write ownership check on top.

Meanwhile COW imposes a fixed tax on every write: `rootEntryWritable()` at the root, `makeWritable(sub, e -> refs[slot] = e)` on each SubState descent, per-descent `Consumer<Entry>` slot-setter closures (even if JIT can usually eliminate them), plus `ensureRefsOwned` / `pointerSerializersOwner` checks on the ref and pointer-switch paths. On a 3-level-deep field write, that's 3× owner checks per decode — every decode, every entity, every packet.

Stripping COW is a monotonic simplification: identical total clone work, less bookkeeping per write, smaller and straighter hot path, and the code that's hardest to reason about (`Entry.owner`, two-axis owner evolution, slot-setter closures) goes away.

## What Changes

### FLAT: eager deep copy
- **`FlatEntityState.copy()` allocates a fully-independent state**: clone `rootEntry` (new `Entry` wrapper with `Arrays.copyOf(data)`), clone `refs` slab (`Arrays.copyOf` to `refsSize`) and recursively clone every sub-`Entry` reachable through it, clone `freeSlots`, clone `pointerSerializers`.
- **Remove** `Entry.owner`, `refsOwner`, `pointerSerializersOwner` fields.
- **Remove** `rootEntryWritable()`, `makeWritable(Entry, Consumer<Entry>)`, `ensureRefsOwned()`, and any other owner-enforcing helpers.
- **Remove** the `Consumer<Entry>` slot-setter plumbing from every descent site (`applyMutation`, `decodeInto`, `write`, and `releaseRefSlot`/`freeRefSlot` walks that currently thread a slot-setter through the recursion for lazy COW).
- **Simplify** descent sites to straight writes: `applyMutation`, `write`, `decodeInto` no longer branch on owner state; they mutate `current.data` directly.
- **Simplify** SubState descent to direct pointer-chase: `sub = (Entry) refs[slot]`, no `makeWritable`.
- **Simplify** ref slab mutation: `refs` and `freeSlots` are owned; no `ensureRefsOwned` before `allocateRefSlot` / `freeRefSlot`.
- **Simplify** the transitive ref-release walk (`Requirement: Refs slot release is transitive`): no more "reads `data` which may be shared" / "mutates only per-copy refs and freeSlots" distinction — everything is per-copy.

### NESTED_ARRAY: eager deep copy
- **`NestedArrayEntityState.copy()` allocates a fully-independent state**: clone `entries` list (new `ArrayList<>(entries.size())`, then clone each non-null `Entry` with a fresh `state[]` via `Arrays.copyOf`), clone `freeEntries` deque, clone `pointerSerializers`.
- **Remove** `Entry.owner`, `entriesOwner`, `pointerSerializersOwner` fields.
- **Remove** owner-enforcing helpers and ownership checks from `set`, `capacity`, `sub`, `createEntryRef`, `clearEntryRef`, `releaseEntryRef`.
- **Simplify** the transitive `EntryRef` release walk — since `state[]` arrays are never shared, the "reads shared `state[]`, mutates only per-copy `entries`/`freeEntries`" discipline collapses to straight mutation.

### AbstractS2EntityState
- **Remove** `pointerSerializersOwner` field and its owner-maintenance discipline. `pointerSerializers` is owned by construction after `copy()` eagerly clones it.

### TREE_MAP
- Unchanged. `TreeMapEntityState.copy()` was already eager (`state.clone()` in the copy constructor). No COW to strip.

### ObjectArrayEntityState (S1)
- Unchanged. Already eager (`System.arraycopy`). Out of scope — `accelerate-s1-flat-state` handles S1 wholesale.

### Non-goals
- No changes to the decode path (`readFieldsFast`, `decodeInto`, `write`). Hot-path correctness is preserved by construction — removing owner checks is a pure deletion at each site.
- No changes to `EntityState` interface shape.
- No changes to the `state-mutation` or `entity-update-commit` capabilities — throw-aborts-replay atomicity is orthogonal to copy semantics.
- No changes to public `Entity` / example-code API.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `flat-entity-state`: `copy()` becomes an eager deep copy. Two-axis COW machinery (`Entry.owner`, `refsOwner`, `pointerSerializersOwner`, `makeWritable`, `rootEntryWritable`, `ensureRefsOwned`, slot-setter closures on descent) is removed. Traversal simplifies to straight `current.data` mutation.
- `nested-entity-state`: `copy()` becomes an eager deep copy. Owner-pointer machinery (`Entry.owner`, `entriesOwner`, `pointerSerializersOwner`) is removed. Transitive `EntryRef` release walks no longer distinguish shared `state[]` from per-copy slab.

## Impact

### Code
- `skadistats.clarity.model.state.FlatEntityState` — `copy()` rewritten; owner fields deleted; descent helpers removed; slot-setter `Consumer<Entry>` plumbing deleted; `Entry` inner class loses `owner` field.
- `skadistats.clarity.model.state.NestedArrayEntityState` — `copy()` rewritten; owner fields deleted; owner-enforcing helpers removed; `Entry` inner class loses `owner` field.
- `skadistats.clarity.model.state.AbstractS2EntityState` — `pointerSerializersOwner` field deleted; copy constructor clones `pointerSerializers` directly.
- `skadistats.clarity.model.state.ObjectArrayEntityState` — unchanged.
- `skadistats.clarity.model.state.TreeMapEntityState` — unchanged.

### Performance expectations (to be validated by benches)
- **Write hot path** (parse): modestly faster — each descent loses an owner check; SubState descent loses the `makeWritable` branch; ref-slab writes lose `ensureRefsOwned`. Expected: small but measurable `EntityStateParseBench` improvement for FLAT and NESTED_ARRAY.
- **`copy()` micro**: slower in isolation — eager deep copy replaces a one-wrapper aliasing operation. Expected: `FlatCopyBench` and equivalent `NestedArrayCopyBench` regress meaningfully. This is the trade side.
- **End-to-end GC**: allocation shifts from deferred (COW fork on first write) to up-front (deep copy at `copy()` call). Total bytes allocated should be within noise; per-tick alloc *bursts* may grow in analyzer workloads.
- **Decision gate**: `EntityStateParseBench` wall-clock for FLAT and NESTED_ARRAY must be ≥ neutral vs. pre-change baseline (HEAD of archived `accelerate-flat-entity-state`). `-prof gc` total alloc within ±10%.

### Benches / tests
- `FlatCopyBench` — existing bench, will regress; kept as the "cost side" measurement.
- `EntityStateParseBench` — existing bench; must not regress (target: small improvement).
- `FlatWriteBench` — existing bench; expected to improve (fewer owner checks on the write inner loop).
- `FlatEntityStateCowTest.deepNestedWriteClonesOnlyTouchedPath` and equivalent NESTED_ARRAY COW tests — **deleted** (assertions describe COW semantics that no longer hold).
- Full suite (`./gradlew build`) must stay green. `MutationTraceBench` replay round-trips must still succeed. `dtinspector` and downstream `clarity-analyzer` must compile and run.

### Consumers
- No user-visible API changes. `EntityState.copy()` contract tightens (the return is fully independent from construction rather than after-first-write), but no consumer relied on the prior aliasing semantics — by definition, observing aliasing would have been a correctness bug for the consumer.

### Successor
- `accelerate-s1-flat-state` lands after this change, built directly on the eager-copy model.
