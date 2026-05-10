# Tasks

Mechanical rename change. Order matters: protobuf first (everything imports
generated wire classes), then parser, then downstream consumers.

## 1. clarity-protobuf — proto tree restructure

- [x] 1.1 Move `proto/csgo/common/` → `proto/cs/common/` (`git mv`)
- [x] 1.2 Move `proto/csgo/s1/` → `proto/cs/csgo/` (`git mv`)
- [x] 1.3 Move `proto/csgo/s2/` → `proto/cs/cs2/` (`git mv`)
- [x] 1.4 Update `option java_package` in every moved `.proto`:
      - `cs/common/*.proto`: `wire.csgo.common.proto` → `wire.cs.common.proto`
      - `cs/csgo/*.proto`:   `wire.csgo.s1.proto`     → `wire.cs.csgo.proto`
      - `cs/cs2/*.proto`:    `wire.csgo.s2.proto`     → `wire.cs.cs2.proto`
- [x] 1.5 Update `option java_outer_classname` per the rename table in proposal.md
      (CSGOCommonGcMessages → CsCommonGcMessages, CSGOS1NetMessages → CsgoNetMessages, etc.)
- [x] 1.6 Update `build.json` — `"path"` entries and `"include"` arrays for the cs builds
- [x] 1.7 Run `make.sh` (or whatever the proto compile invocation is); ensure clean compile
- [x] 1.8 Delete the now-empty `proto/csgo/` and any stale generated `wire/csgo/` outputs (also: moved hand-written `EmbeddedPackets.java` × 2 and `UserMessagePackets.java` to `wire/cs/csgo/` and `wire/cs/cs2/` with package + import + class-ref rewrites; updated `module-info.java` exports)
- [x] 1.9 Update `tools/proto-sync/mapping.json` path entries (left-side keys: `csgo/{common,s2}/...` → `cs/{common,cs2}/...`; right-side upstream `csgo/...` paths intentionally untouched — those are Valve's directory layout)
- [x] 1.10 Rewrite `tools/proto-sync/RUNBOOK.md` sections:
      - "Don't touch Source 1" → "Don't touch the legacy game directories" (now describes the dota/cs asymmetry: dota = engine-split, cs = product-split)
      - "Common vs. S2 directories" → "Common vs. version-specific directories" (covers both families, with their distinct semantics)
      - Step 3 `csgo/s2/message_id.proto` → `cs/cs2/message_id.proto`
      - Step 6 `wire/csgo/s2/EmbeddedPackets.java` → `wire/cs/cs2/EmbeddedPackets.java`
      - Naming-conventions table: `CSGOCommonUserMessages` → `CsCommonUserMessages`
- [x] 1.11 No publish needed for local dev — clarity, clarity-analyzer, and clarity-examples all composite-include `clarity-protobuf` and pick up the new wire classes directly from sources. (Publish step is only for eventual release.)

## 2. clarity (parser) — EngineId + class renames

- [x] 2.1 `EngineId.java`: rename `CSGO_S1` → `CSGO`, `CSGO_S2` → `CS2` (IDE)
- [x] 2.2 `engine/s1/CsGoS1EngineType.java` → `engine/s1/CsgoEngineType.java` (IDE)
- [x] 2.3 `engine/s1/PacketInstanceReaderCsGoS1.java` → `engine/s1/PacketInstanceReaderCsgo.java` (IDE)
- [x] 2.4 `engine/s2/CsgoS2EngineType.java` → `engine/s2/Cs2EngineType.java` (IDE)
- [x] 2.5 `EngineMagic.java`: imports + constructor args updated (IDE); also renamed the local `EngineMagic.CSGO_S1` enum value → `CSGO` (post-IDE cleanup — separate enum, missed by IDE refactor)
- [x] 2.6 `model/csgo/PlayerInfoType.java` → `model/cs/PlayerInfoType.java` (IDE)
- [x] 2.7 `module-info.java`: `exports skadistats.clarity.model.csgo` → `exports skadistats.clarity.model.cs` (IDE)
- [x] 2.8 `processor/stringtables/PlayerInfo.java`, `processor/stringtables/OnPlayerInfo.java`: imports updated (IDE)
- [x] 2.9 Mechanical sweep — all `EngineId.CSGO_S1` / `EngineId.CSGO_S2` switch sites updated (IDE); also renamed `FieldGeneratorPatches.PATCHES_CSGO_S2` → `PATCHES_CS2` (post-IDE cleanup — private field, name still legacy)
- [x] 2.10 Wire-class imports throughout updated to new `wire.cs.{common,csgo,cs2}.proto.*` paths (IDE auto-import)
- [x] 2.11 `./gradlew :compileJava :compileTestJava test` — BUILD SUCCESSFUL
- [x] 2.12 Smoke test passed via `:examples:gameeventRun`:
      - CSGO: `csgo/s1/issue-271/astralis-vs-godsent-m1-nuke.dem` (228MB) parsed in 0.426s, game events streaming cleanly
      - CS2: `csgo/s2/prelaunch/003628632841199288407_0580788690.dem` parsed in 0.169s, game events streaming cleanly
      Full pipeline exercised: EngineMagic dispatch, renamed engine classes, EngineId propagation through processors, wire-class loading from new `wire.cs.*` packages.

## 3. clarity-analyzer — downstream alignment

- [x] 3.1 `binding/CSGOS1BindingGenerator.java` → `binding/CsgoBindingGenerator.java` (IDE)
- [x] 3.2 `binding/CSGOS2BindingGenerator.java` → `binding/Cs2BindingGenerator.java` (IDE)
- [x] 3.3 `position/CSGOS1PositionBinder.java` → `position/CsgoPositionBinder.java` (IDE)
- [x] 3.4 `position/CSGOS2AndDeadlockPositionBinder.java` → `position/Cs2AndDeadlockPositionBinder.java` (IDE)
- [x] 3.5 `icon/csgo/PlayerIcon.java` → `icon/PlayerIcon.java` (IDE)
- [x] 3.6 References and imports updated (IDE auto-update)
- [x] 3.7 `MapControl.java` switch sites updated (IDE)
- [x] 3.8 `./gradlew compileJava` — BUILD SUCCESSFUL

## 4. clarity-examples — minor import surgery

- [x] 4.1 Renamed `dev/.../csgo2test/` → `dev/.../cstest/` (`git mv`); updated package declaration and `@Example(name = "cstest")`. Import `model.csgo.PlayerInfoType` → `model.cs.PlayerInfoType` already done by user via IDE. Local `Replay CSGO_S1` / `CSGO_S2` constants renamed to `CSGO` / `CS2` (and string labels in B_* Replays from `"CSGO_S2"` to `"CS2"`).
- [x] 4.2 `dev/test/Main.java`: import updated (IDE)
- [x] 4.3 `dev/dump/Main.java`: import updated (IDE)
- [x] 4.4 `./gradlew build` — BUILD SUCCESSFUL

## 5. Documentation

- [x] 5.1 `clarity-protobuf/CLAUDE.md` lines 32-34 — rewrote the build-include
      diagram. Today reads:
      ```
      {csgo,dota}/common ← shared/{common,demo}
      {csgo,dota}/s1     ← shared/{common,demo,s1} + game/common
      {csgo,dota}/s2     ← shared/{common,demo,s2} + game/common
      ```
      After: split into the asymmetric shape, e.g.
      ```
      cs/common  ← shared/{common,demo}
      cs/csgo    ← shared/{common,demo,s1} + cs/common
      cs/cs2     ← shared/{common,demo,s2} + cs/common
      dota/common ← shared/{common,demo}
      dota/s1     ← shared/{common,demo,s1} + dota/common
      dota/s2     ← shared/{common,demo,s2} + dota/common
      ```
      Confirm the actual `build.json` once edited and copy the truth in.
- [x] 5.2 `clarity/CHANGELOG.md` — added "CS2 naming cleanup (BREAKING)" entry under Unreleased with the rename table
      (EngineId values, package paths, outer-class names). Past entries stay
      untouched — they reference `CSGO_S2` because that's what the value was
      called at the time, same principle as archived OpenSpec changes.
- [x] 5.3 Sweep verification — re-ran the grep gate across living `*.md` files. Two hits in another active OpenSpec change (`add-entity-bindings-cs2-state`) reference `replays/csgo/s2/...` which is the user's external filesystem layout (replays dir convention), intentionally out of scope for this rename.

## 6. OpenSpec sync (post-archive, not part of this change)

After this change is implemented and merged, archive it as usual; then run
`openspec sync` (or follow the existing sync workflow) to fold the deltas
under `specs/engine-identification/` into the live `openspec/specs/` tree.
The archived change artefact stays frozen with the post-rename names; the
*previous* archived changes that mention `CSGO_S2` etc. are likewise left
alone — they document what was true at the time. The git history + archive
form the audit trail; the live specs form the current contract.

## 7. Memory hygiene

- [x] 7.1 `proto_sync_tool.md` — no class/path-name surface, unchanged
- [x] 7.2 `feedback_proto_structure.md` — refreshed: now describes the dota-engine-split / cs-product-split asymmetry and notes the 2026-05-10 rename rationale
- [x] 7.3 `feedback_include_clarity_analyzer.md` — no specific analyzer class names listed, unchanged

## Verification gate before archive

- [x] V.1 Grep gate clean across all four repos. Two false positives remain (in another active OpenSpec change) referencing `replays/csgo/s2/...` — those are the user's external filesystem layout, intentionally out of scope.
- [x] V.2 Both replays parsed end-to-end through full game-event extraction (see task 2.12). Did not capture a pre-rename byte-identical baseline diff — but the rename is purely textual (no logic changes), and both engines produce coherent game-event streams with the renamed wire classes, which is the meaningful behavioural check.
- [x] V.3 `./gradlew build` (or compile equivalent) clean in clarity-protobuf, clarity, clarity-analyzer, clarity-examples. The pre-existing `:compileJmhJava` failure on `MutationTraceCapture` (`DTClasses.getPointerCount()` missing — unrelated API drift from prior refactors `7fe6936` / `cb32e4f`) is **not** a rename regression.
