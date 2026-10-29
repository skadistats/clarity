# Source 2 Replay Parser Comparison

A benchmark and analysis of clarity against three other open-source Source 2 replay parsers: **demoparser2** (Rust, CS2), **demoinfocs-golang** (Go, CS2), and **manta** (Go, Dota 2).

Last run: **2026-10-29** for Dota 2 (clarity 5.0.0 and clarity `master@3be39a5d`, manta v1.5.2), plus a batch run with several parsers in one process; the other CS2 parsers are from the **2026-09-24** run (clarity `next` at `c9d00c17`) and were not re-run. The harness lives in [clarity-bench](https://github.com/spheenik/clarity-bench) under `parsers/`; raw results are in its `results/parsers/2026-10-29_ryzen9950x/` (5.0.0), `results/parsers/2026-10-29_ryzen9950x_master/`, `results/parsers/2026-10-29_ryzen9950x_batch/` and `results/parsers/2026-09-24_ryzen9950x/`.

## TL;DR

- **manta is faster than clarity on Dota 2, also at steady state.** manta v1.5.2 (dotabuff/manta#181 and #183) is ~1.25-1.3x faster wall-clock than clarity 5.0.0 and needs ~1.4-1.5x less CPU per demo. clarity `master` (two hot-path fixes after this comparison, −7-12% steady wall) narrows that to ~1.13-1.21x wall and ~1.25-1.4x CPU. One-shot manta is still 1.5-2x faster.
- **Batch in one process** (8 parser threads, one JVM vs 8 goroutines): manta parses ~1.23x more demos per second at ~1.22x less CPU per demo, with ~250 MB RSS vs ~2.8 GB for clarity at `-Xmx2g`. ParallelGC is clarity's best collector there; SerialGC loses a third of the throughput.
- **manta got ~2x faster again** since v1.5.0, and ~4.4x since the April build. Each of the two rounds measured here as roughly its own PR's claim (−24-26%, then −32-35%).
- **On CS2 (2026-09-24 run)**, clarity has the fastest single-threaded decode, ~1.2x faster than single-threaded demoparser2 at steady state, with the lowest CPU cost per demo; clarity `master` is another 7% faster.
- **Single-demo wall-clock** on CS2 is demoparser2's: 0.45s one-shot via fullpacket-parallel decode, at ~1.7x the CPU of its own single-threaded mode.
- **Memory** is clarity's weak spot: ~0.8-1.1 GB RSS for a single-parse JVM (`-Xmx4g`, replay memory-mapped) vs 35-57 MB for the Go parsers, and ~2.8 GB vs ~250 MB with 8 parsers in one process.

## Methodology

**Host**: AMD Ryzen 9 9950X (16c/32t), OpenJDK 21.0.12.

| Run | Kernel | Go | Python | Load average |
|---|---|---|---|---|
| 2026-10-29 (Dota 2) | 7.2.7 | 1.27.2 | 3.14.8 | 1.2-2.7 |
| 2026-09-24 (CS2) | 7.2.4 | 1.27.0 | 3.14.7 | 1.0-1.5 |

**Demos** (pinned by sha256 in the harness):

| Id | Replay | Size |
|---|---|---:|
| cs2-anubis | `cs2/350/3dmax-vs-falcons-m1-anubis.dem` (pro match) | 434 MB |
| dota-2016 | `dota/s2/normal/2267054840.dem` (2016 match) | 61 MB |
| dota-2025 | `dota/s2/340/8168882574_1198277651.dem` (current build) | 187 MB |

**Parsers**:

| Parser | Version | Workload |
|---|---|---|
| clarity | 5.0.0 and `master@3be39a5d` (Dota, CS2 re-check); `next@c9d00c17` (CS2) | `SimpleRunner` + `@UsesEntities`, no listeners (same as `dev:entityrun`) |
| demoparser2 | 0.42.0 (and 0.41.1) | `parse_ticks(["X","Y","Z","health"])`; ST = `RAYON_NUM_THREADS=1` |
| demoinfocs-golang | v5.2.0 (and v5.1.4) | `ParseToEnd()`, no handlers; ST = `MsgQueueBufferSize = 0` |
| manta | v1.5.2 = `d4e4647` (and v1.5.1, v1.5.0, and `91f7979`, the April build) | `NewStreamParser` + `Start()`, no callbacks |

**Two measurements**:

- **One-shot**: one parse per process, 7 rounds interleaved across all targets, median reported. The parse is timed inside the process (JVM startup excluded, JIT warmup included). Process CPU and peak RSS come from `/usr/bin/time` and include everything the process did.
- **Steady state**: 10 parses in one process, median of iterations 4-10. Wall and CPU are measured around each parse inside the process.

Run-to-run spread (max/min) in the Dota run was 1.5-8.4% for every cell (one-shot clarity is the noisiest, from JIT timing); the 2026-09-24 run had 1.7-8.3%.

## Results

### CS2 — anubis (434 MB), 2026-09-24 run

| Parser | Mode | One-shot wall | One-shot process CPU | Steady wall | Steady CPU | Peak RSS |
|---|---|---:|---:|---:|---:|---:|
| demoparser2 0.42.0 | MT | **0.449s** | 4.61s | **0.249s** | 2.41s | 1.2-1.8 GB |
| clarity next | ST | 1.437s | 2.87s | 1.170s | **1.29s** | 1.1-1.2 GB |
| demoparser2 0.42.0 | ST | 1.670s | 3.65s | 1.442s | 1.45s | 1.1-1.3 GB |
| demoinfocs v5.2.0 | default | 2.220s | 6.00s | 2.220s | 6.12s | 57 MB |
| demoinfocs v5.2.0 | ST | 4.319s | 6.44s | 4.305s | 6.42s | 54 MB |

The 2026-10-29 runs re-measured clarity on this demo: 5.0.0 at 1.375s one-shot / 1.114s steady (1.18s CPU), i.e. within ~5% of the row above on wall time (steady CPU −9%; the one-shot rounds were noisy, 19% spread), and `master@3be39a5d` at 1.290s one-shot (2.71s CPU) / 1.035s steady (1.12s CPU). The other CS2 parsers were not re-run.

### Dota 2 — 2016 match (61 MB)

| Parser | Mode | One-shot wall | One-shot process CPU | Steady wall | Steady CPU | Peak RSS |
|---|---|---:|---:|---:|---:|---:|
| manta v1.5.2 | ST | **0.256s** | **0.29s** | **0.260s** | **0.29s** | 35 MB |
| clarity master | ST | 0.509s | 1.79s | 0.286s | 0.35s | 0.8 GB |
| clarity 5.0.0 | ST | 0.553s | 1.88s | 0.326s | 0.40s | 0.8 GB |

### Dota 2 — current build (187 MB)

| Parser | Mode | One-shot wall | One-shot process CPU | Steady wall | Steady CPU | Peak RSS |
|---|---|---:|---:|---:|---:|---:|
| manta v1.5.2 | ST | **0.874s** | **1.00s** | **0.889s** | **1.05s** | 56 MB |
| clarity master | ST | 1.316s | 3.25s | 1.070s | 1.48s | 1.0 GB |
| clarity 5.0.0 | ST | 1.401s | 3.40s | 1.163s | 1.60s | 1.0 GB |

The clarity master rows come from a second run the same day (clarity and manta only, load 1.3-2.3), in which manta measured 0.259s / 0.253s (2016) and 0.897s / 0.885s (2025) one-shot / steady, within 3% of the rows above. clarity `master@3be39a5d` adds two hot-path changes over 5.0.1: decoders stored in the flat state's layout leaves, so the per-field `resolveField` walk of the serializer tree is gone (−7-8% in an interleaved A/B), and a per-kind cache of the embedded-message listener lookups (−1.5%).

manta and demoparser2 each support one game, so the comparison is per engine.

### Why one-shot and steady state differ for clarity

A fresh JVM spends its first parse partly interpreted while the JIT compiles the hot path on background threads. On the 61 MB demo that is most of the run: 1.88 CPU-seconds one-shot vs 0.40 at steady state. Clarity reaches steady state from the second parse on (cs2-anubis: 1.43s, then 1.19s, 1.18s, ...). The Go parsers have no warmup: their first parse costs the same as the tenth. demoparser2 warms up modestly (1.66s, then ~1.44s).

"ST" is not strictly single-core for clarity: GC runs on its own threads, so steady-state CPU is 1.1-1.4x wall. Go's GC does the same for manta and demoinfocs; manta v1.5.2 now allocates so little that its steady CPU is only 1.1-1.2x wall.

### manta across versions

All four builds were benched side by side in the 2026-10-29 run (one-shot medians):

| manta build | dota-2016 | dota-2025 | vs previous |
|---|---:|---:|---|
| `91f7979` (April 2026) | 1.139s | 3.852s | |
| v1.5.0 (#180, June) | 0.497s | 1.823s | 2.1-2.3x faster |
| v1.5.1 (#181, October) | 0.379s | 1.349s | −24% / −26% |
| v1.5.2 (#183, October) | **0.256s** | **0.874s** | −32% / −35% |

The two October rounds roughly match manta's own claims (#181: −24% sec/op; #183: −33%). The same v1.5.0 build ran within 1-3% of its 2026-09-24 one-shot numbers (steady state 1-8% faster), and clarity's Dota wall times moved by 4-5% (clarity 5.0.0 vs `next@c9d00c17`, different kernel and Go), so the environment is comparable across the two runs.

### What changed between the April and September 2026 runs

From the 2026-09-24 run, old and new versions benched side by side:

| Parser | Old | New | Change |
|---|---|---|---|
| demoinfocs ST | v5.1.4: 5.332s | v5.2.0: 4.319s | 19% faster |
| demoinfocs default | v5.1.4: 2.547s | v5.2.0: 2.220s | 13% faster |
| demoparser2 MT | 0.41.1: 0.519s | 0.42.0: 0.449s | 13% faster |
| demoparser2 ST | 0.41.1: 1.814s | 0.42.0: 1.670s | 8% faster |

All numbers are one-shot medians on cs2-anubis.

### Batch throughput

#### Several parsers in one process (measured)

The usual way to run clarity in batch is one JVM with a thread pool, each thread parsing its own demo. The batch harness (clarity-bench `parsers/batch/`) runs exactly that: N threads (goroutines for manta) pull demos from a shared queue, 12 demos per thread, after one warmup pass per thread. Demos: the four `dota/s2/normal` replays of the clarity A/B set (2015-2016 matches) plus dota-2025. clarity is `master@3be39a5d`; medians of 3 interleaved rounds, spread ≤ 3.4%, load ≤ 4. The G1 `-Xmx8g` and SerialGC rows come from a first matrix with the same setup (where ParallelGC `-Xmx8g` measured 2.59 / 8.40 / 14.13 demos/s, within 2% of its row here).

| Parser | Config | N=1 demos/s | N=4 demos/s | N=8 demos/s | N=8 CPU / demo | N=8 peak RSS |
|---|---|---:|---:|---:|---:|---:|
| manta v1.5.2 | default | **3.02** | **9.95** | **17.81** | **0.46s** | **0.25 GB** |
| clarity | ParallelGC `-Xmx2g` | 2.77 | 8.59 | 14.51 | 0.57s | 2.8 GB |
| clarity | ParallelGC `-Xmx4g` | 2.63 | 8.39 | 13.94 | 0.57s | 4.8 GB |
| clarity | ParallelGC `-Xmx8g` | 2.60 | 8.25 | 14.30 | 0.57s | 6.7 GB |
| clarity | G1 `-Xmx4g` | 2.55 | 8.05 | 13.45 | 0.63s | 4.0 GB |
| clarity | G1 `-Xmx8g` | 2.58 | 8.10 | 13.54 | 0.63s | 6.3 GB |
| clarity | SerialGC `-Xmx8g` | 2.50 | 6.60 | 9.09 | 0.52s | 2.0 GB |

- At 8 threads manta parses ~1.23x more demos per second at ~1.22x less CPU per demo. On this mostly older demo set the single-thread gap is smaller (1.09x).
- **ParallelGC is clarity's best collector for batch**: ~4% more throughput and ~10% less CPU per demo than the default G1. **SerialGC is a loss with several parser threads**: it stops all of them for every collection (4.1 s of pauses in a ~10 s run at N=8), so throughput drops by a third even though CPU per demo looks lowest.
- The heap grows to whatever `-Xmx` allows. `-Xmx2g` is enough for 8 parsers and costs no throughput (it was the fastest clarity row), at 2.8 GB RSS; that is still ~11x manta's 250 MB, partly because every running parse keeps its replay memory-mapped.
- Both parsers lose per-demo efficiency with more threads (manta 0.37 → 0.46 CPU-s/demo, clarity 0.47 → 0.57 from N=1 to N=8), most likely from the lower all-core clock; this was not checked.
- A single-parse JVM looks different: there G1's parallel GC workers add ~40% process CPU on top of the parse thread for many tiny young collections, which ParallelGC (−15%) or SerialGC (−27%) avoid. That only matters for one-parse-at-a-time use.

#### Idealized from single-parse runs

The original estimate below scales one parse's steady CPU to 1000 demos on 32 hardware threads (1000 × steady CPU / 32, ignoring SMT, clock and memory-bandwidth effects). The measured numbers above are more realistic:

| Workload | Parser | Steady CPU / demo | 1000 demos / 32 threads |
|---|---|---:|---:|
| CS2 (anubis-sized, 2026-09-24) | clarity | 1.29s | ~40s |
| | demoparser2 ST | 1.45s | ~45s |
| | demoparser2 MT | 2.41s | ~75s |
| | demoinfocs default | 6.12s | ~191s |
| Dota 2 (current, 187 MB) | manta v1.5.2 | 1.05s | ~33s |
| | clarity 5.0.0 | 1.60s | ~50s |
| | clarity master | 1.48s | ~46s |

This assumes a long-lived clarity worker that parses many demos per JVM. Launching one JVM per demo pays the one-shot CPU instead (2.87s on CS2, 3.40s on Dota), which puts clarity ~3.4x behind manta on Dota (1.00s); on CS2 it stays ahead of demoparser2 ST's one-shot 3.65s.

Running one JVM per worker would also multiply memory (~1 GB each); one JVM with a thread pool, as measured above, is the better setup.

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

v1.5.1 (dotabuff/manta#181, October 2026) went after allocations:
- **Hand-rolled envelope decode** for `CDemoPacket`, `CSVCMsg_PacketEntities`, `CSVCMsg_UpdateStringTable` and `CNETMsg_Tick`: payloads are aliased instead of copied through the reflective protobuf unmarshal, used only when no user callback is registered for that message type (the PR reports −42% bytes allocated).
- **Per-packet arena** for embedded message bodies, copied a word at a time from the bit-shifted stream.
- **Entities in a dense slice** instead of a map, string-table slabs, slab-allocated baseline clones, and an event-id peek that skips unmarshalling unhandled game events.

v1.5.2 (dotabuff/manta#183, October 2026), mostly CPU (it also cut allocated bytes by another 42%):
- **Shift-merge byte copies** and **varint/ubitvar decode from a single accumulator peek** in the bit reader, with refills moved out of line so the fast paths inline.
- **Field-path ops dispatched through a switch** of direct calls, keeping the path accumulator on the stack.
- **16-byte state cell** (pointer + length + kind) instead of an interface, copy-on-write baseline clones, and alloc-free typed getters.
- `CDemoFullPacket` string-table dumps are skipped when unused.

v1.5.0 to v1.5.2 is ~2x faster, ~4.4x faster than the April build. With no warmup, 35-56 MB RSS and the lowest CPU per demo, it is now the fastest Dota 2 parser measured here both one-shot and at steady state.

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
| manta v1.5.2 | 64-bit accumulator, 8-byte word refill, single-peek varints | 8-bit lookup table over a flattened tree, switch-dispatched ops |
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

1. **Clarity has lost the Dota 2 lead.** manta v1.5.2 was ~1.25-1.3x faster than clarity 5.0.0 at steady state and used ~1.4-1.5x less CPU per demo; on CS2 clarity was still the fastest per core in the 2026-09-24 run. A warmed-up JFR profile of clarity on Dota (2026-10-29, entities only, no listeners) put ~80% of CPU in the per-field work in `S2FieldReader`; protobuf parsing and byte copies together were only ~3%, so manta's envelope and zero-copy work has no clarity equivalent worth porting. Removing the per-field `resolveField` walk (decoders now live in the flat state's layout leaves) and caching the embedded-message listener lookups brought clarity `master` to ~1.13-1.21x behind. What remains in the profile is field-op decode, the layout walk plus value decode into the state, field-path snapshots, the `FieldChanges` copy and the queued update dispatch.
2. **JVM warmup is a visible cost.** For one-shot use, a fresh JVM loses to manta by 1.5-2x on Dota. Batch consumers should keep one long-lived JVM and parse many demos in it; worth stating in the docs. AppCDS or a CRaC/Leyden-style warm start would be the tools if one-shot latency ever matters.
3. **Batch setup: one JVM, a thread pool, ParallelGC, a modest heap.** With 8 parser threads, `-XX:+UseParallelGC -Xmx2g` was the fastest clarity configuration measured, ~4% ahead of default G1; SerialGC costs a third of the throughput. Memory is still clarity's biggest batch disadvantage: ~2.8 GB RSS for 8 parsers vs ~250 MB for manta.
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

It builds every pinned parser version itself (Go modules, Python virtualenvs, the clarity side from `mavenLocal`), verifies the replays by sha256, and writes `results.json` and `summary.md` under `results/parsers/<date>_<host>/`. The one-process batch numbers come from `parsers/batch/run_batch.sh`. See `parsers/README.md` there for details.
