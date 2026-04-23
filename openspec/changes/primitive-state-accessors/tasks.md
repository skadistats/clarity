## 1. Primitive getters on EntityState

- [x] 1.1 Add engine-agnostic static dispatchers `EntityState.getInt(state, fp)`, `getLong`, `getFloat`, `getObject` — mirroring the existing static `getValueForFieldPath(state, fp)` shape that casts to the engine-specific `FieldPath` subtype.
- [x] 1.2 Add abstract methods `getInt(S1FieldPath)` / `getLong` / `getFloat` / `getObject` on `S1EntityState` and corresponding `S2FieldPath`-typed methods on `S2EntityState`.
- [x] 1.3 Implement on `S2FlatEntityState` / `S1FlatEntityState`: read the primitive slot directly from the backing `byte[]` via VarHandle; no boxing.
- [x] 1.4 Implement on nested/tree impls (`S2NestedArrayEntityState`, `S2NestedEntityState`, `S2TreeMapEntityState`, `S1ObjectArrayEntityState`): navigate to the stored wrapper and return `.intValue()` / `.longValue()` / `.floatValue()`. No new allocation at the accessor boundary.
- [x] 1.5 Ensure `getValueForFieldPath` remains behavior-compatible: same null/default semantics, same return type. Primitive-getter usage is additive — `getValueForFieldPath` does not need to layer over `getInt` (different storage paths want different read shapes).

## 2. Entity-level delegates

- [x] 2.1 Add `int getInt(FieldPath)`, `long getLong(FieldPath)`, `float getFloat(FieldPath)`, `Object getObject(FieldPath)` on `Entity` as thin delegates to the underlying `EntityState` (via the static dispatchers).
- [x] 2.2 Name-resolving overloads: `getInt(String name)`, etc. that resolve the `FieldPath` once and delegate.

## 3. StateDelta type

- [x] 3.1 Create `StateDelta` interface in `skadistats.clarity.model.state`: `FieldPath[] fields()`, `getInt`, `getLong`, `getFloat`, `getObject`.
- [x] 3.2 Create concrete `SparseStateDelta` implementation: holds parallel arrays sized to the captured count, plus an `int[]` slot-index table keyed by `FieldPath` position within the input array.
- [x] 3.3 Unknown-field-path access returns zero / null (documented). No exception.

## 4. captureChanged on EntityState

- [x] 4.1 Add engine-agnostic static dispatcher `EntityState.captureChanged(state, fps, num)`; add abstract methods with typed `FieldPath` on `S1EntityState` / `S2EntityState`.
- [x] 4.2 Implement on the flat impls: one pass over `fps[0..num]`, dispatch by field's primitive type using existing layout metadata, write into the matching slot array of the delta. No boxing for `int` / `long` / `float` fields.
- [x] 4.3 Implement on nested/tree impls: one pass over `fps[0..num]`, read the stored wrapper, put the reference into the delta's `Object[]` slot (plus type tag). Unbox happens lazily on delta read — no new allocation during capture.

## 5. Merge primitives (applyFrom, applyAll)

- [x] 5.1 Add engine-agnostic static dispatchers `EntityState.applyFrom(state, delta, fp)` and `EntityState.applyAll(state, delta)`; add corresponding abstract methods with typed `FieldPath` on `S1EntityState` / `S2EntityState`.
- [x] 5.2 Implement on each concrete state impl: read the typed primitive (or object ref) from `delta`, write into the matching slot. Flat impls write inline bytes; nested/tree impls write the wrapper reference (no new boxing).
- [x] 5.3 `applyAll` walks `delta.fields()` internally and dispatches per-field; MAY be optimized to a single storage-shape-aware pass where worthwhile.

## 6. Tests

- [x] 6.1 Unit: primitive getters on `EntityState` return the value written by a decode-path mutation (round-trip), on every concrete impl.
- [x] 6.2 Unit: `captureChanged` produces a `StateDelta` whose getters return the same values as the live state at capture time.
- [x] 6.3 Unit: `applyFrom` merges a delta into a target state such that subsequent reads on the target match the delta values. Fields not in the delta are untouched.
- [x] 6.4 Unit: unknown-field-path access on a delta returns zero/null per documented contract.
- [x] 6.5 Unit: wrong-primitive-type access on a delta (e.g., `getInt` on a float field) returns zero, does not throw.
- [ ] 6.6 Allocation regression: a `getInt` call on a fresh state allocates zero objects (JOL or allocation-profiler assertion, or a JMH -prof gc run that shows it).

## 7. Downstream smoke

- [x] 7.1 Rebuild clarity-analyzer against the local clarity sibling checkout; confirm it still compiles and starts (per `feedback_dtinspector_analyzer_compile_only` — compile, don't run GUI without briefing).
- [ ] 7.2 Confirm the companion `sparse-state-delta-updates` change in clarity-analyzer can consume the new API without further clarity-side changes.

## 8. Benchmark — analyzer-shaped consumer (go/no-go gate)

This benchmark answers whether the sparse-delta story has any real prospect
of improving analyzer scrub behavior. Do it **before** committing to the
companion `sparse-state-delta-updates` change.

- [x] 8.1 In `clarity-bench`, add a JMH benchmark that registers a tiny
      analyzer-shaped processor: listens on `@OnEntityCreated`,
      `@OnEntityUpdated`, `@OnEntityPropertyCountChanged`; on each event
      calls `entity.getState().copy()` and retains the copy in an array
      indexed by entity index (mirroring `ObservableEntityList.onUpdate`).
      Headless `SimpleRunner`, no FX dependency. Deterministic; not
      real-time — full Dota replay runs in seconds-to-minutes. Wired as
      dispatch variant `AnalyzerCopy` in `v5.0.0/V500Adapter`.
- [x] 8.2 Baseline run: current API (`state.copy()` per update). Recorded
      one Dota S2 replay (`dota/s2/normal/1560289528.dem`, 40 MB):
      Baseline = 740 MB alloc/op; AnalyzerCopy = 3.38 GB alloc/op (delta
      attributable to `copy()` ≈ +2.64 GB, ~78% of total).
- [ ] 8.3 Treatment run: swap `state.copy()` for
      `state.captureChanged(fieldPaths, num)` and hold the resulting
      `StateDelta`s. Same replays, same `-prof gc`. Deferred — go/no-go
      already answered by §8.2 baseline (78% ≫ 5% threshold).
- [ ] 8.4 Also capture an alloc flamegraph (async-profiler `-e alloc -t`) on
      the baseline — confirms `S2FlatEntityState.copy` / `Entry.data.clone`
      are the line items, so we know where the bytes come from. Deferred
      with §8.3.
- [x] 8.5 **Decision gate**: baseline `state.copy()` allocation = ~78% of
      total bytes — far above the 5% threshold. Companion change
      `sparse-state-delta-updates` is green-lit.
- [ ] 8.6 Record numbers in `bench-results/` following existing convention;
      cross-link from this change's archive entry **and** from the
      analyzer companion change's proposal.

## 9. Read-side throughput benchmark (optional)

- [ ] 9.1 Extend a JMH benchmark exercising read-heavy consumer patterns
      (per-tick hero-state aggregation across all entities of a class) to
      compare `getValueForFieldPath` vs `getInt`. Measure `allocs/op` and
      wall-clock before/after. Purely to validate the primitive-read API
      claim; not a go/no-go for the change.

## 10. Documentation

- [ ] 10.1 Update the relevant Javadoc on `EntityState` / `Entity` to surface the primitive accessors alongside `getValueForFieldPath`.
- [ ] 10.2 Add a short section to any consumer-facing API doc (if present) noting the primitive read contract and the `captureChanged` / `applyFrom` pattern for cross-thread snapshots.
