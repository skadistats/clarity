# seal-engine-types — Benchmark before/after

JMH single-shot, 3 warmup × 10 measurement iterations, `-prof gc`. Three points measured to isolate the sealing effect from the bundled JDK 17→21 toolchain bump:

- **A. Pre / JDK 17**: `6ddc606` (origin/next, pre-seal base) — toolchain as-shipped.
- **B. Pre / JDK 21**: `6ddc606` with only `languageVersion.set(JavaLanguageVersion.of(21))` flipped — isolates JDK-only delta.
- **C. Post / JDK 21**: `00bf3c8` (next HEAD) — the shipped change.

Bench artifact dirs in `bench-results/`:
- A (S2): `2026-04-17_152830_HEAD-6ddc606/`, A (S1): `2026-04-17_153611_s1_HEAD-6ddc606/`
- B (S2): `2026-04-17_154018_HEAD-6ddc606/`, B (S1): `2026-04-17_154523_s1_HEAD-6ddc606/`
- C (S2): `2026-04-17_151826_next-00bf3c8/`, C (S1): `2026-04-17_152500_s1_next-00bf3c8/`

## S2 FLAT (`S2FlatEntityState`, median ms)

| Replay | A: pre/17 | B: pre/21 | C: post/21 | B-A (JDK only) | C-B (sealing only) | C-A (full) |
|---|---:|---:|---:|---:|---:|---:|
| cs2/3dmax-vs-falcons | 1261.0 | 1272.7 | 1226.4 | **+0.9%** | **-3.6%** | -2.7% |
| deadlock/19206063 | 1069.2 | 1072.2 | 1053.4 | **+0.3%** | **-1.8%** | -1.5% |
| dota/s2/8168882574 | 1423.1 | 1402.0 | 1338.3 | **-1.5%** | **-4.5%** | -6.0% |

## S2 NESTED_ARRAY (median ms)

| Replay | A: pre/17 | B: pre/21 | C: post/21 | B-A (JDK only) | C-B (sealing only) |
|---|---:|---:|---:|---:|---:|
| cs2 | 1302.2 | 1306.5 | 1264.9 | +0.3% | **-3.2%** |
| deadlock | 1128.1 | 1125.1 | 1110.5 | -0.3% | **-1.3%** |
| dota/s2 | 1516.7 | 1515.1 | 1458.5 | -0.1% | **-3.7%** |

## S1 FLAT (`S1FlatEntityState`, median ms)

| Replay | A: pre/17 | B: pre/21 | C: post/21 | B-A (JDK only) | C-B (sealing only) | C-A (full) |
|---|---:|---:|---:|---:|---:|---:|
| csgo/luminosity-azio | 674.5 | 679.1 | 658.8 | **+0.7%** | **-3.0%** | -2.3% |
| dota/s1/271145478 | 274.9 | 274.5 | 254.4 | **-0.1%** | **-7.3%** | -7.5% |

## Allocations

Unchanged across all replay × impl combinations in all three runs. Sealing is type-system only — no alloc effects, and the JDK bump does not show up in alloc rates either.

## Interpretation

The JDK 17→21 toolchain bump on this code is **not** the driver: B-A deltas sit in JMH noise (-1.5% to +0.9%, mean close to zero). The sealing work is doing the real work — **-1.3% to -4.5% on S2, -3.0% to -7.3% on S1**, consistent across replays and impls, with the largest wins on Dota (largest per-tick field-update counts, so the cast-removal in the `readFields` hot loop pays off most).

This is a bigger measurable payoff than the design framed — `design.md` called the perf delta "a non-blocking sanity check", not a goal. Turns out removing the per-field runtime cast in `S1FieldReader`/`S2FieldReader` is worth several percent on dense replays. Reasonable retrospectively: the loop runs thousands of times per tick and the old cast chain went through `FieldPath → S1/S2FieldPath` plus `EntityState → concrete` per iteration.

No regressions in any cell. Ship.
