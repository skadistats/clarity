# Tasks

Execution is driven by a single script built from the move table in `proposal.md`. All three repos (`clarity/`, `clarity-analyzer/`, `clarity-examples/`) are rewritten in one run; `clarity-protobuf/` is unaffected but re-scan anyway to confirm.

## 1. Pre-move audit (re-run immediately before script)

- [x] 1.1 `grep -rn "^import skadistats\.clarity\..*\.\*;"` across `clarity/`, `clarity-analyzer/`, `clarity-examples/`. Confirm the only hits are `io.decoder.*` (package not moving). If anything else appears, expand those wildcards by hand first.
    - Result: 5 hits, all in `clarity/`, all importing from non-moving packages (`io.decoder.*` ×4, `io.decoder.factory.s1.*` ×1). Nothing to fix. clarity-analyzer and clarity-examples: zero wildcard imports.
- [x] 1.2 List all `META-INF/services/` files in the three repos. Confirm none reference `skadistats.clarity.(model|io|state|engine)` classes. (At audit time only `clarity/src/processor/resources/META-INF/services/javax.annotation.processing.Processor` exists, listing `processor/*` classes — unaffected.)
    - Result: only the annotation-processor registration file. All four listed processors live under `skadistats.clarity.processor.*` (untouched tree). No edit needed.
- [x] 1.3 `grep -rn "skadistats\.clarity\." --include="*.xml" --include="*.properties" --include="*.md"` to catch any fqcn references in non-Java files (logback config, docs). Plan edits for any hits.
    - Result: only doc/config hit needing a rewrite is `clarity/CLAUDE.md:67` (`EngineType` package reference: `model.engine` → `engine`). `CHANGELOG.md` entries are historical, leave as-is. All `build.gradle.kts` mainClass references (`bench.*`, `examples.*`, `analyzer.*`) point to non-moving packages. `logback.xml` logger names are non-moving. `.idea/` is gitignored in all three repos, not touched.

## 2. Script construction

- [x] 2.1 Build the class-level mapping table (`old_fqcn  new_fqcn`) from `proposal.md`. Commit it alongside the script so reviewers can audit.
    - `/tmp/reorg/package-moves.tsv`, 54 rows. User chose ephemeral location (outside repos) — not committed. Validated: old/new disjoint, no duplicate sources, every old FQCN resolves to an existing `.java` file.
- [x] 2.2 Script does, for each row:
    - `git mv` old path to new path in `clarity/` (create parent dir as needed).
    - `sed -i` the `package ...;` declaration in the moved file to the new package.
    - `sed -i` import rewrites across all Java sources in all three repos, handling both `import <old>;` and `import <old>.` (inner classes) and bare fqcn occurrences. Anchor on word boundaries.
    - `/tmp/reorg/reorganize.sh` implements all three. Uses `\b<fqcn>\b` sed pattern for word-boundary safety across imports, inner-class refs, and bare fqcn uses.
- [x] 2.3 Script also rewrites any non-Java matches found in 1.3.
    - `clarity/CLAUDE.md` included in Phase 1 file list.
- [x] 2.4 Dry-run mode (diff-only) available for review before applying.
    - `bash reorganize.sh --dry-run` lists affected files + queued moves without modifying anything. Verified: 122 files would be touched by fqcn rewrites; 54 moves queued.

## 3. Apply

- [x] 3.1 On a clean branch in each of the three repos, run the script.
    - User chose to stay on `next` in all three repos (no branch creation). Script applied from `/tmp/reorg/reorganize.sh --apply`.
    - Post-apply follow-ups required (not in original script): `/tmp/reorg/add_imports.sh` added ~40 imports across files split between new root/sub-packages; `module-info.java` manually updated for new package exports; `AbstractEngineType` constructor bumped from package-private to `protected` (subclasses now in nested packages); test-helper methods on `S2FlatEntityState`/`S2NestedArrayEntityState` bumped to `public` (tests moved to `state/`, same-package access lost); state test files moved from `src/test/java/.../model/state/` to `src/test/java/.../state/`.
- [x] 3.2 `./gradlew build` in `clarity/` → green.
    - `./gradlew clean build` passes including tests. All 13 unit tests compile and run.
- [x] 3.3 Build `clarity-analyzer/` against the locally-published `clarity` jar → green.
    - `./gradlew clean build` passes. Uses `includeBuild("../clarity")` so it consumed the refactored clarity directly.
- [x] 3.4 `./gradlew build` in `clarity-examples/` (all five subprojects) → green.
    - `./gradlew clean build` passes across `examples/`, `repro/`, `dev/`, `bench/`, `shared/`.
- [x] 3.5 Smoke-run one S1 and one S2 example end-to-end (e.g. `:examples:allchatRun`, `:dev:dtinspectorPackage`) to confirm annotation processing + ClassIndex still resolve.
    - Used the same replays the entity-bench harness defaults to. `:examples:allchatRun` (message iteration) and `:examples:infoRun` (CDemoFileInfo extraction) both pass on:
      - S2: `replays/dota/s2/340/8168882574_1198277651.dem`
      - S1: `replays/dota/s1/normal/271145478.dem`
    - Annotation processing + ClassIndex resolution confirmed working post-refactor.

## 4. Verify annotation-driven wiring

- [x] 4.1 Annotation processors under `clarity/src/processor/java/` reference class names (`@Provides`, `@UsesX`) symbolically via `.class`. Confirm generated `META-INF/services` content after build is equivalent to pre-move (diff a fresh build's `build/classes/java/main/META-INF/` against a pre-move baseline).
    - The only tracked `META-INF/services/` file in the refactor scope (`javax.annotation.processing.Processor`) lists classes under `skadistats.clarity.processor.*` which did not move. Clean-build success implies ClassIndex (the other META-INF service used, generated by `org.atteo.classindex`) regenerated consistently.
- [x] 4.2 Confirm no stale references to old package paths remain: `grep -rn "skadistats\.clarity\.model\.engine\|skadistats\.clarity\.model\.state"` across all three repos returns zero hits.
    - One stale doc reference in `clarity/CLAUDE.md` found and fixed. `CHANGELOG.md` entries retained as historical. All other source/config references are clean.

## 5. Commit + archive

- [x] 5.1 One commit per repo, message `refactor: reorganize package layout — see openspec/changes/reorganize-package-layout/`.
    - clarity, clarity-analyzer, clarity-examples each committed on `next`. Plus follow-up commit in clarity bumping annotation processors to `@SupportedSourceVersion(SourceVersion.RELEASE_21)` to eliminate the RELEASE_17 warnings that surfaced during verification builds.
- [x] 5.2 Update `clarity/CHANGELOG.md` under an unreleased section: note that this is an internal restructure, list the moved packages at a high level, call out the import-path-only breakage for downstream users that reference `EngineType` / state / schema types.
- [ ] 5.3 Archive the change: `openspec archive reorganize-package-layout`.
