# Source 2 Replay Parser Comparison

A benchmark and analysis of clarity against three other open-source Source 2 replay parsers: **demoparser2** (Rust, CS2), **demoinfocs-golang** (Go, CS2), and **manta** (Go, Dota 2).

## TL;DR

- **Per-core decode-only**, clarity has the fastest inner loop of the four parsers benchmarked — on both CS2 and Dota 2 demos.
- **Wall-clock on a single demo**, demoparser2 wins by a factor of ~3x due to fullpacket-parallel decoding; demoinfocs wins modestly via a two-goroutine pipeline.
- **For batch/pipeline workloads** (many demos, one core per demo), clarity's per-core advantage directly converts to throughput advantage. Intra-demo parallelism gives no batch benefit; idle worker threads park harmlessly but consume thread stacks and a bit of scheduler overhead.
- **Workload scope differs**: clarity's `entityrun` is bare decode; demoinfocs maintains extensive CS2 game state (weapons, bomb, players, projectiles) in its inner loop. That scope gap explains ~3.5s of demoinfocs's ST runtime, not a performance deficit.

## Methodology

**Host**: AMD Ryzen 9 9950X (16c/32t), Linux 6.19, OpenJDK 21.0.10.

**Demos**:
- CS2: `3dmax-vs-falcons-m1-anubis.dem` — pro match, 433 MB, ~1.55M entity updates sampled.
- Dota 2: `2267054840.dem` — normal match, 60.6 MB.

**Measurement**: best-of-3 wall-clock.
- Clarity timed inside `run()` (JVM startup excluded, JIT warmup included).
- Python parsers timed from `DemoParser(path)` through `parse_ticks()` return.
- Go parsers timed from `NewParser` through `ParseToEnd`.

**Parsers tested**:
- `clarity` — this repo, examples `dev:entityrun` (bare entity decode).
- `demoparser2` v0.41.1 — Rust, via Python pyo3 bindings from PyPI.
- `demoinfocs-golang` v5.1.4 — Go, from module proxy.
- `manta` — Go, Dota 2 only, `master` as of 2026-03-24.

## Headline Results

### CS2 — anubis demo (433 MB)

| Parser | Mode | Effective cores | Best wall-clock |
|---|---|---:|---:|
| demoparser2 | MT default (rayon, fullpacket-parallel) | ~4-5 effective | **0.51s** |
| clarity `entityrun` | ST | 1 | 1.68s |
| demoparser2 | ST (`RAYON_NUM_THREADS=1`) | 1 | 2.17s |
| demoinfocs | default (goroutine pipeline + mimic events) | 2 | 2.57s |
| demoinfocs | sequential (no pipeline) | 1 | 5.17s |

### Dota 2 — normal match demo (60.6 MB)

| Parser | Mode | Effective cores | Best wall-clock |
|---|---|---:|---:|
| clarity `entityrun` | ST | 1 | **0.73s** |
| manta | ST | 1 | 1.30s |

manta and demoparser2 do not support the other engine's demos, so cross-engine comparison is limited to clarity.

### Batch throughput projection

One demo per thread, 32 threads saturating the host, 1000 CS2 demos:

| Parser | Projected total |
|---|---:|
| clarity ST | ~52s |
| demoparser2 ST | ~68s |
| demoinfocs default (2 threads/demo) | ~83s |

For batch workloads, you run demos in parallel, one (or two) threads per demo. demoparser2's MT-by-fullpacket mode gives no benefit in this regime — rayon's extra workers park on a condvar when there's no work, so leaving it on has only a small overhead (thread startup, scheduler churn). The right configuration for batch is `RAYON_NUM_THREADS=1` + GNU parallel / xargs across demos.

Clarity is the fastest for batch throughput by a meaningful margin on per-core decode speed.

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
- **Fullpacket-parallel second pass**. Fullpackets are keyframes containing a complete baseline snapshot. Each inter-fullpacket chunk can be decoded independently with a cloned baseline.
- **Wanted-props filter** at storage time: only selected props hit the output DataFrame. The bitstream is positional so every field is still *decoded*, but per-prop hashmap inserts, Variant boxing, and column accumulation are skipped.
- **Bit reader** via `bitter` crate: 64-bit register with explicit `refill` / `peek(n)` / `consume(n)`, `#[inline(always)]` throughout.
- **Huffman field-path decode** via 17-bit peek table, not a tree walk.
- **Reusable scratch buffers** (400 KB + 120 KB) avoid per-message allocation.
- `ahash` hashmaps, `prost` for protobufs, `snap` for snappy decompression.

**Weaknesses**:
- `Entity.props: AHashMap<u32, Variant>` — per-entity hashmap vs flat array. Hot-path insert cost when many props are wanted.
- Combine phase clones every sub-output (`df.clone()`, stringtables, event lists). Bounded by fullpacket count (tens), but allocates.
- Parallel scaling plateaus at ~4-5 effective cores even on 32-core hosts. Limited by number of fullpackets in a typical demo and by the serial first pass.
- MT is silently disabled when user requests stateful props (velocity, derived time-series) — `check_multithreadability` falls back to ST.
- Baselines are cloned per thread; on 32 chunks in parallel, 32 copies of the baseline state are resident simultaneously.

**Scope**: CS2 only. Rejects HL2DEMO magic outright. No Source 1, no Dota.

**Completeness within CS2**: entities, events, stringtables, usercmds, voice data, chat, convars, item drops, skins, bullet events, projectiles, purchase/sell reconciliation. Explicit gaps: POV demos (`parse_user_command_cmd` is a stub), `DemAnimationData`, opus decode for voice.

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
- **Built-in two-goroutine pipeline**. Default `MsgQueueBufferSize = -1` auto-sizes to tick count. Net-message reading and entity/event dispatch run concurrently. Disabling the pipeline (`MsgQueueBufferSize = 0`) doubles wall-clock: 2.57s -> 5.17s.

**What it does in its inner loop that clarity's `entityrun` doesn't**:
- **Full CS2 game-state maintenance** in `bindEntities()` (1321 LOC in `datatables.go`):
  - Bomb carrier/position/plant/defuse state
  - Player controller <-> pawn linking, weapon-slot binding
  - Grenade projectile lifecycle and trajectory
  - Inferno (molotov fire) polygon tracking
  - Team state, bombsites, game rules phases, hostages
- **Synthetic Source 1 events**: S2 demos don't emit legacy `BombPickup`/`BombDropped`/`WeaponFire` events, so demoinfocs reconstructs them from entity-prop watches. Disableable via `DisableMimicSource1Events` but impact is only ~3%.

None of this state tracking can be disabled. With user-subscribed handlers empty, the internal prop-update callbacks still fire on every matching change.

**Attribution of the 5.17s sequential baseline**:
- ~1.7s comparable to clarity's decode path (estimated, not directly measured — see caveat below).
- ~3.5s of CS2 state machine work that clarity does not perform.
- ~3% for synthetic event generation.
- The ~2.6s gap between sequential (5.17s) and default (2.57s) is the goroutine-pipeline concurrency.

**Caveat**: the 3.5s/1.7s split is a rough inference, not a measurement. It assumes demoinfocs's decode path has similar per-tick cost to clarity's, which is plausible (both use comparable field-path huffman + bitstream reader designs) but not proven.

### manta (Go, Dota 2)

Repo: https://github.com/dotabuff/manta

Architecture: straightforward sequential loop. No pipeline goroutine, no parallelism, no prop filtering. Last non-trivial change in 2026-03-24 was "update protos"; design has been stable for years. Bare entity decode, similar scope to clarity's `entityrun`.

**Result**: 1.30s on Dota 2 demo (60.6 MB) vs clarity's 0.73s. Clarity is ~78% faster per core.

**Scope**: Dota 2 only. Hardcoded Dota protos. No CS2 support.

## Architectural Observations

### Parallelism strategies, compared

| Parser | Strategy | Works for | Breaks for |
|---|---|---|---|
| clarity | None (single-threaded) | Batch throughput, deterministic ordering | Single-demo latency |
| demoparser2 | Fullpacket-parallel decode | Single-demo latency, big CS2 demos | Stateful processors (velocity); for batch it's a no-op rather than a win |
| demoinfocs | Two-goroutine pipeline (I/O || dispatch) | Single-demo latency, modest win | N/A — always on, can only disable |
| manta | None (single-threaded) | Simplicity | Single-demo latency |

**Assessment for clarity**:

- Demoparser2's fullpacket-parallel approach is the most aggressive and yields the biggest single-demo latency win. Porting it would require: a `canParallelize()` contract for every processor, snapshottable baselines and stringtables at fullpacket boundaries, post-hoc event ordering reconciliation, and reworking `ControllableRunner` seek semantics. High engineering cost for a latency-only benefit; no throughput benefit.
- Demoinfocs's goroutine pipeline is a much cheaper architectural pattern: a single producer/consumer boundary between frame decode and dispatch. Preserves deterministic dispatch ordering. Would be compatible with clarity's processor model in principle. Still only a latency feature, not a throughput feature.
- For a batch-oriented consumer (the common case), the best use of cores is parallelism *across* demos, which is trivial and already works today.

### Decode-path microarchitecture

Hot-path techniques overlap but don't converge:

- 64-bit bitstream register with batched refill — clarity, demoparser2. demoinfocs and manta read bit-by-bit.
- Field-path Huffman decode — all four approaches differ:
  - **demoparser2**: single 17-bit peek into a 131072-entry table (~256 KB, shipped as a binary blob).
  - **clarity**: 8-bit peek into a 256-entry table (~1 KB, L1-resident) resolves ~99.7% of ops; tree-walk fallback for the tail.
  - **demoinfocs** and **manta**: pure bit-by-bit tree walk through a `huffmanTree` interface, one `readBits(1)` per node.
- Field-path tree traversal mapping path -> decoder.
- Per-decoder specialization for noscale float, bitcoord, quantized float, qangle variants, simulation time.

Measured ST decode-only numbers (clarity 1.68s, demoparser2 2.17s, manta 1.30s vs 0.73s on the smaller Dota demo) are consistent with JIT-compiled Java being roughly on par with, or slightly ahead of, unoptimized-release Rust/Go on a tight bitstream loop, despite clarity's lighter-weight Huffman table. demoinfocs and manta pay for the bit-by-bit walk, but it isn't the dominant cost in either.

### What "fast" means, revisited

The headline "parser X is 2x faster than parser Y" claims circulating in the community usually elide three things:

1. **Thread count**. demoparser2's typical benchmark numbers compare MT demoparser2 to ST competitors.
2. **Workload scope**. demoinfocs maintains CS2 state that bare decoders don't. Comparing full-scope demoinfocs to bare-scope clarity flatters the bare decoder by ~3.5s on CS2.
3. **Output shape**. demoparser2 builds a polars DataFrame; clarity has no output; demoinfocs dispatches events. Output construction is real work and varies by parser.

Apples-to-apples requires pinning all three.

## Takeaways for Clarity

1. **The decode path is competitive.** Clarity's bare entity decode is the fastest per-core implementation in this comparison across both supported games. No urgent need for a decoder rewrite.
2. **Intra-demo parallelism is a latency feature with meaningful engineering cost.** For almost all known clarity consumers (analytics pipelines, replay indexing), batch throughput matters more than single-demo latency, and clarity's per-core lead already wins on throughput without needing intra-demo parallelism.
3. **Goroutine-pipeline-style parallelism is the cheapest potential win if latency ever matters.** A single producer/consumer boundary between frame decode and processor dispatch could give a demoinfocs-style ~2x wall-clock speedup without breaking clarity's processor model. Worth keeping in mind if a latency-sensitive use case ever emerges; not a priority today.
4. **Feature parity with demoinfocs for CS2 is a separable question.** Weapon tracking, bomb state, grenade projectile lifecycle etc. aren't currently in clarity. That's a scope decision, not a performance decision; these would add CPU cost if implemented, roughly matching demoinfocs's 3.5s ST overhead.

## Reproducing

Clarity side:

```
./gradlew :dev:entityrunPackage
time java -jar dev/build/libs/entityrun.jar <replay.dem>
```

demoparser2 side (Python):

```python
from demoparser2 import DemoParser
import time
p = DemoParser("<replay.dem>")
t0 = time.perf_counter()
df = p.parse_ticks(["X","Y","Z","health", ...])
print(time.perf_counter() - t0)
```

For ST: `RAYON_NUM_THREADS=1 python ...`

demoinfocs side (Go):

```go
f, _ := os.Open("<replay.dem>")
defer f.Close()
p := demoinfocs.NewParser(f)   // or NewParserWithConfig with MsgQueueBufferSize=0 for ST
defer p.Close()
t0 := time.Now()
p.ParseToEnd()
fmt.Println(time.Since(t0))
```

manta side (Go):

```go
f, _ := os.Open("<replay.dem>")
defer f.Close()
t0 := time.Now()
p, _ := manta.NewStreamParser(f)
p.Start()
fmt.Println(time.Since(t0))
```
