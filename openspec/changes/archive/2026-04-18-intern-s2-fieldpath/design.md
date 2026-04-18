## Context

`S2LongFieldPath` is the production `S2FieldPath` impl selected by `S2FieldPathType.LONG`. It wraps a single primitive `long` (the packed depth + per-level indices encoded by `S2LongFieldPathFormat`) and is otherwise immutable. Construction happens at one site in the hot decode loop:

```
S2FieldReader.readFieldsFast():
  while (more ops in packet) {
      FieldOp.execute(opId, mfp, bs);          // mutates the builder's long
      fieldPaths[n++] = mfp.snapshot();        // allocates a new wrapper
  }
```

Per replay this is millions of allocations. Each wrapper is 24 bytes after object header padding; total wrapper bytes are dominated by gen-zero collection cost rather than raw allocation throughput, but the churn is real and visible in alloc-profiler samples.

The `accelerate-s1-flat-state` change (archived 2026-04-16) left a baseline of `propertychangebench` numbers on disk that this change can directly diff against.

## Goals / Non-Goals

**Goals:**

- Eliminate `S2LongFieldPath` allocations on the steady-state decode path by sharing one instance per distinct `long id` JVM-wide.
- Strengthen `S2LongFieldPath`'s identity contract so that `equals` collapses to `==` for all consumers.
- Keep the change confined to `S2LongFieldPath` and its single matching builder. No other class — state impls, readers, runners, listeners — needs to know.

**Non-Goals:**

- No API change to `S2FieldPath`, `S2FieldPathType`, runners, or any consumer of paths.
- No introduction of a separate intern class — the cache lives on `S2LongFieldPath` itself, since the no-duplicates invariant is part of that type's contract.
- No prepopulation of the cache from schema. Warmup is cheap; static enumeration would add complexity without measurable benefit.
- No changes to other `S2FieldPath` implementors (none exist today; the contract change is scoped to `LONG`).
- No replacement of the wrapper with raw `long` on hot paths. That is a much larger refactor with API ripple; this change is the minimum-surgery alternative.

## Decisions

### Decision: Intern table lives on S2LongFieldPath, not in a separate class

The "every long has at most one wrapper" invariant is intrinsic to `S2LongFieldPath`'s identity. Splitting the intern into a sibling class would force the constructor to remain accessible (so the sibling can populate the cache), defeating the type-level enforcement of the invariant. Co-locating keeps the constructor private and the invariant statically guaranteed.

**Alternatives considered:**

- *Separate `S2FieldPathInterner` class.* Cleaner separation of concerns in the abstract; in practice it forces a public constructor or a package-private back-channel and makes the invariant a convention rather than a guarantee.
- *Intern at the `S2FieldPathBuilder` boundary instead of `snapshot()`.* Same semantics but requires the builder to know about the cache, which leaks the `LONG`-specific design into the builder interface contract.

### Decision: Copy-on-write over a fastutil Long2ObjectOpenHashMap

The workload is read-mostly after a short warmup (one replay's worth of decode populates the cache for a given schema). Reads must be wait-free; writes can be slow because they happen at most a few thousand times over the JVM's life.

Implementation:

```
private static volatile Long2ObjectOpenHashMap<S2LongFieldPath> INTERN
        = new Long2ObjectOpenHashMap<>(8192);

public static S2LongFieldPath of(long id) {
    var t = INTERN;                       // volatile load
    var hit = t.get(id);
    if (hit != null) return hit;
    return ofSlow(id);
}

private static synchronized S2LongFieldPath ofSlow(long id) {
    var t = INTERN;
    var hit = t.get(id);                  // re-check inside the lock
    if (hit != null) return hit;
    var fresh = new S2LongFieldPath(id);
    var copy = new Long2ObjectOpenHashMap<>(t);
    copy.put(id, fresh);
    INTERN = copy;                        // volatile publish
    return fresh;
}
```

Read path: one volatile load + one fastutil `get` (single hash + linear probe, no chain walk). No CAS, no per-bucket volatile reads.

Write path: synchronized on the class monitor. Uncontended in single-threaded parsers (~couple of ns for monitor enter/exit); only contended during overlapping warmups in multi-threaded scenarios, which is itself rare and bounded.

The "no duplicate flyweight" invariant holds unconditionally because:
1. `ConcurrentHashMap`-style atomicity is not needed — the synchronized block + double-check guarantees single-creation under concurrency.
2. Resizing the underlying fastutil map (during the COW copy) only moves references; it never reconstructs values.

**Alternatives considered:**

- *`ConcurrentHashMap<Long, S2LongFieldPath>`.* Re-introduces a `Long.valueOf` boxing allocation per lookup for ids outside the `[-128, 127]` range, silently undoing the win. Also has slightly more expensive read path than COW + fastutil.
- *Striped or segmented concurrent map.* Solves a contention problem we don't have.
- *Per-thread `ThreadLocal` L1 over a global L2.* Adds bookkeeping; the volatile-load + fastutil-get baseline is already cheap enough that an L1 isn't justified.

### Decision: equals collapses to identity, hashCode unchanged

Once the intern invariant holds, `equals` reduces to `this == o`. The override could be removed entirely (letting `Object.equals` handle it), but keeping an explicit `equals` with a comment documents the non-local invariant for readers.

`hashCode` stays as `S2LongFieldPathFormat.hashCode(id)` because:
- It gives well-distributed values for `HashMap` consumers, whereas `System.identityHashCode` is essentially random and harms bucket distribution.
- The cost of computing it is trivial (a few bit ops on the long).

`compareTo` is unchanged — ordering is intrinsic to the long encoding, not to instance identity.

### Decision: Initial table size 8192

Roughly the steady-state distinct-path count for a Dota schema (empirically a few thousand, with some headroom). Sized to avoid resizes during normal warmup; a too-small initial table would still be correct (resizes preserve the flyweight invariant) but would cause a few unnecessary copies in the first replay. Memory cost: ~64KB of references for an empty table — negligible.

## Risks / Trade-offs

- **[Risk]** A reflection-based or deserialization path that bypasses `of(long)` and constructs an `S2LongFieldPath` directly would mint a duplicate and silently break `equals`. → **Mitigation:** private constructor; class is not `Serializable`; no Jackson/serialization use today. A doc comment on `equals` explains why identity is sufficient.
- **[Risk]** Memory leak across long-running multi-replay servers if many distinct schema versions are parsed and each contributes new path ids. → **Mitigation:** the practical upper bound is small (low tens of thousands across many patches). At ~24 bytes per wrapper plus map entry overhead, even 100K distinct paths is well under 10MB. Not worth weak references.
- **[Risk]** Bench regression: the lookup is theoretically slower than a TLAB allocation. → **Mitigation:** the prior `accelerate-s1-flat-state` baseline numbers exist; re-run `propertychangebench` (and the same set used for the 4.0 vs 4.1 comparison) and diff. If the result is a wash or regression, revert — the change is ~30 lines.
- **[Trade-off]** Constructor goes private. Any future code that wants to construct an `S2LongFieldPath` directly must route through `of(long)`. This is intentional — the invariant only holds if the factory is the sole construction path.

## Migration Plan

No migration needed for consumers — the change is observably equivalent except for `equals` becoming faster and instances being deduplicated. Ship behind no flag; the bench result is the gate. If regression, revert (the change is small and fully isolated).
