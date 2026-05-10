## 1. Parser: new `Entities` query API

- [x] 1.1 Add `stream() : Stream<Entity>` to `Entities` using
      `IntStream.range(0, entityCount).mapToObj(this::getByIndex).filter(Objects::nonNull)`.
- [x] 1.2 Add static `byDtName(String) : Predicate<Entity>` to
      `Entities`.
- [x] 1.3 Remove `getAllByPredicate`, `getByPredicate`,
      `getAllByDtName`, `getByDtName` from `Entities`.
- [x] 1.4 Remove the `import skadistats.clarity.util.SimpleIterator;`
      line and any unused imports it leaves behind.

## 2. Parser: drop `SimpleIterator` from S1 entity states

- [x] 2.1 Inline `S1ObjectArrayEntityState.fieldPathIterator` as an
      anonymous `Iterator<FieldPath>` over `state.length`, including
      the `NoSuchElementException` guard in `next()`.
- [x] 2.2 Inline `S1FlatEntityState.fieldPathIterator` as an
      anonymous `Iterator<FieldPath>` over `layout.leaves().length`,
      including the `NoSuchElementException` guard.
- [x] 2.3 Update imports in both files: drop
      `skadistats.clarity.util.SimpleIterator`, add
      `java.util.NoSuchElementException`.

## 3. Parser: delete legacy util classes

- [x] 3.1 Delete `src/main/java/skadistats/clarity/util/SimpleIterator.java`.
- [x] 3.2 Delete `src/main/java/skadistats/clarity/util/Iterators.java`.

## 4. Parser: verify build

- [x] 4.1 `./gradlew build` clean in clarity (compile + tests +
      jmh compile).
- [x] 4.2 Confirm no remaining references to `SimpleIterator` or
      `util.Iterators` (`grep -r` in `src/`).

## 5. Examples: migrate callers

- [x] 5.1 In `examples/src/main/java/skadistats/clarity/examples/cooldowns/Cooldowns.java`,
      replace `entities.getByDtName("CDOTAGamerulesProxy")` with
      `entities.stream().filter(Entities.byDtName("CDOTAGamerulesProxy")).findFirst().orElse(null)`
      (or import-static the factory).
- [x] 5.2 In `examples/src/main/java/skadistats/clarity/examples/matchend/Main.java`,
      apply the same migration to the `getByDtName(...)` call site
      around line 140.
- [x] 5.3 `./gradlew build` in clarity-examples passed. Runtime
      spot-run skipped — migration is a 1:1 semantic rewrite
      (`getByDtName(x)` returned the first match or null;
      `stream().filter(byDtName(x)).findFirst().orElse(null)`
      returns the first match or null), so compile-clean is
      sufficient verification.

## 6. Downstream check (analyzer)

- [x] 6.1 Compile `clarity-analyzer` against the updated parser to
      confirm no unexpected breakage. (Do not launch the GUI;
      compile-only per project convention.) Compiles clean — no
      affected callers.

## 7. Documentation

- [x] 7.1 Add a `**Modernised Entities query API (BREAKING)**`
      section under `## Unreleased` in `clarity/CHANGELOG.md`
      following the existing format. Cover: removal of the four
      legacy methods (`getAllByPredicate`, `getByPredicate`,
      `getAllByDtName`, `getByDtName`), addition of `stream()` and
      `byDtName(...)`, the cardinality-explicit motivation, and
      the four-row migration table from the proposal.
- [x] 7.2 Add a brief mention of the deletion of
      `util.SimpleIterator` and `util.Iterators` to the same
      changelog entry (lower-impact, but technically a public
      `util` package removal).
- [x] 7.3 Add Javadoc on the new `Entities.stream()` and
      `Entities.byDtName(String)` describing iteration order,
      null-skipping behaviour, and the deliberate absence of a
      single-result helper (point readers at `findFirst`).
- [x] 7.4 Skim `clarity/README.md` and any per-package
      `package-info.java` for references to the old methods.
      No hits found.

## 8. Known external downstream

- [x] 8.1 Note `odota-parser` (5 `getByDtName` calls in
      `opendota.Parse` plus 1 in `bench/Probe.java`) as a known
      external downstream that breaks with this change. Out of
      scope — odota-parser maintainers migrate independently in
      their own repo. Documented in the proposal's Impact
      section.
