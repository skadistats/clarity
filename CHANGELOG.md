# Clarity 2 Changelog

## Unreleased

**Internal restructure**

* package layout reorganized so that every horizontal concern has a
  consistent `(root, s1, s2)` shape. No behavior change. Import-path
  updates are the only migration.
  * new top-level packages: `skadistats.clarity.engine` (holds
    `EngineType`, `AbstractEngineType`, concrete engine types split
    into `engine/s1/`, `engine/s2/`, plus `PacketInstanceReader*`),
    and `skadistats.clarity.state` (entity-state storage — abstract
    `EntityState`, registries, field layout at root; concrete impls
    under `state/s1/` and `state/s2/`).
  * schema types (`SendProp`, `SendTable`, `ReceiveProp`, `Serializer`,
    `Field`, `FieldType`, `Pointer`, `FieldOp`, `S1DTClass`,
    `S2DTClass`, ...) move from `io/s{1,2}/` to `model/s{1,2}/` so the
    abstract `DTClass` and its concrete subclasses finally sit in the
    same top-level package.
  * `io/` shrinks to field-reading only (`FieldReader`, `FieldChanges`,
    `MutationListener`, `S1FieldReader`, `S2FieldReader`, decoder
    factories).
  * `processor/`, `source/`, `event/`, `io/bitstream/`, `io/decoder/`
    are unchanged.
  * `@On*` event-handler parameter types all stay at their current
    paths (`Entity`, `FieldPath`, `GameEvent`, `StringTable`,
    `DTClass`, `CombatLogEntry`). User code that only writes event
    handlers needs no changes.
  * Downstream code that imported `EngineType` from
    `skadistats.clarity.model` or referenced schema/state internals
    from `model.engine.*`, `model.state.*`, or `io.s{1,2}.*` needs
    import-path updates.

**Breaking changes**

* minimum runtime bumped to Java 21. Published jar no longer runs on
  Java 17 JVMs. Exhaustive `switch` over sealed types (required by
  this refactor) forces source=21, which javac couples to target=21.
* `DTClass`, `FieldPath`, and `EntityState` are now sealed sum types.
  * `DTClass permits S1DTClass, S2DTClass`
  * `FieldPath permits S1FieldPath, S2FieldPath`
  * `EntityState permits S1EntityState, S2EntityState`
  * Removed: `DTClass.evaluate(Function, Function)`, `DTClass.s1()`,
    `DTClass.s2()`, `FieldPath.s1()`, `FieldPath.s2()`.
  * *Migration:* replace escape-hatch casts with exhaustive `switch`
    over the sealed hierarchies (e.g.
    `switch (dtClass) { case S1DTClass s1 -> …; case S2DTClass s2 -> …; }`).
* Engine-typed write methods (`write`, `decodeInto`, `applyMutation`,
  `getValueForFieldPath`) moved from the base `EntityState` onto
  `S1EntityState` / `S2EntityState` with typed `S1FieldPath` /
  `S2FieldPath` arguments. Static dispatch helpers
  `EntityState.applyMutation(state, fp, mut)`,
  `EntityState.applyMutations(state, fps, muts, beforeEach)`, and
  `EntityState.getValueForFieldPath(state, fp)` cover callers that
  hold a bare `EntityState`.
* `S2AbstractEntityState` was merged into `S2EntityState` (sealed
  abstract class, directly permitted by `EntityState`). The separate
  interface no longer exists.
* `FieldReader` became a generic interface
  `FieldReader<D extends DTClass, FP extends FieldPath, S extends EntityState>`.
  `S1FieldReader` and `S2FieldReader` bind the engine triple; entry-time
  casts and per-field `FieldPath` casts inside the read loop are gone.
  The mutable `FieldReader.DEBUG_STREAM` static moved to
  `FieldReader.Debug.STREAM` (interface statics are implicitly final).
* `FieldChanges` became generic `FieldChanges<FP extends FieldPath>` with
  a typed `FP[] fieldPaths`. Mutation application delegates to the
  static `EntityState.applyMutations` helpers; sealed dispatch lives in
  one place.

**Performance**

Cumulative wins on `EntityStateParseBench` since 4.0.0 (JDK 21.0.10,
3 warmup + 10 measurement iterations, single-shot, `-prof gc`):

* New default S2 entity state is `S2FlatEntityState` (was
  `NestedArrayEntityState`); new default S1 entity state is
  `S1FlatEntityState` (was `ObjectArrayEntityState`).
* End-to-end parse, default impl: **-24% to -49% wall-clock**, **-56% to
  -89% allocations**.

| Engine | Replay              | wall-clock (4.0 → 4.1) | alloc/op (4.0 → 4.1) |
|--------|---------------------|------------------------|----------------------|
| S2     | cs2 3dmax-falcons   | 1769 → 1226 ms (-31%)  | 13.27 → 3.32 GB (-75%) |
| S2     | deadlock 19206063   | 1387 → 1053 ms (-24%)  | 5.60 → 2.45 GB (-56%) |
| S2     | dota 8168882574     | 1896 → 1338 ms (-29%)  | 9.47 → 3.31 GB (-65%) |
| S1     | csgo luminosity-azio| 1288 → 659 ms (-49%)   | 15.64 → 1.73 GB (-89%) |
| S1     | dota S1 271145478   | 422 → 254 ms (-40%)    | 4.40 → 0.97 GB (-78%) |

The bulk of the wall-clock and allocation wins came from unrelated
dispatch / reader rewrites — `static-decoder-dispatch`,
`fieldop-dispatch-rework`, `accelerate-flat-entity-state` (which also
eliminated `WriteValue` records on the unified reader path for *every*
impl), `strip-entity-state-cow`, and `accelerate-s1-flat-state`. The
flat representation itself accounts for an additional 3-8% wall-clock
and 8-26% allocations vs. the array-based defaults at 4.1.

## April 12, 2026: Version 4.0.0 released

**Breaking changes**

* typed event dispatch via `LambdaMetafactory`, and `Event` subclasses are
  now generated by an annotation processor.
  *Migration:* delete any hand-written `Event` subclass — the processor
  emits it from your `@Provides`/`@Initializer` declarations. Custom
  listener-invocation code (`listeners()`, `handleListenerException()`)
  is no longer needed.
* JPMS module declaration `com.skadistats.clarity`.
  *Migration:* if you consume clarity as a module, add
  `requires com.skadistats.clarity;` to your `module-info.java`.
* adapted to clarity-protobuf 6.0: several generated proto classes moved,
  notably `DOTACombatLog` was extracted out of `DOTAUserMessages`.
  *Migration:* re-import the affected proto classes; the Gradle/Maven
  dependency bumps to `clarity-protobuf:[6.0,7.0)` automatically.
* fix #289: `Source` is now `Closeable` and `ControllableRunner` exposes
  `join()`.
  *Migration:* you can drop any manual cleanup workarounds and use
  try-with-resources on `Source`.

**Fixes**

* fix #350, #351: `ResourceId_t` was falling back to the 32-bit varint
  decoder, under-reading the bit stream for values larger than 35 bits
  and desynchronising the entity decoder for the rest of the packet —
  causing either multi-minute hangs or `ArrayIndexOutOfBoundsException`
  in `ClientFrame.getEntity`. Now registered as a 64-bit varint decoder.
* fix #260: `LiveSource` watcher thread is terminated on close/stop, and
  the aborted flag is reset on reopen.
* fix: CSGO S1 temp entities; `TempEntities` processor scoped to DOTA_S1.
* fix: clean up `readCellCoord` and implement the low-precision path.

**New features**

* feat #322: `Clarity.headerForFile()` helper to read just the file
  header without iterating the demo.
* expose the S2 entity spawn group handle on `Entity`.
* expose `tracked_stat_id` and `modifier_purged_duration` on
  `CombatLogEntry`.

**Performance & hardening**

* replaced `sun.misc.Unsafe` with `VarHandle` for buffer access.
* Huffman field-op decoding via 8-bit lookup table; flattened `Event`
  listener storage; cached `classPattern` matches; partitioned
  `@OnEntityPropertyChanged` listeners by `DTClass`; interned strings
  from `readString()`.
* defensive caps on vector lengths and packed-long field-path limits;
  warn when baseline decoding has remaining bits; baseline-application
  errors now include the class name.

**Build**

* switched to the `nmcp` plugin for Maven Central Portal publishing;
  Gradle wrapper 8.14.4.

## December 16, 2025: Version 3.1.3 released

* fix #349: m_flRuneTime low and high value are null for 7.40

## October 29, 2025: Version 3.1.2 released

* fix #345: new type VectoWS

## September 21, 2024: Version 3.1.1 released

* fix #321: cleanup baselines when entity is deleted
* remove SimulationTimeDecoder, as it depends on ticks/second, and return uint32 instead

## September 14, 2024: Version 3.1.0 released

* add support for Deadlock
* update protobufs
* properly support HeroID_t type

## July 10, 2024: Version 3.0.6 released

* CS2 stopped sending deletions as well
* implemented a fix that should be backwards compatible
  (in replays that still have deletions, they are read)

## May 25, 2024: Version 3.0.5 released

Compatibility with patch 7.36

## April 19, 2024: Version 3.0.4 released

Workaround #311: Deletions are not encoded correctly. Maybe Valve removed them?

## April 06, 2024: Version 3.0.3 released

Fix clarity-examples #60: cannot determine last tick before engine type is known

## Febuary 15, 2024: Version 3.0.2 released

Some additional fixes to support PVS bits.

## Febuary 09, 2024: Version 3.0.1 released

Fix CS2: Arms Race Update

* add support for polymorphic pointers

## November 18, 2023: Version 3.0.0 released

Major release with a lot of new features:

* switched build system to Gradle
* raised minimum required JDK version to 17
* support for parsing CSGO 2 replays
* Protobuf structure improved

## April 21, 2023: Version 2.7.9 released

Compatibility with Dota 7.33 - The New Frontiers Update

## March 08, 2023: Version 2.7.8 released

Fix game events not correctly parsing.

## March 07, 2023: Version 2.7.7 released

Yesterday, Valve released "The Dead Reckoning" update.
This release adds a new "GameTime_t" data type to be able to parse new replays.
The protobufs have also been updated to 4.29.

## January 14, 2023: Version 2.7.6 released

Finally, protobufs have been updated to version 4.28.

You can use the new protobufs with older versions (they will use them automatically), 
however there have been some additions to the combatlog, and if you want to be able 
to access them, you need 2.7.6.

Also contains a performance update for `Util.arrayIdxToString()`, as well as dependency 
updates to bring everything to current versions.

Attention: Some dependencies could not be updated to their newest revisions, because they
rely on a minimum of Java 11. So it's quite possible that I will up the minimum
requirement to Java 11 in the near future as well. If you're still on 8, start working on it!

## October 7, 2022: Version 2.7.5 released

Improve on the incomplete fix from yesterday.

## October 6, 2022: Version 2.7.4 released

Fixes an issue with today's update, which introduced field path length 7.

## August 01, 2022: Version 2.7.3 released

Fixes an issue with console recorded replays on a bad connection, where more than one entity update needs to be deferred.

## July 27, 2022: Version 2.7.2 released

Fixes an issue with CSGO replays where entities with the same handle but different dtClass are created.

## August 4, 2021: Version 2.7.1 released

Fixes an issue where bytecode generated was not executable on an older JVM.

## July 27, 2021: Version 2.7.0 released

Version 2.7.0 brings support for running on JDK > 8.
Tested with 8, 11 and 16.

## June 24, 2021: Version 2.6.2 released

Contains a bugfix for replays with the new Nemestice update, as well as a fix for an NPE when using the LiveSource.
Sorry for the double jump in patch level.

## January 26, 2021: New releases and versioning model update!

Starting today, I will switch to a semantic versioning theme (MAJOR.MINOR.PATCH)

* MAJOR will probably not change in a long time
* MINOR will be increased when there are changes that I believe to be disruptive (and want you to test them first)
* PATCH will be for bugfixes, like we had with the latest 7.28 update.

Today, I made two releases:
* Version 2.5: this is simply the last snapshot as a release
* Version 2.6.0: the first version using the new scheme

There will be no bugfix releases for 2.5, so please migrate your code in a timely manner.

## Changes in 2.6.0

* lots of restructuring / code cleanup regarding the field update parsing code (should not be noticeable)
* package rename `skadistats.clarity.decoder` -> `skadistats.clarity.io` (global search/replace should suffice)
* package rename `skadistats.clarity.io.unpacker` -> `skadistats.clarity.io.decoder` (global search/replace should suffice)
* new event `OnEntityPropertyCountChanged`, which is raised when the amount of properties in an entity changed
* with ControllableRunner (seeking), improved `OnEntityUpdated` to only contain FieldPaths that have been changed
* small performance increase for BitStream
* added proper handling of a special case with Dota 2 console recorded replays, which would throw an exception before
