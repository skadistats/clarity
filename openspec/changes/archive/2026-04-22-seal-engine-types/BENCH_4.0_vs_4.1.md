# Clarity 4.0.0 → 4.1-to-be (next HEAD `00bf3c8`) — full state-type comparison

JMH single-shot, 3 warmup × 10 measurement iterations, `-prof gc`, `-Xmx4g`, JDK 21.0.10. Logback silenced (root=warn) on both sides. 4.0.0 ran from a worktree at tag `53f03b2` with the bench harness back-ported.

**4.0.0 caveats**:
- No `S2EntityStateType` / `S1EntityStateType` enum, no `withS{1,2}EntityState()` API. To get TREE_MAP numbers I edited `EntityStateFactory.forS2` to return `new TreeMapEntityState()` and recompiled.
- `S2FlatEntityState` and `S1FlatEntityState` did not exist — no FLAT row possible at 4.0.0.
- S1 only had one impl (`ObjectArrayEntityState`).

## S2 — wall-clock (ms, median)

| Replay | impl | 4.0.0 | 4.1 | Δ |
|---|---|---:|---:|---:|
| cs2/3dmax-vs-falcons | NESTED_ARRAY | 1769.6 | 1264.9 | **-28.5%** |
| cs2/3dmax-vs-falcons | TREE_MAP | 1894.3 | 1501.2 | **-20.8%** |
| cs2/3dmax-vs-falcons | FLAT | n/a | **1226.4** | (new in 4.1) |
| deadlock/19206063 | NESTED_ARRAY | 1387.1 | 1110.5 | **-19.9%** |
| deadlock/19206063 | TREE_MAP | 1525.6 | 1362.6 | **-10.7%** |
| deadlock/19206063 | FLAT | n/a | **1053.4** | (new in 4.1) |
| dota/s2/8168882574 | NESTED_ARRAY | 1895.9 | 1458.5 | **-23.1%** |
| dota/s2/8168882574 | TREE_MAP | 2146.9 | 1813.3 | **-15.5%** |
| dota/s2/8168882574 | FLAT | n/a | **1338.3** | (new in 4.1) |

## S2 — allocations (GB/op)

| Replay | impl | 4.0.0 | 4.1 | Δ |
|---|---|---:|---:|---:|
| cs2 | NESTED_ARRAY | 13.27 | 3.65 | **-72%** |
| cs2 | TREE_MAP | 13.29 | 3.67 | **-72%** |
| cs2 | FLAT | n/a | 3.32 | — |
| deadlock | NESTED_ARRAY | 5.60 | 2.81 | **-50%** |
| deadlock | TREE_MAP | 5.65 | 2.85 | **-50%** |
| deadlock | FLAT | n/a | 2.45 | — |
| dota/s2 | NESTED_ARRAY | 9.47 | 3.58 | **-62%** |
| dota/s2 | TREE_MAP | 9.61 | 3.71 | **-61%** |
| dota/s2 | FLAT | n/a | 3.31 | — |

## S1 — wall-clock (ms, median)

| Replay | impl | 4.0.0 | 4.1 | Δ |
|---|---|---:|---:|---:|
| csgo/luminosity-azio | OBJECT_ARRAY | 1288.4 | 659.0 | **-48.9%** |
| csgo/luminosity-azio | FLAT | n/a | **658.8** | (new in 4.1) |
| dota/s1/271145478 | OBJECT_ARRAY | 421.7 | 268.8 | **-36.3%** |
| dota/s1/271145478 | FLAT | n/a | **254.4** | (new in 4.1) |

## S1 — allocations (GB/op)

| Replay | impl | 4.0.0 | 4.1 | Δ |
|---|---|---:|---:|---:|
| csgo | OBJECT_ARRAY | 15.64 | 2.33 | **-85%** |
| csgo | FLAT | n/a | 1.73 | — |
| dota/s1 | OBJECT_ARRAY | 4.40 | 1.11 | **-75%** |
| dota/s1 | FLAT | n/a | 0.97 | — |

## What 4.0.0 → 4.1 changed (recap)

Major perf-impacting changes between `53f03b2` and `00bf3c8`:

1. `static-decoder-dispatch` + `fieldop-dispatch-rework` (April 12) — generated tableswitch decoder dispatch; switch-based field-op dispatch.
2. `nested-array-state-recursive-cleanup` (April 14) — restructured `NestedArrayEntityState`.
3. `flat-entity-state` (April 14) — introduced `S2FlatEntityState` (opt-in).
4. `accelerate-flat-entity-state` (April 16) — `decodeInto` zero-alloc path; FLAT became S2 default.
5. `strip-entity-state-cow` (April 16) — eager copy, dropped CoW wrapper.
6. `prefix-entity-state-names` + `accelerate-s1-flat-state` (April 16-17) — S1 FLAT.
7. `seal-engine-types` (current) — sealed types, removed runtime casts in `S{1,2}FieldReader` hot loops; bumped JDK 21.

## Default-experience headline

A 4.0.0 user runs NESTED_ARRAY on S2 / OBJECT_ARRAY on S1 (the only impls). A 4.1 user gets FLAT by default on both engines. End-to-end deltas:

| Engine | Replay | 4.0.0 default ms | 4.1 default ms | Δ wall | 4.0.0 alloc | 4.1 alloc | Δ alloc |
|---|---|---:|---:|---:|---:|---:|---:|
| S2 | cs2 | 1769.6 | 1226.4 | **-30.7%** | 13.27 GB | 3.32 GB | **-75%** |
| S2 | deadlock | 1387.1 | 1053.4 | **-24.1%** | 5.60 GB | 2.45 GB | **-56%** |
| S2 | dota | 1895.9 | 1338.3 | **-29.4%** | 9.47 GB | 3.31 GB | **-65%** |
| S1 | csgo | 1288.4 | 658.8 | **-48.9%** | 15.64 GB | 1.73 GB | **-89%** |
| S1 | dota S1 | 421.7 | 254.4 | **-39.7%** | 4.40 GB | 0.97 GB | **-78%** |

## Notes on the TREE_MAP rerun

After swapping `EntityStateFactory.forS2` to TreeMapEntityState and recompiling, the JMH bytecode-generated wrappers still encoded the old `@Param` label `NESTED_ARRAY` (only `compileJmhJava` re-ran, not `jmhRunBytecodeGenerator`). The label in `bench-results/2026-04-17_163634_HEAD-53f03b2/results.txt` is therefore misleading — the actual runtime impl was TreeMapEntityState. Numbers in the table above are the real TREE_MAP measurements.

## Raw artifacts

- 4.0.0 NESTED_ARRAY S2: `/tmp/clarity-4.0.0/bench-results/2026-04-17_162715_HEAD-53f03b2/`
- 4.0.0 TREE_MAP S2 (mislabelled): `/tmp/clarity-4.0.0/bench-results/2026-04-17_163634_HEAD-53f03b2/`
- 4.0.0 OBJECT_ARRAY S1: `/tmp/clarity-4.0.0/bench-results/2026-04-17_163118_s1_HEAD-53f03b2/`
- 4.1 S2 (all 3 impls): `bench-results/2026-04-17_151826_next-00bf3c8/`
- 4.1 S1 (both impls): `bench-results/2026-04-17_152500_s1_next-00bf3c8/`
