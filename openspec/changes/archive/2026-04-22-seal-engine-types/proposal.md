## Why

Three shared bases — `DTClass`, `FieldPath`, and `EntityState` — each sit above an S1 and an S2 concrete hierarchy. They serve two opposing needs at once: they act as opaque handles for user code (fine) and as cast-heavy escape hatches for internal code that statically knows its engine (not fine). The result is `evaluate(Function, Function)` dispatch, `.s1() / .s2()` default casts on every interface, and per-field-path casts on every packet entity update in the hot path.

We want to make the sum type honest — a sealed closed hierarchy the compiler can check — and eliminate the per-field-path casts that the interface erasure forces on the field readers and entity state implementations.

## What Changes

- **BREAKING** `DTClass` becomes `sealed permits S1DTClass, S2DTClass`. `DTClass.evaluate(Function, Function)` and the `DTClass.s1() / s2()` default casts are removed. Callers that need engine-specific behaviour use exhaustive `switch` over the sealed type.
- **BREAKING** `FieldPath` becomes `sealed permits S1FieldPath, S2FieldPath`. `FieldPath.s1() / s2()` default casts are removed.
- **BREAKING** `EntityState` becomes `sealed permits S1EntityState, S2EntityState`. Engine-specific write methods (`write`, `decodeInto`, `applyMutation`) move to the sub-interfaces and take the engine-typed `FieldPath` directly. Shared reads (`getValueForFieldPath(FieldPath)`, `fieldPathIterator`, `copy`, `dump`) stay on the base.
- `FieldReader` signatures tighten at the subclass level: `S1FieldReader` consumes `S1DTClass` and `S1EntityState` without per-call casts; same for S2. Internal `fieldPaths[]` arrays become engine-typed.
- Build toolchain bumps to JDK 21 with `options.release = 21`. Exhaustive switch over sealed types requires `--source 21`, which `javac` couples to `--target 21`; you cannot have Java 21 syntax with Java 17 bytecode. The minimum supported runtime bumps to Java 21. Consumers of the jar (clarity-analyzer, user projects) must use JDK 21.
- User-facing surface is unchanged: `Entity.getState()`, `Entity.getPropertyForFieldPath(FieldPath)`, `@OnEntityPropertyChanged` callbacks, and all example code continue to treat `FieldPath` as opaque.

## Capabilities

### New Capabilities
- `sealed-engine-types`: the closed sum-type hierarchy spanning `DTClass`, `FieldPath`, and `EntityState`, and the rules for how internal callers dispatch across engines.

### Modified Capabilities
- `state-mutation`: the `EntityState.applyMutation(FieldPath, StateMutation)` requirement moves from the base `EntityState` interface onto the sealed engine-specific sub-interfaces (`S1EntityState.applyMutation(S1FieldPath, …)`, `S2EntityState.applyMutation(S2FieldPath, …)`). Behavior, return contract, and StateMutation shape are unchanged.

## Impact

- **Build**: `clarity/build.gradle.kts` bumps toolchain to 21 and sets `options.release = 21`. Published jar requires Java 21 at runtime.
- **clarity internals** (~15 files): `DTClass.java`, `FieldPath.java`, `EntityState.java`, 6 state impls, `FieldReader.java` + S1/S2 subclasses, `Entity.java`, `processor/entities/Entities.java` (~4 boundary adjustments).
- **clarity-analyzer** (`ObservableEntity.java`, 1 file): rewrite the one `dtClass.evaluate(...)` call and the `fp.s1() / fp.s2()` call sites to exhaustive switch. The cast-to-`S1DTClass` lines become pattern-matching `instanceof`.
- **clarity-examples**: no changes required. No example casts `FieldPath` or `DTClass`.
- **Performance**: removes 2–3 casts per field per packet entity update. Expected 1–3% parse-time improvement on dense Dota replays (needs JMH confirmation in the benchmark harness).
- **Downstream API**: `Entity`, `EntityState.getValueForFieldPath`, and `@OnEntityPropertyChanged` remain binary-compatible for user code using them opaquely. Downstream code that called `DTClass.evaluate`, `DTClass.s1()/.s2()`, or `FieldPath.s1()/.s2()` must migrate to `switch` / `instanceof`. Only one known downstream (clarity-analyzer) is affected and is updated in the same change.
