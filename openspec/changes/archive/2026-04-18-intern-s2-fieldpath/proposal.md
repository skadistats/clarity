> **STATUS: REJECTED (2026-04-18).** Empirically slower than baseline despite saving 16% allocation. See [RESULTS.md](RESULTS.md) for the full negative result and the structural reason no variant can win on this hot path.

## Why

`S2FieldReader` allocates a fresh `S2LongFieldPath` wrapper for every changed field on every packet — the wrapper is just a 24-byte boxed `long`, but the call site sits in the hottest decode loop and produces millions of short-lived objects per replay. The wrapper is immutable and its full state is the `long id`, so distinct instances with the same id are pure duplicates with no semantic difference.

Interning the wrapper as a flyweight collapses these duplicates to a single instance per `id`, JVM-wide. Because the workload is read-mostly after a brief warmup (one replay of a given schema is enough to populate the cache), a primitive-keyed map with copy-on-write inserts gives a contention-free steady state, recovers the wrapper memory, and lets `equals` collapse to identity comparison across all consumers.

## What Changes

- `S2LongFieldPath` gains a private static intern table (`Long2ObjectOpenHashMap`, COW-published via a `volatile` field reference) and a static `of(long id)` factory.
- The constructor becomes private — every `S2LongFieldPath` instance MUST originate from `of(long)`, guaranteeing the no-duplicates invariant by construction.
- `S2LongFieldPathBuilder.snapshot()` calls `S2LongFieldPath.of(id)` instead of `new S2LongFieldPath(id)`.
- `equals` collapses to identity (`this == o`) — correct because of the intern invariant.
- `hashCode` and `compareTo` are unchanged (long-derived hash gives better HashMap distribution than identity hash; ordering is intrinsic to the long encoding).

## Capabilities

### New Capabilities

_None — this strengthens the existing fieldpath capability rather than adding a new one._

### Modified Capabilities

- `configurable-fieldpath-impl`: add a requirement that the `LONG` impl interns its instances JVM-wide so that two `S2LongFieldPath` references with the same `id` are guaranteed to be the same object, and that `equals` is identity-based as a consequence.

## Impact

- **Code**: `S2LongFieldPath` (intern table, factory, private constructor, simplified equals); `S2LongFieldPathBuilder.snapshot()` (one-line change).
- **API**: No public surface change. Constructor visibility tightens but is package-internal already in spirit (only the builder uses it).
- **Dependencies**: Uses fastutil's `Long2ObjectOpenHashMap` (already a project dependency). No new deps.
- **Behaviour**: Observable output bit-exact identical. Memory: per-process flyweights, bounded by distinct path universe (low thousands per Dota schema). Throughput: expected neutral-to-positive on the decode hot path; measured against the `accelerate-s1-flat-state` baseline already on disk.
- **Concurrency**: Multi-threaded parsers share one global intern; reads are wait-free (volatile load + fastutil `get`), writes are synchronized but uncontended in practice.
