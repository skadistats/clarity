## 1. Primitive getters on State

- [ ] 1.1 Add `int getInt(FieldPath)`, `long getLong(FieldPath)`, `float getFloat(FieldPath)`, `Object getObject(FieldPath)` to the `State` interface.
- [ ] 1.2 Implement on the concrete flat state: read the primitive slot directly; no boxing.
- [ ] 1.3 Ensure `getValueForFieldPath` now delegates through the primitive path (single source of truth) — boxes once on the generic-return edge, never inside storage.
- [ ] 1.4 Verify that callers of `getValueForFieldPath` and `Entity.getProperty` still see identical behavior (same boxing, same null/default semantics).

## 2. Entity-level delegates

- [ ] 2.1 Add `int getInt(FieldPath)`, `long getLong(FieldPath)`, `float getFloat(FieldPath)`, `Object getObject(FieldPath)` on `Entity` as thin delegates to `State`.
- [ ] 2.2 Name-resolving overloads: `getInt(String name)`, etc. that resolve the `FieldPath` once and delegate.

## 3. StateDelta type

- [ ] 3.1 Create `StateDelta` interface in `skadistats.clarity.model.state`: `FieldPath[] fields()`, `getInt`, `getLong`, `getFloat`, `getObject`.
- [ ] 3.2 Create concrete `SparseStateDelta` implementation: holds parallel arrays sized to the captured count, plus an `int[]` slot-index table keyed by `FieldPath` position within the input array.
- [ ] 3.3 Unknown-field-path access returns zero / null (documented). No exception.

## 4. captureChanged on State

- [ ] 4.1 Add `StateDelta captureChanged(FieldPath[] fps, int num)` to the `State` interface.
- [ ] 4.2 Implement on the concrete flat state: one pass over `fps[0..num]`, dispatch by field's primitive type using the existing field metadata, write into the matching slot array.
- [ ] 4.3 Confirm no boxing on the capture path for `int` / `long` / `float` fields.

## 5. applyFrom merge primitive

- [ ] 5.1 Add `void applyFrom(StateDelta delta, FieldPath fp)` to the `State` interface.
- [ ] 5.2 Implement on the concrete flat state: read the typed primitive from `delta`, write into the matching flat slot. Object fields write the reference.

## 6. Tests

- [ ] 6.1 Unit: primitive getters on State return the value written by a decode-path mutation (round-trip).
- [ ] 6.2 Unit: `captureChanged` produces a `StateDelta` whose getters return the same values as the live state at capture time.
- [ ] 6.3 Unit: `applyFrom` merges a delta into a target state such that subsequent reads on the target match the delta values. Fields not in the delta are untouched.
- [ ] 6.4 Unit: unknown-field-path access on a delta returns zero/null per documented contract.
- [ ] 6.5 Unit: wrong-primitive-type access on a delta (e.g., `getInt` on a float field) returns zero, does not throw.
- [ ] 6.6 Allocation regression: a `getInt` call on a fresh state allocates zero objects (JOL or allocation-profiler assertion, or a JMH -prof gc run that shows it).

## 7. Downstream smoke

- [ ] 7.1 Rebuild clarity-analyzer against the local clarity sibling checkout; confirm it still compiles and starts (per `feedback_dtinspector_analyzer_compile_only` — compile, don't run GUI without briefing).
- [ ] 7.2 Confirm the companion `sparse-state-delta-updates` change in clarity-analyzer can consume the new API without further clarity-side changes.

## 8. Benchmark (optional but recommended)

- [ ] 8.1 Add or extend a JMH benchmark that exercises read-heavy consumer patterns (e.g., per-tick hero-state aggregation across all entities of a class). Measure allocs/op before and after.
- [ ] 8.2 Record numbers in `bench-results/` following the existing convention; cross-link from this change's archive entry.

## 9. Documentation

- [ ] 9.1 Update the relevant Javadoc on `State` / `Entity` to surface the primitive accessors alongside `getValueForFieldPath`.
- [ ] 9.2 Add a short section to any consumer-facing API doc (if present) noting the primitive read contract and the `captureChanged` / `applyFrom` pattern for cross-thread snapshots.
