# Source 2 Replay Parser Comparison

A benchmark and analysis of clarity against three other open-source Source 2 replay parsers: **demoparser2** (Rust, CS2), **demoinfocs-golang** (Go, CS2), and **manta** (Go, Dota 2).

Last run: **2026-09-24**, clarity `next` at `c9d00c17`. The harness lives in [clarity-bench](https://github.com/spheenik/clarity-bench) under `parsers/`; raw results are in its `results/parsers/2026-09-24_ryzen9950x/`.

## TL;DR

- **Steady state (long-lived process, the batch case)**, clarity has the fastest single-threaded decode on both games: ~1.5x faster than manta v1.5.0 on Dota 2 and ~1.2x faster than single-threaded demoparser2 on CS2, with the lowest CPU cost per demo of all parsers.
- **One-shot runs (one demo per process)** include clarity's JIT warmup. There manta v1.5.0 now edges out clarity on a small 2016 Dota demo (0.51s vs 0.58s) and burns ~3x less CPU; clarity still wins on a current 187 MB Dota demo.
- **manta got ~2.2x faster** since the April run (v1.5.0, dotabuff/manta#180). The previous "clarity is ~78% faster than manta" no longer holds one-shot; at steady state the gap is ~55%.
- **Single-demo wall-clock** is still demoparser2's: 0.45s one-shot on CS2 via fullpacket-parallel decode, at ~1.7x the CPU of its own single-threaded mode.
- **Memory** is clarity's weak spot for batch: ~0.8-1.4 GB RSS per JVM (`-Xmx4g`, replay memory-mapped) vs 40-85 MB for the Go parsers.

## Methodology

**Host**: AMD Ryzen 9 9950X (16c/32t), Linux 7.2.4, OpenJDK 21.0.12, Go 1.27.0, Python 3.14.7. Load average 1.0-1.5 during the run.

**Demos** (pinned by sha256 in the harness):

| Id | Replay | Size |
|---|---|---:|
| cs2-anubis | `cs2/350/3dmax-vs-falcons-m1-anubis.dem` (pro match) | 434 MB |
| dota-2016 | `dota/s2/normal/2267054840.dem` (2016 match) | 61 MB |
| dota-2025 | `dota/s2/340/8168882574_1198277651.dem` (current build) | 187 MB |

**Parsers**:

| Parser | Version | Workload |
|---|---|---|
| clarity | 5.0.0-SNAPSHOT, `next@c9d00c17` | `SimpleRunner` + `@UsesEntities`, no listeners (same as `dev:entityrun`) |
| demoparser2 | 0.42.0 (and 0.41.1) | `parse_ticks(["X","Y","Z","health"])`; ST = `RAYON_NUM_THREADS=1` |
| demoinfocs-golang | v5.2.0 (and v5.1.4) | `ParseToEnd()`, no handlers; ST = `MsgQueueBufferSize = 0` |
| manta | v1.5.0 = `0efe7e1` (and `91f7979`, the April build) | `NewStreamParser` + `Start()`, no callbacks |

**Two measurements**:

- **One-shot**: one parse per process, 7 rounds interleaved across all targets, median reported. The parse is timed inside the process (JVM startup excluded, JIT warmup included). Process CPU and peak RSS come from `/usr/bin/time` and include everything the process did.
- **Steady state**: 10 parses in one process, median of iterations 4-10. Wall and CPU are measured around each parse inside the process.

Run-to-run spread (max/min) was 1.7-8.3% for every cell.

## Results

### CS2 — anubis (434 MB)

| Parser | Mode | One-shot wall | One-shot process CPU | Steady wall | Steady CPU | Peak RSS |
|---|---|---:|---:|---:|---:|---:|
| demoparser2 0.42.0 | MT | **0.449s** | 4.61s | **0.249s** | 2.41s | 1.2-1.8 GB |
| clarity next | ST | 1.437s | 2.87s | 1.170s | **1.29s** | 1.1-1.2 GB |
| demoparser2 0.42.0 | ST | 1.670s | 3.65s | 1.442s | 1.45s | 1.1-1.3 GB |
| demoinfocs v5.2.0 | default | 2.220s | 6.00s | 2.220s | 6.12s | 57 MB |
| demoinfocs v5.2.0 | ST | 4.319s | 6.44s | 4.305s | 6.42s | 54 MB |

### Dota 2 — 2016 match (61 MB)

| Parser | Mode | One-shot wall | One-shot process CPU | Steady wall | Steady CPU | Peak RSS |
|---|---|---:|---:|---:|---:|---:|
| manta v1.5.0 | ST | **0.509s** | **0.65s** | 0.531s | 0.70s | 43 MB |
| clarity next | ST | 0.577s | 1.98s | **0.340s** | **0.44s** | 0.8 GB |

### Dota 2 — current build (187 MB)

| Parser | Mode | One-shot wall | One-shot process CPU | Steady wall | Steady CPU | Peak RSS |
|---|---|---:|---:|---:|---:|---:|
| clarity next | ST | **1.474s** | 3.60s | **1.214s** | **1.78s** | 1.0-1.4 GB |
| manta v1.5.0 | ST | 1.844s | **2.48s** | 1.860s | 2.52s | 80 MB |

manta and demoparser2 each support one game, so the comparison is per engine.

### Why one-shot and steady state differ for clarity

A fresh JVM spends its first parse partly interpreted while the JIT compiles the hot path on background threads. On the 61 MB demo that is most of the run: 1.98 CPU-seconds one-shot vs 0.44 at steady state. Clarity reaches steady state from the second parse on (cs2-anubis: 1.43s, then 1.19s, 1.18s, ...). The Go parsers have no warmup: their first parse costs the same as the tenth. demoparser2 warms up modestly (1.66s, then ~1.44s).

"ST" is not strictly single-core for clarity: GC runs on its own threads, so steady-state CPU is 1.1-1.5x wall. Go's GC does the same for manta and demoinfocs (1.3-1.5x).

### What changed since the April 2026 run

Old and new versions were benched side by side on the same machine, so these are the parsers' own changes:

| Parser | Old | New | Change |
|---|---|---|---|
| manta, dota-2016 | `91f7979`: 1.171s | v1.5.0: 0.509s | **2.3x faster** |
| manta, dota-2025 | `91f7979`: 3.903s | v1.5.0: 1.844s | **2.1x faster** |
| demoinfocs ST | v5.1.4: 5.332s | v5.2.0: 4.319s | 19% faster |
| demoinfocs default | v5.1.4: 2.547s | v5.2.0: 2.220s | 13% faster |
| demoparser2 MT | 0.41.1: 0.519s | 0.42.0: 0.449s | 13% faster |
| demoparser2 ST | 0.41.1: 1.814s | 0.42.0: 1.670s | 8% faster |

All numbers are one-shot medians on the same demos.

Clarity's April number (1.68s on cs2-anubis) is not comparable: the old clarity build wasn't re-benched, and the environment changed (kernel, JDK, Go). The same old manta build runs ~10% faster here than in April while the same old demoinfocs build runs at the same speed, so environment drift is real but uneven.

### Batch throughput

For batch work (many demos, one per worker), what matters is CPU seconds per demo at steady state. Idealized cost for 1000 demos on 32 hardware threads (1000 × steady CPU / 32, ignoring SMT and memory-bandwidth effects):

| Workload | Parser | Steady CPU / demo | 1000 demos / 32 threads |
|---|---|---:|---:|
| CS2 (anubis-sized) | clarity | 1.29s | ~40s |
| | demoparser2 ST | 1.45s | ~45s |
| | demoparser2 MT | 2.41s | ~75s |
| | demoinfocs default | 6.12s | ~191s |
| Dota 2 (current, 187 MB) | clarity | 1.78s | ~56s |
| | manta v1.5.0 | 2.52s | ~79s |

This assumes a long-lived clarity worker that parses many demos per JVM. Launching one JVM per demo pays the one-shot CPU instead (2.87s on CS2, 3.60s on Dota), which puts clarity behind manta on Dota (2.48s); on CS2 it stays ahead of demoparser2 ST's one-shot 3.65s.

Memory also caps batch parallelism: 32 clarity JVMs at ~1.2 GB each need ~40 GB, vs ~3 GB for 32 manta processes. The clarity heap was not tuned here (`-Xmx4g`, default G1); a smaller heap would likely cut RSS at some GC cost, but that wasn't measured.

For demoparser2, fullpacket-parallel mode costs ~1.7x the CPU of ST, so for batch `RAYON_NUM_THREADS=1` across demos is the better configuration.

## Parser-by-Parser Analysis

### demoparser2 (Rust, CS2)

Repo: https://github.com/LaihoE/demoparser

Architecture:

```
                  demo bytes
                      |
                      v
              +----------------+   collects: sendtables, class info,
              |  FIRST PASS    |   stringtables, game event list,
              |  (sequential)  |   FULLPACKET OFFSETS, baselines
              +-------+--------+
                      |
                      v
              +----------------+
              |  SECOND PASS   |  rayon::par_iter over
              |                |  fullpacket_offsets
              |  thread 1: fp0..fp1
              |  thread 2: fp1..fp2
              |  thread N: fpN..end
              +-------+--------+
                      |
                      v
              combine_outputs (clones per chunk)
```

**Why it's fast**:
- **Fullpacket-parallel second pass**. Fullpackets are keyframes containing a complete baseline snapshot. Each inter-fullpacket chunk can be decoded independently with a cloned baseline. On cs2-anubis it averages ~10 busy cores (2.41 CPU-s over 0.249s steady wall).
- **Wanted-props filter** at storage time: only selected props hit the output DataFrame. The bitstream is positional so every field is still *decoded*, but per-prop hashmap inserts, Variant boxing, and column accumulation are skipped.
- **Bit reader** via `bitter` crate: 64-bit register with explicit `refill` / `peek(n)` / `consume(n)`, `#[inline(always)]` throughout.
- **Huffman field-path decode** via 17-bit peek table, not a tree walk.
- **Reusable scratch buffers** avoid per-message allocation.
- 0.41.4 ported further parse speedups (LaihoE/demoparser#334); 0.42.0 is 8-13% faster than 0.41.1 here.

**Weaknesses**:
- `Entity.props: AHashMap<u32, Variant>` — per-entity hashmap vs flat array. Hot-path insert cost when many props are wanted.
- Parallel mode costs ~1.7x the CPU of ST (clone/combine overhead, serial first pass).
- MT is silently disabled when user requests stateful props (velocity, derived time-series) — falls back to ST.
- Baselines are cloned per chunk; with many chunks in flight, many copies of the baseline state are resident at once.

**Scope**: CS2 only. No Source 1, no Dota.

### demoinfocs-golang (Go, CS2)

Repo: https://github.com/markus-wa/demoinfocs-golang

Architecture:

```
              bitstream reader goroutine
                      |
                      v
              +------------------+
              |  msgQueue        |  buffered channel
              |  (chan any)      |  default: max(50k, PlaybackTicks)
              +---------+--------+
                        |
                        v
              dispatcher goroutine
                        |
                        v
              +------------------+
              |  handleX / bindY |  per-class wiring:
              |                  |    CC4, CCSTeam, CCSPlayerController,
              |                  |    CCSPlayerPawn, CBaseCSGrenade*,
              |                  |    CInferno, CCSGameRulesProxy,
              |                  |    CHostage, ...
              +---------+--------+
                        |
                        v
              gameState (live CS2 state)
                        |
                        v
              eventDispatcher -> user handlers (if any)
```

**What makes its wall-clock fast**:
- **Built-in two-goroutine pipeline**. Net-message reading and entity/event dispatch run concurrently. Disabling it (`MsgQueueBufferSize = 0`) nearly doubles wall-clock: 2.22s -> 4.32s.

**What it does in its inner loop that clarity's bare decode doesn't**:
- **Full CS2 game-state maintenance**: bomb state, controller <-> pawn linking, weapon-slot binding, grenade projectile lifecycle, inferno tracking, team state, game rules, hostages.
- **Synthetic Source 1 events** reconstructed from entity-prop watches (disableable; ~3% impact).

None of the state tracking can be disabled. With no user handlers, the internal prop-update callbacks still fire on every matching change.

**The gap to clarity** (4.3s ST vs clarity's 1.2s steady) is mostly attributed to that game-state work, but the split is an inference, not a measurement: demoinfocs's decode path is not directly comparable (bit-at-a-time Huffman walk, see below), so part of the gap is decode cost too.

v5.2.0 (markus-wa/demoinfocs-golang#644) replaced a map lookup in the field-path name cache with slice indexing and skips field-path name resolution when no update handlers are registered; measured here as 13-19% faster, matching the PR's own 12-15% claim.

### manta (Go, Dota 2)

Repo: https://github.com/dotabuff/manta

Architecture: straightforward sequential loop. No pipeline goroutine, no parallelism, no prop filtering. Bare entity decode, the same scope as clarity's `entityrun`.

v1.5.0 (dotabuff/manta#180, June 2026) overhauled the hot path:
- **Word-at-a-time bit reader**: refills a 64-bit accumulator from 8-byte loads instead of byte by byte.
- **Flattened Huffman tree + 8-bit op lookup table**: most field-path ops resolve in a single table index, the same approach clarity uses.
- **Reusable field-path buffer** of value types instead of a freshly grown `[]*fieldPath` per entity per tick (the PR reports −78% allocations).
- **Typed entity state**: a 24-byte tagged-union cell instead of `interface{}` boxing on the write path.
- **Class baselines decoded once** and cloned per entity.

Measured here as 2.1-2.3x faster than the April build. With no warmup and 40-85 MB RSS it is now the cheapest parser for one-shot Dota runs; at steady state clarity is still ~1.5x faster.

**Scope**: Dota 2 only. No CS2 support.

## Architectural Observations

### Parallelism strategies, compared

| Parser | Strategy | Works for | Breaks for |
|---|---|---|---|
| clarity | None (single-threaded) | Batch throughput, deterministic ordering | Single-demo latency |
| demoparser2 | Fullpacket-parallel decode | Single-demo latency, big CS2 demos | Stateful processors (velocity); costs ~1.7x CPU, so it's a loss for batch |
| demoinfocs | Two-goroutine pipeline (I/O || dispatch) | Single-demo latency, modest win | N/A — always on, can only disable |
| manta | None (single-threaded) | Simplicity | Single-demo latency |

**Assessment for clarity**:

- Demoparser2's fullpacket-parallel approach yields the biggest single-demo latency win (~5.8x vs its own ST at steady state). Porting it would require a `canParallelize()` contract for every processor, snapshottable baselines and stringtables at fullpacket boundaries, post-hoc event ordering reconciliation, and reworking `ControllableRunner` seek semantics. High engineering cost for a latency-only benefit.
- Demoinfocs's goroutine pipeline is a much cheaper pattern: a single producer/consumer boundary between frame decode and dispatch. Preserves deterministic dispatch ordering and would fit clarity's processor model in principle. Still only a latency feature.
- For batch consumers, parallelism *across* demos is the right use of cores and already works today.

### Decode-path microarchitecture

| | Bit reader | Field-path Huffman decode |
|---|---|---|
| clarity | 64-bit register, batched refill | 8-bit peek into a 256-entry table (~1 KB, L1-resident), tree-walk fallback for the tail |
| demoparser2 | 64-bit register (`bitter`), explicit refill/peek/consume | 17-bit peek into a 131072-entry table (~256 KB) |
| manta v1.5.0 | 64-bit accumulator, 8-byte word refill | 8-bit lookup table over a flattened tree |
| demoinfocs v5.2.0 | 64-bit accumulator, byte-at-a-time refill | flattened node array, one `readBoolean()` per tree level |

Clarity, demoparser2 and now manta have converged on table-driven Huffman decode over a wide bit register; demoinfocs is the remaining bit-at-a-time walker. With decode techniques this close, steady-state differences come mostly from what each parser does per field after decoding (state layout, boxing, allocation) and from runtime (JIT vs AOT, GC).

### What "fast" means, revisited

The headline "parser X is 2x faster than parser Y" claims usually elide four things:

1. **Thread count**. demoparser2's typical benchmark numbers compare MT demoparser2 to ST competitors.
2. **Workload scope**. demoinfocs maintains CS2 state that bare decoders don't.
3. **Output shape**. demoparser2 builds a polars DataFrame; clarity has no output; demoinfocs dispatches events.
4. **Process lifetime**. JIT-compiled clarity pays warmup on every fresh JVM; a one-shot CLI benchmark and a batch worker give different rankings.

Apples-to-apples requires pinning all four.

## Takeaways for Clarity

1. **The decode path is still competitive, but the lead has narrowed.** At steady state clarity is the fastest per core on both games; manta closed most of the gap on Dota by adopting the same table-driven Huffman decode and word-at-a-time reads.
2. **JVM warmup is now a visible cost.** For one-shot use on small demos, a fresh JVM loses to manta. Batch consumers should keep one JVM per worker and parse many demos in it; worth stating in the docs. AppCDS or a CRaC/Leyden-style warm start would be the tools if one-shot latency ever matters.
3. **Memory per worker is clarity's biggest batch disadvantage.** ~1 GB RSS per JVM vs <100 MB for the Go parsers limits how many workers fit on a box. A measured minimal-heap configuration would be worth documenting.
4. **Intra-demo parallelism remains a latency feature with meaningful engineering cost.** demoparser2's MT mode is ~5.8x faster wall-clock but ~1.7x more CPU; not worth porting for throughput.
5. **Feature parity with demoinfocs for CS2 is a separable question.** Weapon tracking, bomb state, grenade lifecycle etc. would add CPU cost if implemented; that's a scope decision, not a performance decision.

## Reproducing

The harness is in [clarity-bench](https://github.com/spheenik/clarity-bench) under `parsers/`:

```bash
cd ../clarity-protobuf && ./gradlew publishToMavenLocal && cd -
cd ../clarity && ./gradlew publishToMavenLocal && cd -
python3 parsers/run.py --replays-root /path/to/replays \
  --clarity-label "next@$(git -C ../clarity rev-parse --short HEAD)" --record
```

It builds every pinned parser version itself (Go modules, Python virtualenvs, the clarity side from `mavenLocal`), verifies the replays by sha256, and writes `results.json` and `summary.md` under `results/parsers/<date>_<host>/`. See `parsers/README.md` there for details.
