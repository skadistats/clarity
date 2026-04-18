## 1. Implement intern in S2LongFieldPath

- [x] 1.1 Add private static `volatile Long2ObjectOpenHashMap<S2LongFieldPath> INTERN` initialised to a pre-sized empty map (~8192).
- [x] 1.2 Add static `S2LongFieldPath of(long id)`: volatile load → fastutil `get` → return on hit, else delegate to `ofSlow`.
- [x] 1.3 Add private static `synchronized S2LongFieldPath ofSlow(long id)`: re-check inside the lock, then COW-copy the map, insert, volatile-publish.
- [x] 1.4 Make the constructor private.
- [x] 1.5 Replace `equals` body with `return this == o;` and add a single-line comment noting the intern invariant.
- [x] 1.6 Leave `hashCode` and `compareTo` unchanged.

## 2. Route construction through the factory

- [x] 2.1 Change `S2LongFieldPathBuilder.snapshot()` to `return S2LongFieldPath.of(id);`.
- [x] 2.2 Grep the codebase for any remaining `new S2LongFieldPath(` usages and either remove them or route through `of(long)`. (Constructor going private will surface them at compile time.)

## 3. Verify behaviour

- [x] 3.1 Build the project; resolve any callers exposed by the constructor visibility change.
- [x] 3.2 Run the existing test suite — all tests must pass with no behavioural change.
- [x] 3.3 Add a focused unit test asserting `S2LongFieldPath.of(id) == S2LongFieldPath.of(id)` for several ids including 0, common path encodings, and `Long.MAX_VALUE`.
- [x] 3.4 Add a focused unit test asserting reference identity for two builders snapshotted to the same path.
- [x] 3.5 Add a concurrency test: N threads each call `of(sameId)` on a fresh JVM-state — collect all returned references, assert all are `==`.

## 4. Bench against prior baseline

- [ ] 4.1 Re-run `propertychangebench` from `bench/` on the same replay set used for the `accelerate-s1-flat-state` baseline.
- [ ] 4.2 Compare results against the archived baseline; record delta in a comment on this change.
- [ ] 4.3 If regression: revert the change. If wash or improvement: proceed.

## 5. Verify downstream compatibility

- [ ] 5.1 Compile `clarity-analyzer` against the updated clarity (per the standing rule for API-affecting changes). Verify it still builds.
- [ ] 5.2 Compile `clarity-examples` against the updated clarity.
