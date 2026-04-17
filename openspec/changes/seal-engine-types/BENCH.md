# seal-engine-types — Benchmark before/after

JMH single-shot, 3 warmup × 10 measurement iterations, `-prof gc`.

- **Pre**: `6ddc606` (origin/next, pre-seal base) — JDK 17 toolchain
- **Post**: `00bf3c8` (next HEAD) — JDK 21 toolchain (JDK bump is part of this change)

Bench artifact dirs in `bench-results/`:
- `2026-04-17_152830_HEAD-6ddc606/` — S2 pre
- `2026-04-17_153611_s1_HEAD-6ddc606/` — S1 pre
- `2026-04-17_151826_next-00bf3c8/` — S2 post
- `2026-04-17_152500_s1_next-00bf3c8/` — S1 post

## S2 — `EntityStateParseBench` (median wall-clock)

| Replay | impl | pre | post | Δ |
|---|---|---:|---:|---:|
| cs2/350/3dmax-vs-falcons-m1-anubis | NESTED_ARRAY | 1302.2 ms | 1264.9 ms | **-2.9%** |
| cs2/350/3dmax-vs-falcons-m1-anubis | TREE_MAP | 1502.8 ms | 1501.2 ms | -0.1% |
| cs2/350/3dmax-vs-falcons-m1-anubis | **FLAT** | 1261.0 ms | 1226.4 ms | **-2.7%** |
| deadlock/newer/19206063 | NESTED_ARRAY | 1128.1 ms | 1110.5 ms | -1.6% |
| deadlock/newer/19206063 | TREE_MAP | 1399.9 ms | 1362.6 ms | -2.7% |
| deadlock/newer/19206063 | **FLAT** | 1069.2 ms | 1053.4 ms | **-1.5%** |
| dota/s2/340/8168882574_1198277651 | NESTED_ARRAY | 1516.7 ms | 1458.5 ms | -3.8% |
| dota/s2/340/8168882574_1198277651 | TREE_MAP | 1866.4 ms | 1813.3 ms | -2.8% |
| dota/s2/340/8168882574_1198277651 | **FLAT** | 1423.1 ms | 1338.3 ms | **-6.0%** |

## S1 — `S1EntityStateParseBench` (median wall-clock)

| Replay | impl | pre | post | Δ |
|---|---|---:|---:|---:|
| csgo/s1/luminosity-vs-azio-cache | **FLAT** | 674.5 ms | 658.8 ms | **-2.3%** |
| csgo/s1/luminosity-vs-azio-cache | OBJECT_ARRAY | 689.3 ms | 659.0 ms | -4.4% |
| dota/s1/normal/271145478 | **FLAT** | 274.9 ms | 254.4 ms | **-7.5%** |
| dota/s1/normal/271145478 | OBJECT_ARRAY | 288.3 ms | 268.8 ms | -6.8% |

## Allocations (alloc/op)

Unchanged across all replay × impl combinations pre vs post. Sealing is type-system only — no allocation effects expected and none observed.

## Interpretation

- Sealing + cast-at-entry is a validation, not a goal (per `design.md` non-goal #30). Design explicitly did not gate on perf delta.
- Observed 2-8% wall-clock wins are net positive across every replay × impl cell; no regressions.
- The JDK 17 → 21 toolchain bump is bundled into this change and almost certainly contributes to the delta (JIT improvements, pattern-matching `switch` lowering, etc.). The sealing effect alone cannot be isolated without a JDK-held-constant re-bench, which is not warranted given the non-goal.
- Architecturally the win is the removed runtime casts in the hot path (`S1FieldReader`/`S2FieldReader`); the measured delta is consistent with "no hot-path regression, small wins from better dispatch + JDK bump".
