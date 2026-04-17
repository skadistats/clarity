## Context

Three shared types — `DTClass`, `FieldPath`, and `EntityState` — live above parallel S1/S2 concrete hierarchies in the `skadistats.clarity.model` and `skadistats.clarity.io` packages. Each exists for two reasons:

1. **Opaque handle for user-facing code.** Examples (`examples/`, user projects) pass `FieldPath` and `DTClass` through `Entity` without ever introspecting them.
2. **Shared signature for internal dispatch.** The abstract `FieldReader.readFields(DTClass, EntityState, …)` and the polymorphic `EntityState.applyMutation(FieldPath, …)` both accept the shared types so that a single call site covers both engines.

Job 1 is paying its way. Job 2 is not: the field readers, state impls, and `Entities` container *statically* know their engine (there's one reader per engine, one state impl lineage per engine, one replay is one engine from the first tick to the last), yet the API forces per-field-path casts on the hot path. An audit of call sites shows:

- `DTClass.s1() / s2()` / `DTClass.evaluate(…)` / cast-to-concrete: 6 call sites in clarity, 1 in clarity-analyzer (`ObservableEntity.createProperty`), 3 in clarity-examples/dev (dtinspector). Zero in user-facing examples.
- `FieldPath.s1() / s2()` / cast-to-concrete: 16 call sites, all inside clarity internals and clarity-analyzer. Zero in user-facing examples.

Per-packet-entity-update cost is 2–3 casts per field in the readers and state impls. On a dense Dota tick (~100 entities × ~20 changed fields), that's 4k–6k casts per tick.

Java 17 finalised sealed types and pattern matching for `instanceof`. Java 21 finalised exhaustive `switch` over sealed hierarchies. Clarity's toolchain currently pins 17 with no `release` target set; bumping the toolchain to 21 while setting `options.release = 17` lets us use exhaustive switch syntax while still emitting class files that run on Java 17 JVMs. No post-17 library APIs are used.

## Goals / Non-Goals

**Goals:**
- Make the sum-type nature of the three shared bases honest: sealed hierarchies the compiler closes.
- Remove `DTClass.evaluate(Function, Function)` and the `.s1() / .s2()` default-cast helpers — replace every current caller with exhaustive `switch` or `instanceof`.
- Move engine-specific write methods (`write`, `decodeInto`, `applyMutation`) off the shared `EntityState` base and onto sealed `S1EntityState` / `S2EntityState` sub-interfaces, so implementations no longer cast `FieldPath` on each call.
- Eliminate per-field-path casts in the `FieldReader` hot path by narrowing the state reference once at the readFields boundary, then invoking engine-typed methods inside the loop.
- Preserve the user-facing API surface. `Entity.getState()`, `Entity.getPropertyForFieldPath(FieldPath)`, `EntityState.fieldPathIterator()`, `@OnEntityPropertyChanged` callbacks, all example code — binary-compatible.

**Non-Goals:**
- Parameterising `Entity<E>` or the `Entities` container by engine family. That would be viral into every example and isn't justified by the cast cost at those boundaries.
- Changing `FieldChanges` or `StateMutation` shapes. `FieldChanges.fieldPaths[]` can stay typed as `FieldPath[]` because array covariance lets engine-typed readers populate it — the runtime type is correct, the declared type stays shared.
- Changing `getValueForFieldPath(FieldPath)` on the base interface. It is the one *genuinely* polymorphic method on `EntityState` that user code relies on; making it sub-interface-only would break the user-facing opaque-handle contract.
- Benchmark-driven micro-optimisation beyond removing the cast. A JMH check is a validation step, not a goal.
- Touching clarity-examples source (no cast sites to migrate there).

## Decisions

### Decision 1: Seal all three shared bases in one change

`DTClass`, `FieldPath`, and `EntityState` become sealed together. They share a motivation (same three-way sum-type pattern), the migration rhythm is identical (declare sealed, rewrite cast callers), and splitting the work creates artificial seams — "why is `EntityState` still open after we sealed the other two?"

Alternative considered: seal `DTClass` and `FieldPath` first, leave `EntityState` for a follow-up. Rejected because the `EntityState` work is what actually removes the hot-path casts. Without it, Move 1 is a typing clean-up with no measurable payoff.

### Decision 2: Split `EntityState` rather than parameterise it

Use:

```java
sealed interface EntityState permits S1EntityState, S2EntityState { … shared reads … }
sealed interface S1EntityState extends EntityState permits S1FlatEntityState, S1ObjectArrayEntityState {
    boolean write(S1FieldPath fp, Object decoded);
    boolean decodeInto(S1FieldPath fp, Decoder d, BitStream bs);
    boolean applyMutation(S1FieldPath fp, StateMutation m);
}
sealed interface S2EntityState extends EntityState permits S2FlatEntityState, S2NestedArrayEntityState, S2TreeMapEntityState { … S2FieldPath-typed … }
```

Alternative considered: `EntityState<FP extends FieldPath>` parametric. Rejected — generics are viral, `FieldChanges`, `StateMutation`, `Entity`, and the `Entities` container all hold `EntityState` references and would need type parameters or wildcards. The sealed split keeps every reference point monomorphic at its natural granularity.

Alternative considered: leave `EntityState` fully shared with `FieldPath`-typed methods, remove casts by some other means (e.g. reflection, method handles). Rejected — adds machinery that buys nothing the type system wouldn't buy for free.

### Decision 3: Cast-once at the FieldReader boundary, not generic `FieldReader<S>`

`FieldReader.readFields(DTClass, EntityState, …)` remains a shared-type abstract signature. Inside `S1FieldReader.readFields`, the first two lines cast the incoming `DTClass` and `EntityState` to `S1DTClass` / `S1EntityState`. The per-field-path loop then uses engine-typed state references with no casts.

Alternative considered: `abstract class FieldReader<D, FP, S>` parametric. Rejected — forces `Entities.fieldReader` to become `FieldReader<?, ?, ?>`, which then needs wildcards at every call site. The cast-at-entry approach pays one cast per `readFields` call, amortised over ~20 fields, and keeps the abstract signature simple.

### Decision 4: Replace `DTClass.evaluate(Function, Function)` with exhaustive `switch`

Delete `evaluate(s1Fn, s2Fn)` and the `DTClass.s1() / s2()` / `FieldPath.s1() / s2()` defaults. Rewrite the ~9 call sites across clarity internals and clarity-analyzer. Example:

```java
// Before
var typeInfo = dtClass.evaluate(
    s1 -> s1.getReceiveProps()[fp.s1().idx()].getSendProp().getType(),
    s2 -> s2state.getTypeForFieldPath(fp.s2())
);

// After
var typeInfo = switch (dtClass) {
    case S1DTClass s1 -> s1.getReceiveProps()[((S1FieldPath) fp).idx()].getSendProp().getType();
    case S2DTClass s2 -> s2state.getTypeForFieldPath((S2FieldPath) fp);
};
```

Where a `FieldPath` is accessed alongside a sealed `DTClass`, the cast to `S1FieldPath` / `S2FieldPath` is still explicit at the specific call site — sealed dispatch does not transitively propagate engine knowledge. This is fine: these are ~15 sites, not thousands, and `ClassCastException` on a mismatched engine is impossible in practice (one replay is one engine).

Alternative considered: keep `evaluate(Function, Function)` because it has exactly one caller and the syntactic cost is real. Rejected — keeping the method keeps the whole "sum type via lambdas" pattern alive as a template others may reach for. One caller is easy to migrate; the long-term tax of keeping it as API surface is higher.

### Decision 5: Toolchain 21, release 17

`clarity/build.gradle.kts` gains:

```kotlin
java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }
tasks.withType<JavaCompile> { options.release = 17 }
```

Gradle's toolchain auto-discovery on Arch finds `/usr/lib/jvm/java-{17,21}-openjdk` without downloading. Setting `org.gradle.java.installations.auto-download=false` in `gradle.properties` makes missing JDKs fail loudly instead of silently pulling from disco.foojay.io.

Alternative considered: bump both toolchain and release to 21. Rejected — strands any downstream still on 17 (including older clarity-analyzer deployments, user projects) for zero benefit. All the language features we use compile to plain Java 17 bytecode.

## Risks / Trade-offs

- **Sealed `permits` list maintenance** → Every new state impl (e.g. the completed `accelerate-s1-flat-state` work, any future variant) must add itself to the `permits` clause on `S1EntityState` / `S2EntityState`. Mitigation: accepted chore; consistent with the pattern already established for `StateMutation`. The compile error when omitted is clear and local.
- **Source-compat break for downstream callers of `DTClass.evaluate` / `.s1()` / `.s2()`** → Anyone outside clarity + clarity-analyzer who used these helpers will fail to compile. Mitigation: the only known downstream is clarity-analyzer, which is updated in the same change. Mention in CHANGELOG.md; flag breaking change markers on the proposal.
- **Performance win may be smaller than estimated** → JIT routinely devirtualizes monomorphic casts. The 1–3% estimate is a ceiling, not a floor. Mitigation: before-and-after JMH run on the benchmark harness. If the win is below 0.5% and not repeatable, we still keep the change for the exhaustiveness benefit — we do not lose if the perf delta is noise.
- **Maintainers who rebuild without JDK 21 installed** → Gradle would auto-download unless `auto-download=false` is set. Mitigation: set it. Document the requirement in README.md and CLAUDE.md. On Arch, `pacman -S jdk21-openjdk` is the one-liner.
- **`FieldPath` cast still appears at some sites** → After sealing, a caller pattern-matching on `DTClass` still has to cast a separate `FieldPath` parameter (sealed dispatch is not transitive across method parameters). The cast is now checked against a closed hierarchy (`ClassCastException` at runtime instead of silent coercion), but the syntactic cast remains. Mitigation: accept; this is 15 sites, not thousands, and a bad-engine `FieldPath` is not a real scenario.

## Migration Plan

1. **Toolchain bump**: edit `build.gradle.kts`, add `options.release = 17`, bump toolchain to 21. Verify `./gradlew build` still produces Java 17 bytecode (`javap -v` on an output class shows `major version: 61`).
2. **Seal `DTClass` and `FieldPath`**: add `sealed permits`, remove default methods. Fix the ~10 cast sites in clarity internals + dev tools + clarity-analyzer with `instanceof` patterns / `switch`. All tests green before proceeding.
3. **Split `EntityState`**: add `S1EntityState` and `S2EntityState` sealed sub-interfaces. Move `write / decodeInto / applyMutation` to sub-interfaces. Update 6 concrete state impls to implement the right sub-interface and drop internal `fpX.s1()` / `.s2()` casts.
4. **Narrow the hot path**: `S1FieldReader.readFields` and `S2FieldReader.readFields` cast once at entry (`var state = (S1EntityState) entityStateGeneric;` etc.), then call typed methods. `Entities.applySetupChanges` / `applyUpdateChanges` narrow state once before their loop.
5. **Clarity-analyzer**: rewrite `ObservableEntity.createProperty` dispatch to exhaustive `switch`. Replace the one `dtClass.evaluate(…)` call. Replace the three `((S1DTClass) dtClass).…` casts with `instanceof` narrowing.
6. **Benchmark**: run the existing JMH harness (`./gradlew bench`) on a standard dense Dota replay before and after. Record the result in the change's tasks; archive into the memory directory if noteworthy. Do not gate the change on a specific perf delta.
7. **CHANGELOG + migration note**: document the removed `DTClass.evaluate`, `DTClass.s1() / s2()`, `FieldPath.s1() / s2()` surface so downstream users see the breakage up front.

Rollback: all changes are on a single branch. Revert the branch if post-merge issues appear. No data-format changes, no on-disk compatibility concerns, no persistence migration.

## Open Questions

- Do we want `gradle.properties` to force `org.gradle.java.installations.auto-download=false`, or leave it at default (allow auto-download)? Maintainer-machine hygiene vs. first-time-builder convenience. Recommend setting it to false and documenting the requirement, but willing to hear the other side.
- Should the JMH before/after run be added to `bench-results/` as an archived data point, or treated as a one-off sanity check? No existing convention clearly covers "perf delta captured for a single code change."
