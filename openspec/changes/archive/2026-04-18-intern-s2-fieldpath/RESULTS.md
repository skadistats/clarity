# Results — `intern-s2-fieldpath` (REJECTED)

**Status:** Rejected. Source changes reverted on 2026-04-18.
**Reason:** Empirical wall-clock regression that no implementation variant could close.

## What was tried

Three implementation shapes, all measured against the pre-change `next` baseline (commit `7cff438`) on `replays/dota/s2/340/8168882574_1198277651.dem` via `EntityStateParseBench` (JMH single-shot, 3 warmup × 10 measurement iterations):

1. **fastutil COW intern** — `volatile Long2ObjectOpenHashMap`, double-checked-locked insert, hand-rolled `of(long)` factory with no allocation on hits.
2. **Hand-rolled fixed-size open-addressed table** — single `synchronized` method (under-engineered hash, see "Why the hand-rolled version looked broken" below).
3. **Per-builder fastutil intern** (instrumentation only, never benched).

## Headline numbers (fastutil version)

| Impl (S2 parse) | pre-change median | post-intern median | Δ wall-clock | Δ alloc/op |
|---|---|---|---|---|
| NESTED_ARRAY    | 1472.6 ms | 1529.0 ms | **+3.8%** | -16% |
| TREE_MAP        | 1824.3 ms | 1889.0 ms | **+3.5%** | -15% |
| FLAT (S2)       | 1343.0 ms | 1426.4 ms | **+6.2%** | -18% |
| FLAT (S1)       | 1455.3 ms | 1515.9 ms | **+4.2%** | n/a |
| OBJECT_ARRAY    | 1454.5 ms | 1528.6 ms | **+5.1%** | n/a |

Memory dedup is real (~16% allocation drop, exactly the wrapper bytes). Wall-clock regresses ~4% across all impls — consistent direction, outside the JMH error bars (±20–50 ms; deltas 50–80 ms).

## Why this design cannot win

`S2LongFieldPath` is a 24-byte wrapper around a single `long`. TLAB allocation of such an object is ~3–5 cycles (bump pointer + write header + write field, no shared state, no synchronization). Any intern lookup must touch shared state at minimum:

```
Best-possible lookup (single-thread, no sync, perfect hash):
  load table reference          ~1 cycle
  hash + mask                   ~3 cycles
  array load (slot)             ~3 cycles
  null check + key compare      ~2 cycles
                                ──────────
                                  ~9 cycles
```

Even a perfect hash cannot get under TLAB's floor for this object size. The structural ceiling is real.

## Path-universe size (measured, not estimated)

After the experiment, the intern was repurposed to measure distinct path counts via a JVM shutdown-hook log. Single-parse counts:

| replay | distinct paths |
|---|---|
| Dota S2 (`8168882574_1198277651.dem`) | 7,409 |
| Deadlock (`19206063.dem`)             | 4,058 |
| CS2 (`3dmax-vs-falcons-m1-anubis.dem`) | 6,547 |

Universe is solidly in the 4K–7.5K range across all three engines — confirming a single-digit-thousands working set per replay. The 65,536-slot slab tried in the hand-rolled version was 9× over-provisioned; slab size was never the issue.

## Hash-quality finding (incidental)

The naive `(int)(id ^ (id >>> 32)) & mask` hash used in the first hand-rolled version is **catastrophic** for this keyset:

```
collision rate (Dota, 7409 keys):
                       slab=8K   slab=16K  slab=32K  slab=64K
  (id ^ id>>>32)        96.1%     94.2%     91.5%     91.5%   ← unusable
  HashCommon.mix        33.6%     18.6%      9.7%      5.0%   ← fine
```

`S2LongFieldPathFormat` packs the depth-0 value into bits 52–62 with the low 39 bits typically zero for shallow paths. XOR-shift collapses these into slot 0 en masse. The "synchronized hand-rolled looked like an infinite loop" symptom was likely huge probe chains under the lock, not a true infinite loop. **Lesson:** never use ad-hoc XOR-shift for ids derived from this packed format; always use a proper bit-mixer (`HashCommon.mix`, MurmurHash3 finalizer, etc.).

## Why the hand-rolled version looked broken

Combining the bad hash above with `synchronized` on every call produced pathological wall-clock that JMH iterations could not complete in normal time. Replacing the hash with `HashCommon.mix` would have brought it back to fastutil's range — but that range is itself a regression, so chasing the variant further was rabbit-holing.

## What the future looks like (if anyone revisits this)

Both viable directions skip the wrapper rather than dedup it:

1. **Eliminate the wrapper from the decoder hot path.** Pass `long` directly through `S2EntityState.write()`/`decodeInto()`/`applyMutation()` as a parallel API; only materialize an `S2LongFieldPath` at the listener boundary where consumers actually want an object. Allocation site count drops from "millions per parse" to "however many paths escape to listeners." Larger surgery; touches every state impl.

2. **Cache paths on the schema's `Field` tree.** Each leaf `Field` lazily owns the `S2LongFieldPath` that points to it. The decoder already walks the tree by index during field resolution; once the walk lands on a leaf, snapshot becomes a single field load on an object the decoder is already holding. Eliminates the `mfp.snapshot()` call entirely. Even larger surgery; couples the path-builder to the schema tree.

Neither is on the table for this proposal; both are real follow-ups if someone wants to attack S2 decode wall-clock further.

## Files reverted

- `src/main/java/skadistats/clarity/model/s2/S2LongFieldPath.java` — restored to value-based `equals`, public constructor, no intern.
- `src/main/java/skadistats/clarity/model/s2/S2LongFieldPathBuilder.java` — `snapshot()` back to `new S2LongFieldPath(id)`.
- `src/test/java/skadistats/clarity/model/s2/S2LongFieldPathInternTest.java` — deleted.

## Bench outputs preserved

- Pre-change baseline: `bench-results/2026-04-18_035430_next-7cff438/`
- Post-fastutil-intern: `bench-results/2026-04-18_045016_next-410457f/`
