## Goal

Produce a bench that can resolve a dispatch-level perf change (≈1-3% on `applyMutation`) cleanly, where the existing whole-replay parse bench has a ±2-10% noise floor from upstream decoding work.

## Non-goal

Replicating the parse bench's workload mix — including object allocation from protobuf decoding, snappy decompression I/O, and JIT compilation of decode paths. Those are real costs but they drown dispatch-level signal.

## Architecture

```
┌────────────────────── @Setup(Level.Trial) ──────────────────────┐
│                                                                 │
│    MappedFileSource ──▶ SimpleRunner ──▶ MutationRecorder       │
│                                              │                  │
│                                              ▼                  │
│                                     List<Mutation> trace        │
│                                     (one per applyMutation      │
│                                      call the parser would      │
│                                      have made)                 │
│                                                                 │
│    Serializer / FieldLayout tables retained by reference        │
│    (interned in the capture — mutations may carry them)         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

┌────────────────────── @Benchmark (measured) ────────────────────┐
│                                                                 │
│    state = S2EntityStateType.valueOf(impl).create(...)          │
│    // plus any per-entity-class state setup the capture         │
│    // needs to reproduce — see "Entity identity" below          │
│                                                                 │
│    for (Mutation m : trace) {                                   │
│        state.applyMutation(m.fp, m.mutation);                   │
│    }                                                            │
│                                                                 │
│    // Nothing above runs the parser. Measured throughput is     │
│    // purely descent + dispatch + leaf write.                   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## State identity model

Entities are uniquely identified by `Entity.uid = (dtClassId << 32) | handle` (`EntityRegistry`). Index reuse at the *Entity* level is not a concern — recreates with a bumped serial produce a new handle and therefore a new `Entity` object.

However, a single `Entity` can host multiple sequential `EntityState` instances over its lifetime via `executeEntityRecreate` (each create/recreate calls `setState(newState)` with a freshly-built state). And baselines themselves are `EntityState` instances built by the parser. So the trace needs a key that identifies *each individual EntityState ever constructed*, not each Entity.

**Chosen: monotonic `stateId`.** A counter incremented every time an `EntityState` is constructed by the parser (baseline build, entity create, entity recreate, `updateBaseline` snapshot). The recorder assigns the id at construction time and tags every captured mutation with the id of the state it targets.

Replay-side storage is a dense `EntityState[]` indexed by `stateId` — no map lookup in the hot loop.

## Baselines

Confirmed by reading `BaselineRegistry` and `Entities.getBaseline()`: every `EntityState` in the system originates from `cls.getEmptyState()` and accumulates only `applyMutation` calls. Baselines are constructed by the parser itself (`changes.applyTo(s)` against an empty state from network-encoded baseline data) and entity baselines are snapshots (`newState.copy()`) of states that were themselves built that way. Once cached, a baseline reference is never mutated in place — `markClassBaselineDirty` nulls and rebuilds; `setClassBaselineState` replaces the slot.

This means the bench can faithfully reproduce production state shapes by replaying captured mutations against fresh empty states of the requested impl. No foreign-data fixture is needed.

## Captured trace shape

The recorder distinguishes between two kinds of `applyMutation` calls based on which call site fires:

- **Setup mutations** — those that build baselines (`Entities.getBaseline()` line ~677) and those that occur at entity create/recreate time (`executeEntityCreate` line ~492, `executeEntityRecreate` line ~527). These fire ~thousands of times per replay.
- **Update mutations** — those from `executeEntityUpdate` line ~551. These fire ~millions of times per replay and are the bench's measurement target.

Trace shape:

```java
record BirthRecipe(
    int          stateId,
    BirthKind    kind,           // EMPTY (baseline build) or COPY_OF (baseline.copy() / newState.copy())
    SerializerField field,        // for EMPTY — needed for impl-specific createState
    int          srcStateId,      // for COPY_OF — id of the state to clone from
    Mutation[]   setupMutations   // mutations applied to this state during its setup phase
) {}

record Mutation(
    int          stateId,
    FieldPath    fp,              // captured by reference
    StateMutation mutation        // captured by reference
) {}

// Two outputs from a capture run:
List<BirthRecipe> births;          // ordered by birth time
Mutation[]        updateMutations; // ordered by capture time — the measured workload
```

`FieldPath` and `StateMutation` are captured by reference (FieldPaths are pooled; StateMutation is a record — both are immutable from the recorder's perspective).

## Recorder hook

`EntityState` cannot be transparently wrapped: `S2FieldReader.readFieldsFast` and `readFieldsDebug` perform a hard cast `(AbstractS2EntityState) entityState` (`S2FieldReader.java:75, :106`). A pure-interface decorator would `ClassCastException` on the first packet.

Subclassing each `*EntityState` impl (option iii) requires three copies of hook code — rejected.

**Chosen: hook the call sites in `Entities` and `BaselineRegistry`-using code.** All `applyMutation` traffic enters via `FieldChanges.applyTo(state)`, called from exactly four sites in `Entities.java`:

| Line | Site | Trace destination |
|------|------|-------------------|
| 492  | `executeEntityCreate`   | birth `setupMutations` |
| 527  | `executeEntityRecreate` | birth `setupMutations` |
| 551  | `executeEntityUpdate`   | `updateMutations` (the measured stream) |
| 677  | `getBaseline` (build)   | birth `setupMutations` for an EMPTY birth |

(`TempEntities.java:58` is excluded — temp entities are short-lived effects, not part of the steady-state workload.)

A minimal listener interface is added to `Entities` and the baseline-build path, null in production, set by the recorder at @Setup. The recorder also observes `setState(...)` to assign stateIds and emit `BirthRecipe` entries (EMPTY when the state came from `getEmptyState()`, COPY_OF when from `baseline.copy()` or `newState.copy()`).

The listener fields and a small adapter on `BaselineRegistry` are the only main-code changes required. No published API surface is affected.

## Iteration shape: split setup vs measurement

The hot loop measures **only update mutations**, against states already brought to their post-create shape. This is the whole point of the bench — diluting the measurement with `createState` and `copy()` calls (which fire ~1% of the time but cost ~10× a mutation) would re-introduce the same kind of noise floor we're trying to escape.

```
@Setup(Level.Invocation):
    states = new EntityState[birthCount];
    for (BirthRecipe b : births) {
        switch (b.kind) {
            case EMPTY   -> states[b.stateId] = S2EntityStateType.valueOf(impl)
                                .createState(b.field, ...);
            case COPY_OF -> states[b.stateId] = states[b.srcStateId].copy();
        }
        for (Mutation m : b.setupMutations)
            states[b.stateId].applyMutation(m.fp, m.mutation);
    }
    // At this point every state holds its post-create shape.
    // Pre-touch the updateMutations array to ensure it's tenured before the measured loop.

@Benchmark:
    for (Mutation m : updateMutations)
        states[m.stateId].applyMutation(m.fp, m.mutation);
```

Bias to be aware of: production updates land on entities at varying ages — a freshly-spawned hero gets its first updates against a sparse state, a long-lived `CGameRules` gets updates against a fully-populated state. Pre-applying all setup means the measured loop only sees updates against mature states. In practice, entities mature within seconds and the long tail of updates dominates, so the bias is small and skews toward the most representative shape.

`@Benchmark` does not allocate fresh states per iteration in the measured method — fresh-state setup happens in `@Setup(Level.Invocation)`. JMH's single-shot vs throughput mode tradeoff:

- **SingleShotTime** (current parse bench): measures one full iteration. For trace replay, "one iteration" = N mutations. Reports ms/iteration. Comparable to the parse bench.
- **AverageTime** with batchSize > 1: reports ns/op where op is "replay the full trace." Same information, different units.
- **Throughput**: ops/sec. Useful for cross-impl comparison.

**Chosen: SingleShotTime.** Same mode as existing bench → report shape is consistent, ReportWriter reuse is straightforward.

## Parameter sweep

```java
@Param({"NESTED_ARRAY", "TREE_MAP", "FLAT"}) String impl;
@Param({""}) String replay;                   // filled by Main from args
```

Mirrors `EntityStateParseBench`. Users can run `./gradlew bench -PbenchArgs="--trace"` (or a separate `./gradlew traceBench` task) to select this bench.

## Expected signal

Rough budget per mutation on current hardware with FLAT impl:
- Descent: 2-6 instanceof checks × ~1 ns = 2-6 ns
- Leaf write: byte[] access via VarHandle ≈ 5-10 ns
- Method call + frame setup ≈ 5 ns
- **Total ≈ 15-30 ns per mutation**

At a typical replay of ~1-2 million mutations, a full trace replay takes **15-60 ms**. Noise floor on a well-behaved JMH single-shot bench is ~1-2% — so we can resolve changes of **0.3 ms and up**, or about **10× finer resolution** than the parse bench for this workload.

If the numbers above are wrong by a factor of 3 in either direction, the bench still clears the parse bench's resolution floor.

## Validation steps (formerly open questions)

- **Trace memory.** Realistic estimate ~80-120 bytes per mutation once `StateMutation` objects and boxed value payloads are counted (the previous 32 B/entry figure ignored those). For a long replay this can reach ~1 GB of trace. The bench host has 64 GB, so v1 just gives the JMH fork a generous heap (e.g., `-Xmx16g`) and accepts the cost. No deduping, no primitive specialization, no slice cap in v1 — keep the trace shape simple and the workload faithful to whole-replay parsing. Memory micro-optimizations (interning, primitive-typed `WriteValue` variants, disk serialization) are listed in the v2/v3 evolution path and revisited only if the trace grows beyond what 16 GB comfortably holds.
- **Warmup.** Same `@Warmup(iterations=3)` SingleShotTime as the parse bench — three full trace replays at ~30 ms each is plenty for JIT to settle on the dispatch hot loop.
- **GC noise during replay.** `applyMutation` is allocation-light but not allocation-free (e.g., NESTED_ARRAY allocates `Object[]` sub-arrays when descending into uninitialized vectors). This allocation IS part of dispatch cost — it's signal we want to measure, not noise to suppress. Recorder allocations (the captured trace itself) are kept out of the measured window by pre-touching `updateMutations` at `@Setup(Level.Invocation)` to ensure the array is tenured before the hot loop. The trace itself is built once at `@Setup(Level.Trial)` and lives in the old generation throughout — it should not contribute to in-loop GC pressure even when sized in the hundreds of MB.

## Evolution path (explicitly not in this change)

- **v2 — disk-serialized trace.** Write the captured mutation stream to a binary file checked into the repo as a test fixture. Enables CI to run the bench without needing a large replay file and makes perf comparisons reproducible across machines. Requires a serialization format for `FieldPath` and `StateMutation`, plus a serializer-table header so `SwitchPointer` ids can be rehydrated.
- **v3 — per-entity-class slicing.** If v1 reveals large variance across entity classes (hero entities vs `CGameRules` vs combat-log entities), add `@Param("entityClass")` to measure each class in isolation.
- **v4 — descent-only mode.** If dispatch-loop work specifically becomes a perf suspect and the full-apply signal is still too noisy to isolate it, add a `@Param("mode")` that replaces every captured mutation with a no-op to measure descent in isolation. Explicitly scoped out of v1 — measuring something that never happens in the real workload is an anti-pattern we should only accept if there's no other way to get the number.
