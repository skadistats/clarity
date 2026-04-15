# clarity benches

Two JMH benches share this source set. They answer different questions; pick the right one.

## `EntityStateParseBench` → `./gradlew bench`

Measures **whole-replay parsing throughput** under each `S2EntityStateType` impl. One iteration =
one full parser run (Snappy decompression, protobuf decoding, bit-reader traversal, serializer-table
construction, field-path decoding, mutation application). Useful for end-to-end perf gates.

Resolution floor: ~2-10% (≈ ±40 ms on ~2000 ms iterations) — dominated by upstream decoding noise.
Cannot distinguish dispatch-level changes (1-3% on `applyMutation` are invisible here).

```bash
./gradlew bench                                    # default replay
./gradlew bench -PbenchArgs="--all"                # all canonical replays
./gradlew bench -PbenchArgs="path/to/file.dem"     # custom replay
```

Results land in `bench-results/<timestamp>_<branch-sha>/`.

## `MutationTraceBench` → `./gradlew traceBench`

Measures **`EntityState.applyMutation` throughput in isolation**. Captures a real mutation stream
from a parser run at `@Setup(Trial)`, then replays it against fresh-built states inside
`@Benchmark` — no protobuf, no bit-reader, no serializer-table work in the measured window.

Use this when validating dispatch-shape changes (e.g. `FieldLayout` virtualization,
`StateMutation` switch rewrites) — the parse bench cannot resolve them.

Resolution floor: ~1-2% on the ~300-600 ms hot loop. ~10× finer than the parse bench for this
workload.

The trace can reach 2-3 GB on a single Dota replay (~25M update mutations × ~80 B/entry), hence
the `-Xmx16g` in both the gradle task and the bench's `@Fork` annotation.

```bash
./gradlew traceBench                                    # default replay
./gradlew traceBench -PbenchArgs="--all"                # all canonical replays
./gradlew traceBench -PbenchArgs="path/to/file.dem"     # custom replay
```

Results land in `bench-results/trace_<timestamp>_<branch-sha>/`.

## When to run which

| Question | Bench |
|----------|-------|
| Did this change affect end-to-end parsing perf? | parse |
| Did this change affect `applyMutation` dispatch? | trace |
| Did this change affect anything outside `applyMutation`? | parse |
| Sub-3% delta on dispatch-only code? | trace (parse can't resolve it) |
| Allocation cost in the entity-state hot loop? | trace (allocation is part of dispatch cost) |

## Adding a new bench

Drop a JMH `@State` class next to the existing two, then either fold it into `Main`/`TraceMain`
or add a sibling. Each `Main` produces its own self-contained results directory under
`bench-results/`.
