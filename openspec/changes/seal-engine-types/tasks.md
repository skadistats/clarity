## 1. Toolchain bump

- [x] 1.1 Bump `clarity/build.gradle.kts` toolchain to 21, set `options.release = 21` (target Java 21 fully — exhaustive switch requires source=21, javac couples source and target, so release floor moves to 21)
- [x] 1.2 `clarity/gradle.properties` already has `auto-download=false`
- [x] 1.3 `./gradlew build` succeeds with local JDK 21
- [x] 1.4 Published classes are major version 65 (Java 21 bytecode)
- [x] 1.5 Smoke-tested via `./gradlew test` — all tests pass against Java 21 runtime (Java 17 smoke moot: jar requires 21 now)

## 2. Seal DTClass

- [x] 2.1 `DTClass` declared `sealed permits S1DTClass, S2DTClass`
- [x] 2.2 `DTClass.evaluate(...)` deleted
- [x] 2.3 `DTClass.s1()` / `s2()` default methods deleted
- [x] 2.4 `@Deprecated collectFieldPaths` deleted (nothing still called it)
- [x] 2.5 `S1FieldReader.readFields` casts `dtClassGeneric` once at entry
- [x] 2.6 `S2FieldReader.readFields` / `readFieldsFast` / `readFieldsMaterialized` / `readFieldsDebug` cast `dtClassGeneric` once at entry each
- [x] 2.7 `S1DTClass` and `S2DTClass` marked `final`

## 3. Seal FieldPath

- [x] 3.1 `FieldPath` declared `sealed permits S1FieldPath, S2FieldPath`
- [x] 3.2 `FieldPath.s1()` / `s2()` default methods deleted
- [x] 3.3 `S1FieldPath` marked `final`. `S2FieldPath` sealed permits `S2LongFieldPath, S2ModifiableFieldPath`. `S2ModifiableFieldPath` sealed permits `S2LongModifiableFieldPath`. `S2LongFieldPath` and `S2LongModifiableFieldPath` marked `final`.

## 4. Migrate DTClass / FieldPath cast sites — clarity internals

- [x] 4.1 `S1DTClass.getNameForFieldPath` uses `((S1FieldPath) fp).idx()`
- [x] 4.2 `S1FieldReader` loop indexes typed `S1FieldPath` via a single entry cast; one `(S1FieldPath)` cast remains in debug/materialize helpers (non-hot)
- [x] 4.3 `S2FieldReader` loop uses `(S2FieldPath) fieldPaths[r]` after the single entry cast; amortised over n fields
- [x] 4.4 `Entity.getNameForFieldPath` / `getFieldPathForName` use exhaustive `switch` over sealed `EntityState`

## 5. Split EntityState into sealed sub-hierarchies

- [x] 5.1 `EntityState` declared `sealed permits S1EntityState, S2EntityState`
- [x] 5.2 `write` / `decodeInto` / `applyMutation` removed from base `EntityState`
- [x] 5.3 `S1EntityState` sealed permits `S1FlatEntityState, S1ObjectArrayEntityState`; declares the three write methods typed `S1FieldPath`
- [x] 5.4 `S2EntityState` sealed permits `S2AbstractEntityState`; concrete class list lives on the abstract class (`S2FlatEntityState, S2NestedArrayEntityState, S2TreeMapEntityState`); methods typed `S2FieldPath`
- [x] 5.5 `S1FlatEntityState` and `S1ObjectArrayEntityState` implement `S1EntityState`; signatures accept `S1FieldPath`; internal casts removed
- [x] 5.6 `S2FlatEntityState`, `S2NestedArrayEntityState`, `S2TreeMapEntityState` extend `S2AbstractEntityState`; signatures accept `S2FieldPath`; internal casts removed
- [x] 5.7 `S2AbstractEntityState` declared `sealed abstract` with the three concrete permits; declares abstract engine-typed methods
- [x] 5.8 `EntityStateFactory.forS1` / `forS2` return types unchanged (still the base `EntityState`) since callers bind to sub-interfaces via the sealed switch; widening is still valid
- [x] 5.9 Added static helper `EntityState.applyMutation(state, fp, mutation)` for one-shot dispatch used by jmh trace benches

## 6. Narrow FieldReader hot path

- [x] 6.1 `S1FieldReader.readFields` casts `state` to `S1EntityState` once at entry
- [x] 6.2 `S2FieldReader.readFieldsFast` / `readFieldsMaterialized` / `readFieldsDebug` cast `state` once at entry (to `S2AbstractEntityState` because the debug paths need `getNameForFieldPath` / `getTypeForFieldPath`)
- [x] 6.3 Not done — kept shared `FieldPath[] fieldPaths` and cast `(S1FieldPath)` / `(S2FieldPath)` once per field at use site. Typed shadow fields would duplicate storage for no measurable gain; revisit if JMH shows cost.
- [x] 6.4 No `.s1()` / `.s2()` calls remain anywhere (Grep confirmed)

## 7. Narrow Entities container call sites

- [x] 7.1 `applySetupChanges` uses exhaustive `switch` on sealed `EntityState`, narrows once before the loop
- [x] 7.2 `applyUpdateChanges` same pattern
- [x] 7.3 All `state.applyMutation` call sites compile against the sub-interface
- [x] 7.4 `Entity.getNameForFieldPath` / `getFieldPathForName` dispatch via exhaustive `switch` over sealed `EntityState`
- [x] 7.5 `FieldChanges.applyTo` uses exhaustive `switch` on sealed `EntityState`

## 8. Migrate clarity-analyzer

- [x] 8.1–8.4 `ObservableEntity.java` migrated: `createProperty` uses exhaustive `switch (dtClass)`; name/path helpers use exhaustive `switch (dtClass)`; FieldPath accesses use `(S1FieldPath) fp` / `(S2FieldPath) fp` inside the sealed branch
- [x] 8.5 `./gradlew compileJava` in clarity-analyzer green
- [x] 8.6 clarity-analyzer toolchain bumped to JDK 21
- [x] 8.7 Lombok plugin bumped from `io.freefair.lombok:8.0.1` to `8.10` — 8.0.1 uses a pre-21 Lombok that fails on JDK 21's `JCTree.JCImport.qualid` change

## 9. Verify clarity-examples

- [x] 9.1 `./gradlew build` in clarity-examples green with zero source edits
- [x] 9.2 No `.s1()` / `.s2()` / `evaluate()` in any example source
- [x] 9.3 Smoke-test examples against a real replay — allchat, matchend, positionRun all clean on `replays/dota/s2/normal/1560294294.dem`
- [x] 9.4 dtinspector compiled via `./gradlew :dev:dtinspectorPackage` as part of `./gradlew build`
- [x] 9.5 clarity-examples toolchain bumped to JDK 21 in both root `build.gradle.kts` and `build-logic/src/main/kotlin/examples-base.gradle.kts`

## 10. Benchmark

- [x] 10.1 JMH baseline — `6ddc606` (pre-seal base, JDK 17 toolchain): `./gradlew bench -PbenchArgs="--all"` + `./gradlew s1Bench` → `bench-results/2026-04-17_152830_HEAD-6ddc606/` and `bench-results/2026-04-17_153611_s1_HEAD-6ddc606/`
- [x] 10.2 Post-change JMH — `00bf3c8` (HEAD, JDK 21 toolchain): same commands → `bench-results/2026-04-17_151826_next-00bf3c8/` and `bench-results/2026-04-17_152500_s1_next-00bf3c8/`
- [x] 10.3 Record perf delta — see `BENCH.md` in this change dir. Headline: S2 FLAT Dota -6.0%, Deadlock -1.5%, CS2 -2.7%; S1 FLAT Dota -7.5%, CSGO -2.3%. Allocations unchanged across the board (sealing is type-system only, no alloc deltas expected). Note: JDK 17→21 toolchain bump is part of the change and contributes to the delta; effect is not isolable to sealing alone.

## 11. Documentation

- [x] 11.1 CHANGELOG entry
- [x] 11.2 CLAUDE.md / README.md note on JDK 21 floor (clarity + clarity-examples)
- [x] 11.3 clarity-analyzer CHANGELOG — N/A (no CHANGELOG file exists)
