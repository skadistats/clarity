# engine-identification

## Purpose

Defines how clarity identifies and names the Valve replay sources it
supports. The capability codifies that `EngineId` enum values name
**Valve products** rather than engine versions (with an `_S1` / `_S2`
suffix used only when one product spans both Source engines), and that
the surrounding code organisation — engine-type class names, shared
model packages, and the protobuf tree under `wire.cs.*` — follows the
same product-oriented convention with plain-camel acronym casing. It
also pins the wire-format origin literals (e.g. the legacy `"csgo"`
`gameId` string Valve still emits for CS2 demo headers) as
non-normalised inputs to engine dispatch.

## Requirements

### Requirement: EngineId values name Valve products, not engine versions, when products differ across engines

The `EngineId` enum SHALL name each supported replay source after the
**Valve product** that produced it, suffixed with the Source engine version
(`_S1` / `_S2`) **only when** the same product spans more than one engine.
When a product is unique to one engine, the suffix SHALL be omitted.

The canonical value set is:

- `DOTA_S1` — Dota 2 on Source 1
- `DOTA_S2` — Dota 2 on Source 2 (same product as `DOTA_S1`, different engine)
- `CSGO`   — Counter-Strike: Global Offensive (Source 1; the product was
            retired when the S2 successor shipped under a different name)
- `CS2`    — Counter-Strike 2 (Source 2; a distinct product from CSGO,
            despite sharing wire-format heritage)
- `DEADLOCK` — Deadlock (Source 2 only)

The pre-rename names `CSGO_S1` and `CSGO_S2` SHALL NOT be retained as
aliases. Consumers migrate at the same major-version boundary.

#### Scenario: CS2 replays surface as EngineId.CS2

- **WHEN** a Source 2 demo whose embedded `game` / `game_directory` header
  resolves to the CS2 product is loaded through the runner
- **THEN** `runner.getEngineType().getId()` returns `EngineId.CS2`
- **AND** no symbol named `EngineId.CSGO_S2` exists in the public API

#### Scenario: CSGO replays surface as EngineId.CSGO

- **WHEN** a Source 1 CS:GO demo (`HL2DEMO\0` magic) is loaded
- **THEN** `runner.getEngineType().getId()` returns `EngineId.CSGO`
- **AND** no symbol named `EngineId.CSGO_S1` exists in the public API

#### Scenario: Dota retains the engine suffix because the product spans both engines

- **WHEN** the `EngineId` enum is inspected
- **THEN** `DOTA_S1` and `DOTA_S2` are present as distinct values
- **AND** neither is collapsed to a bare `DOTA`, because the same product
  legitimately runs on both Source engines and the suffix carries
  information that callers need

#### Scenario: Deadlock has no engine suffix because it is single-engine

- **WHEN** the `EngineId` enum is inspected
- **THEN** the value is `DEADLOCK`, not `DEADLOCK_S2`
- **AND** consumers needing the engine inspect `EngineType` directly

### Requirement: Wire-format origin literals are not normalised

Clarity SHALL preserve the literal `gameId` string `"csgo"` produced by
Valve's demo header (`CDemoFileHeader.game` / `game_directory`) verbatim in
the engine-magic dispatch, including for CS2 replays. Valve's install
directory remains `csgo` even after the rename to Counter-Strike 2; the
EngineId rename SHALL NOT cause clarity to re-write or alias this header
value.

#### Scenario: CS2 demo header still uses the legacy "csgo" gameId literal

- **WHEN** `EngineMagic.S2.determineEngineType` reads a CS2 demo header
- **THEN** it observes `gameId == "csgo"` (the unchanged Valve install-dir
  string), and SHALL dispatch that literal to construct an engine of
  `EngineId.CS2`
- **AND** the literal `"csgo"` in the dispatch switch is preserved verbatim

### Requirement: Engine-type classes follow plain-camel acronym casing

Concrete `EngineType` implementations and their adjacent helpers SHALL use
plain-camel casing (first letter capital, remaining letters lowercase) for
3+ letter acronyms. The previous mixed-case style (`CsGoS1`) is removed.

#### Scenario: Engine-type class names use plain camel

- **WHEN** the `engine.s1` and `engine.s2` packages are listed
- **THEN** the CS-family classes are `CsgoEngineType`, `Cs2EngineType`,
  `PacketInstanceReaderCsgo`
- **AND** no class named `CsGoS1EngineType`, `CsgoS2EngineType`, or
  `PacketInstanceReaderCsGoS1` exists

### Requirement: Shared CS-family model types live under model.cs

Public model types SHALL live under the family-neutral package
`skadistats.clarity.model.cs` when they are used by **both** CSGO and CS2.
The canonical case is `PlayerInfoType` with its `createS1` / `createS2`
factories. The product-specific `model.csgo` package SHALL be removed.

#### Scenario: PlayerInfoType is reachable under model.cs

- **WHEN** consumer code imports the shared player-info type
- **THEN** the import path is `skadistats.clarity.model.cs.PlayerInfoType`
- **AND** no class named `skadistats.clarity.model.csgo.PlayerInfoType` exists
- **AND** the `module-info.java` `exports` list contains
  `skadistats.clarity.model.cs` and not `skadistats.clarity.model.csgo`

### Requirement: Wire-package layout reflects products, not engines, for the CS family

The protobuf tree under `wire.cs.*` SHALL be organised by **product**, not
by engine version, for the Counter-Strike family. Specifically:

- `wire.cs.common.proto.*` — protos shared between CSGO and CS2 (the
  upstream `cstrike15_*-common.proto` files Valve still ships in both)
- `wire.cs.csgo.proto.*`   — protos specific to the CSGO product (S1)
- `wire.cs.cs2.proto.*`    — protos specific to the CS2 product (S2)

The Dota family retains its `{common, s1, s2}` shape because Dota 2 spans
two engines under one product name; that product/engine asymmetry is
intentional and matches the `EngineId` shape.

#### Scenario: CS protos are addressable by product

- **WHEN** consumer code or generated wire classes are imported
- **THEN** valid prefixes are `skadistats.clarity.wire.cs.common.proto`,
  `skadistats.clarity.wire.cs.csgo.proto`, and `skadistats.clarity.wire.cs.cs2.proto`
- **AND** no class is reachable under `skadistats.clarity.wire.csgo.{s1,s2,common}.proto`

#### Scenario: Outer-class names use plain-camel casing

- **WHEN** the generated wire classes in `wire.cs.*` are inspected
- **THEN** their outer-class names follow plain-camel acronym casing
  (e.g. `CsCommonGcMessages`, `CsgoNetMessages`, `Cs2ClarityMessages`)
- **AND** no generated class named `CSGOCommonGcMessages`, `CSGOS1NetMessages`,
  or `CSGOS2ClarityMessages` exists
