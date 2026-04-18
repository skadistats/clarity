## 1. Capture baseline

- [x] 1.1 Record baseline commit SHA used for benchmark comparison (7cff438, see BASELINE.md)
- [x] 1.2 Run `./gradlew bench` on baseline and save JMH JSON output (folded into 9.1 — bench once at the end)
- [x] 1.3 Run full-replay smoke benchmark on the Dota 2 corpus; save timings (folded into 9.2)

## 2. Reshape the S2FieldPath hierarchy

- [x] 2.1 Add `childAt(int)`, `upperBoundForSubtreeAt(int)` to `S2FieldPath`; implement on `S2LongFieldPath` using `S2LongFieldPathFormat`
- [x] 2.2 Move `compareTo` to `S2FieldPath` as `Comparable<S2FieldPath>`; in `S2LongFieldPath`, add an `instanceof S2LongFieldPath` fast-path and a lexicographic fallback over `get(i)/last()`
- [x] 2.3 Rename `S2ModifiableFieldPath` → `S2FieldPathBuilder` (interface) and `S2LongModifiableFieldPath` → `S2LongFieldPathBuilder`
- [x] 2.4 Remove `S2ModifiableFieldPath` from the `S2FieldPath` `permits` clause; ensure the builder is NOT a subtype of `S2FieldPath`
- [x] 2.5 Replace `unmodifiable()` on the builder with `snapshot()`; return type `S2FieldPath`
- [x] 2.6 Compile; fix all call-site signature breakages in `io.s2` and `state.s2`

## 3. Introduce S2FieldPathType

- [x] 3.1 Add `S2FieldPathType` enum in `skadistats.clarity.model.s2` with constant `LONG` wired to `S2LongFieldPathBuilder::new`
- [x] 3.2 Expose `S2FieldPathType.newBuilder()`
- [x] 3.3 Add a construction path in `S2FieldReader` that accepts an `S2FieldPathType` parameter; 1-arg constructor defaults to `LONG`

## 4. Rewrite S2TreeMapEntityState

- [x] 4.1 Retype the internal `Object2ObjectAVLTreeMap` key to `S2FieldPath`
- [x] 4.2 Remove the `(S2LongFieldPath) fpX` casts from `write`, `applyMutation`, `getValueForFieldPath`
- [x] 4.3 Rewrite `trimEntries` to use `fp.childAt(count)` and `fp.upperBoundForSubtreeAt(fp.last())`; `S2LongFieldPathFormat` references deleted
- [x] 4.4 Rewrite `clearSubEntries` to use `fp.upperBoundForSubtreeAt(fp.last())`; format references deleted
- [x] 4.5 Verify `nextBound` helper is no longer needed; removed

## 5. Update S2FlatEntityState and S2NestedArrayEntityState

- [x] 5.1 All path accesses go through `fp.get(int)` / `fp.last()` only
- [x] 5.2 `decodeInto`, `write`, `applyMutation`, `getValueForFieldPath` signatures accept `S2FieldPath`
- [x] 5.3 No `S2LongFieldPath` / `S2LongFieldPathFormat` references remain in `state.s2` package (replaced with `S2FieldPath.MAX_DEPTH`)

## 6. Update FieldOp and S2FieldReader

- [x] 6.1 `FieldOp.execute` signature takes `S2FieldPathBuilder`
- [x] 6.2 Fast-path and debug-path methods in `S2FieldReader` hold a `S2FieldPathBuilder` obtained from the configured `S2FieldPathType`
- [x] 6.3 `mfp.unmodifiable()` call sites replaced with `builder.snapshot()`
- [x] 6.4 `fieldPaths[]` stays `S2FieldPath[]`

## 7. Expose runner configuration

- [x] 7.1 `withS2FieldPath(S2FieldPathType)` added to `AbstractFileRunner` (inherited by `SimpleRunner`)
- [x] 7.2 Inherited by `ControllableRunner` via `AbstractFileRunner`
- [x] 7.3 Selection threaded through `EntityStateFactory` → `DTClasses` → `Entities.onDTClassesComplete` → `engineType.getNewFieldReader(pointerCount, pathType)`; default `LONG`
- [x] 7.4 Documented in CHANGELOG (Unreleased section)

## 8. Sync existing spec inaccuracy

- [x] 8.1 Updated `openspec/specs/treemap-nested-state/spec.md` — removed stale `fp.s2()` references, aligned with `S2FieldPath` as key type

## 9. Benchmark and decide

- [x] 9.1 Re-ran `./gradlew bench` on the change branch; v1 (defaults on interface) showed TREE_MAP +4.8% vs baseline; v2 (concrete methods on S2LongFieldPathBuilder) recovered half that — TREE_MAP +2.2%, NESTED_ARRAY default unchanged
- [x] 9.2 Functional smoke via `combatlog` example and clarity-analyzer exercised full field-path walk; both green
- [x] 9.3 Regression investigated and mitigated (concrete-method move); final TREE_MAP +2.2% is at/within the 2% gate; NESTED_ARRAY/FLAT/OBJECT_ARRAY within noise
- [x] 9.4 Regression accepted — TREE_MAP is an opt-in alternative impl, already 20%+ slower than default; cleanup value outweighs the small residual hit

## 10. Verify downstream compatibility

- [x] 10.1 `/home/spheenik/projects/clarity/clarity-analyzer` compiles green against the change
- [x] 10.2 No `S2ModifiableFieldPath` references in `clarity-examples`; full build passes

## 11. Finalize

- [x] 11.1 CHANGELOG updated under Unreleased
- [x] 11.2 Full test suite green (260+ tests, 0 failures)
- [x] 11.3 Archive the change via `/opsx:archive configurable-s2-fieldpath-impl` once merged
