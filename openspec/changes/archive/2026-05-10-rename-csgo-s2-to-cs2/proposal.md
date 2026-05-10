## Why

`EngineId.CSGO_S2` is a misnomer. CS:GO never ran on Source 2 — Valve renamed
the game to **Counter-Strike 2** when they cut the engine over in September
2023. CSGO and CS2 are distinct products that share a wire-format heritage,
not the same product on two engines (unlike Dota 2, which legitimately spans
S1 and S2 under one product name). The current naming pretends otherwise and
confuses every reader who knows the actual product timeline.

The misnomer has metastasised across three repos:

- `EngineId.CSGO_S2` (the enum value)
- `engine.s2.CsgoS2EngineType` (the runtime engine class)
- `model.csgo.PlayerInfoType` (a class shared by *both* CSGO and CS2 — the
  package implies CSGO-only)
- `proto/csgo/s2/` and `wire.csgo.s2.proto.*` (the proto tree puts CS2 as a
  sub-folder of "csgo")
- clarity-analyzer's `CSGOS2BindingGenerator`, `CSGOS2AndDeadlockPositionBinder`
- clarity-examples' `dev/csgo2test/` directory and several proto imports

We're already cutting a 5.x major with breaking changes; this is the right
moment to fix it everywhere at once rather than introducing a parallel `CS2`
name and leaving the legacy aliases around.

## What Changes

### EngineId enum — the canonical fix

```
DOTA_S1, DOTA_S2, CSGO_S1, CSGO_S2, DEADLOCK   (today)
DOTA_S1, DOTA_S2, CSGO,    CS2,     DEADLOCK   (after)
```

Asymmetric because reality is asymmetric: Dota 2 spans two engines under one
product name (suffix needed to disambiguate); CSGO and CS2 are different
products (no suffix needed); Deadlock is one product on one engine.

- **BREAKING** `EngineId.CSGO_S2` → `EngineId.CS2`
- **BREAKING** `EngineId.CSGO_S1` → `EngineId.CSGO`

`DOTA_S1`, `DOTA_S2`, `DEADLOCK` are unchanged.

### Class casing — locked to plain camel

The codebase has both `CsGoS1` (mixed-case G) and `CsgoS2` styles in flight.
Lock to plain camel for ≥3-letter acronyms (`Csgo`, `Cs2`), which matches
modern Java style guides and the existing `CsgoS2EngineType`.

- **BREAKING** `engine.s1.CsGoS1EngineType` → `engine.s1.CsgoEngineType`
- **BREAKING** `engine.s2.CsgoS2EngineType` → `engine.s2.Cs2EngineType`
- **BREAKING** `engine.s1.PacketInstanceReaderCsGoS1` → `PacketInstanceReaderCsgo`

### `model.csgo` package — promoted to family-neutral `model.cs`

`PlayerInfoType` has both `createS1` and `createS2` factories — it is a
shared CSGO+CS2 type. Its package implies CSGO-only.

- **BREAKING** package `skadistats.clarity.model.csgo` → `skadistats.clarity.model.cs`
- Affects `PlayerInfoType` and the `module-info.java` exports.

### Proto tree — restructured to reflect product reality

Today's `proto/csgo/{common,s1,s2}/` parents CS2 under "csgo". The fix mirrors
the EngineId rename: `cs/` is the family directory, with **product** children
(not engine children). The `common/` folder retains its role — `build.json`
already includes it for both s1 and s2 builds, and the `cstrike15_*-common.proto`
files are genuinely shared between CSGO and CS2.

```
clarity-protobuf/src/main/proto/
  csgo/                         cs/
    common/        ───►            common/      (shared CSGO+CS2)
    s1/            ───►            csgo/        (CSGO product, S1 only)
    s2/            ───►            cs2/         (CS2 product, S2 only)
  dota/                         dota/           (unchanged — same product, two engines)
    common/        ───►            common/
    s1/            ───►            s1/
    s2/            ───►            s2/
```

Java packages follow:

```
wire.csgo.common.proto   →  wire.cs.common.proto
wire.csgo.s1.proto       →  wire.cs.csgo.proto
wire.csgo.s2.proto       →  wire.cs.cs2.proto
```

Outer class names follow plain-camel casing:

```
CSGOCommonGcMessages     →  CsCommonGcMessages
CSGOCommonUserMessages   →  CsCommonUserMessages
CSGOS1NetMessages        →  CsgoNetMessages
CSGOS1ClarityMessages    →  CsgoClarityMessages
CSGOS1MessageId          →  CsgoMessageId
CSGOS1UserMessages       →  CsgoUserMessages
CSGOS2ClarityMessages    →  Cs2ClarityMessages
CSGOS2GameEvents         →  Cs2GameEvents
CSGOS2MessageId          →  Cs2MessageId
```

Top-level dir is `cs/` (not `cstrike/`) — short, matches the EngineId casing
(`CSGO`, `CS2`), and parallels the existing s2-side abbreviation in `wire.s2`
package conventions where they exist.

### What does NOT change

- The `gameId == "csgo"` literal in `EngineMagic.S2.determineEngineType` —
  that comes from Valve's demo header (`CDemoFileHeader.game` /
  `game_directory`); even CS2 demos still set it, because Valve's install
  directory is still called `csgo`. This is data, not naming.
- The `engine/s1/` and `engine/s2/` package split inside clarity itself —
  that's an engine boundary (real, technical), orthogonal to product naming.
- Dota's `{s1,s2,common}` tree — same product, two engines, the suffixes are
  meaningful.
- `EngineMagic.S2` — it's correctly named "Source 2 magic," dispatches three
  games off it.

### Proto-sync tooling

The proto-sync tool's `mapping.json` and `RUNBOOK.md` reference `csgo/s1`,
`csgo/s2`, `csgo/common` paths and explain the "common vs. S2 directories"
rule in s1/s2 terms. Both need updating: the new mental model is "common is
shared between products that happen to share heritage (CSGO+CS2)," and CSGO
itself is the s1-era product so the `# Don't touch Source 1` rule maps
cleanly to "don't touch `cs/csgo/`".

## Capabilities

### Modified Capabilities
- `engine-identification`: the public enum that names supported engines/products
  changes its CS-family member naming from `{CSGO_S1, CSGO_S2}` to `{CSGO, CS2}`,
  reflecting that the S2-era product is a distinct game (Counter-Strike 2), not
  CS:GO on a different engine.

## Impact

### clarity (parser)
- `EngineId.java` — enum values renamed; all internal switch sites adjust.
- `EngineMagic.java` — references to `EngineId.CSGO_S2` / `CSGO_S1`, imports
  of renamed engine classes.
- `engine/s1/CsGoS1EngineType.java` → `CsgoEngineType.java` (rename + casing fix).
- `engine/s1/PacketInstanceReaderCsGoS1.java` → `PacketInstanceReaderCsgo.java`.
- `engine/s2/CsgoS2EngineType.java` → `Cs2EngineType.java`.
- `model/csgo/PlayerInfoType.java` → `model/cs/PlayerInfoType.java`; package
  declaration + `module-info.java` `exports` clause updated.
- `processor/stringtables/PlayerInfo.java`, `OnPlayerInfo.java` — import path
  update (one line each).
- All `CSGO_S2` / `CSGO_S1` switch sites in `processor/sendtables/`,
  `processor/stringtables/`, `processor/resources/`, `processor/tempentities/`,
  `processor/runner/Context.java`, etc. — mechanical rename.

### clarity-protobuf
- Move `proto/csgo/{common,s1,s2}/` to `proto/cs/{common,csgo,cs2}/`.
- Update every `option java_package` and `option java_outer_classname` in the
  moved `.proto` files.
- `build.json` — update `path` and `include` entries.
- `tools/proto-sync/mapping.json` — update path entries.
- `tools/proto-sync/RUNBOOK.md` — rewrite the "Common vs. S2 directories" and
  "Don't touch Source 1" sections to reflect the cs/{common,csgo,cs2} layout.
- Rebuild generated wire classes; old `wire.csgo.*` classes deleted.

### clarity-analyzer
- `binding/CSGOS1BindingGenerator.java` → `CsgoBindingGenerator.java`.
- `binding/CSGOS2BindingGenerator.java` → `Cs2BindingGenerator.java`.
- `position/CSGOS1PositionBinder.java` → `CsgoPositionBinder.java`.
- `position/CSGOS2AndDeadlockPositionBinder.java` → `Cs2AndDeadlockPositionBinder.java`.
- `icon/csgo/PlayerIcon.java` → `icon/PlayerIcon.java` (drop the `csgo`
  subpackage). Verified: imported by `CSGOS1BindingGenerator`,
  `CSGOS2BindingGenerator`, **and** `DeadlockBindingGenerator` — it's a
  generic player icon, not CS-family-specific. Promoting it to
  `analyzer.map.icon` (no qualifier) reflects the actual usage.
- `MapControl.java` — switch sites on `EngineId` adjust to renamed values.

### clarity-examples
- `dev/csgo2test/` → `dev/cstest/` (directory rename + package declaration
  update in `Main.java`). Import update: `model.csgo.PlayerInfoType` →
  `model.cs.PlayerInfoType`. The internal `Replay CSGO_S1` / `CSGO_S2`
  constant names are file-private labels and can stay or be renamed at
  author taste.
- `dev/test/Main.java`, `dev/dump/Main.java` —
  `import …wire.csgo.s1.proto.CSGOS1NetMessages` →
  `import …wire.cs.csgo.proto.CsgoNetMessages` (one line each).
- No other example uses CSGO/CS2 EngineId values or proto types.

### Downstream API (publishing)

This rides the same 5.x major as the seal-engine-types breakage. Documented
in the 5.0 migration notes:

- `EngineId.CSGO_S1` → `EngineId.CSGO`
- `EngineId.CSGO_S2` → `EngineId.CS2`
- Import path: `skadistats.clarity.model.csgo.PlayerInfoType` →
  `skadistats.clarity.model.cs.PlayerInfoType`
- Import path: `skadistats.clarity.wire.csgo.{common,s1,s2}.proto.*` →
  `skadistats.clarity.wire.cs.{common,csgo,cs2}.proto.*`
- Outer-class renames listed above.

No behavioural changes. Pure rename + package move; on-wire format,
parsing logic, and observable replay output are byte-identical to pre-rename.

### Memory follow-ups

The following memory entries describe state that this change invalidates and
will need refreshing post-merge:

- `proto_sync_tool.md` — runbook section names
- `feedback_proto_structure.md` — the "S2-only content in s2/ folders" rule
  no longer applies to the cs family (it's product-folder, not engine-folder)
- `feedback_include_clarity_analyzer.md` — analyzer class list
