# Clarity Changelog

## October 5, 2026: Version 5.0.1 released

### Fixes

* Entity filter: when a CREATE for a class rejected by the filter
  reused the index of an entity that passed the filter, the old entity
  was not deleted. The next update at that index was decoded with the
  old entity's class and failed with `no field for class ... at ...`
  (reported in odota/parser#95). Only affects runs with
  `withEntityFilter`.

## October 4, 2026: Version 5.0.0 released

Clarity 5.0 requires Java 21 and clarity-protobuf 7.0. Code that only
writes `@On*` event handlers mostly needs import updates. Code that
queries `Entities`, switches over `EngineId`, references the protobuf
runtime directly or works with the schema and state internals needs the
migrations below.

### Breaking changes

**Java 21 minimum**

The published jar no longer runs on Java 17. The model types are sealed
and dispatched with exhaustive `switch`, which needs source 21, and
javac couples that to target 21.

**Protobuf runtime relocated to `skadistats.clarity.protobuf`**

The protobuf runtime that `clarity-protobuf` vendors has moved out of
`com.google.protobuf`. Clarity no longer squats on that namespace, so it
can finally run alongside a stock `protobuf-java` in the same
application — on the classpath (previously
`IncompatibleClassChangeError`) and on the module path (previously a
split-package `LayerInstantiationException`).

Generated message classes have **not** moved — they remain in
`skadistats.clarity.wire.*`. An `@OnMessage` handler with a generated
message type in its signature needs no edit. Code that references the
runtime directly needs its imports rewritten; the compiler flags every
site:

| Old | New |
|---|---|
| `com.google.protobuf.ByteString` | `skadistats.clarity.protobuf.ByteString` |
| `com.google.protobuf.GeneratedMessage` | `skadistats.clarity.protobuf.GeneratedMessage` |
| `com.google.protobuf.ZeroCopy` | `skadistats.clarity.protobuf.ZeroCopy` |

To bridge clarity bytes into a stock protobuf 3.x message, wrap without
copying via `UnsafeByteOperations.unsafeWrap(ZeroCopy.extract(bs))`.

**Stream-based `Entities` query API**

The four legacy query methods have been replaced by one stream-returning
method and a static predicate factory. Two of the removed methods
(`getByPredicate`, `getByDtName`) silently picked the first match when
several entities were live, a recurring source of subtle bugs. The new
shape makes callers state the cardinality with a stream terminal
operation.

| Old | New |
|---|---|
| `entities.getAllByPredicate(p)` | `entities.stream().filter(p)` (a `Stream<Entity>` instead of an `Iterator<Entity>`) |
| `entities.getByPredicate(p)` | `entities.stream().filter(p).findFirst().orElse(null)` |
| `entities.getAllByDtName(name)` | `entities.stream().filter(Entities.byDtName(name))` |
| `entities.getByDtName(name)` | `entities.stream().filter(Entities.byDtName(name)).findFirst().orElse(null)` |

`Entities.stream()` yields the entities in ascending index order,
skipping empty slots. The helpers `skadistats.clarity.util.SimpleIterator`
and `skadistats.clarity.util.Iterators` have been deleted along with
them.

**CS2 naming**

The Source 2 Counter-Strike engine was called `CSGO_S2`, but CS:GO and
Counter-Strike 2 are distinct products. Names have been corrected
throughout:

* `EngineId.CSGO_S2` → `EngineId.CS2`, `EngineId.CSGO_S1` → `EngineId.CSGO`
  (`DOTA_S1`, `DOTA_S2` and `DEADLOCK` are unchanged)
* `engine.s1.CsGoS1EngineType` → `CsgoEngineType`,
  `engine.s2.CsgoS2EngineType` → `Cs2EngineType`,
  `PacketInstanceReaderCsGoS1` → `PacketInstanceReaderCsgo`
* `model.csgo.PlayerInfoType` → `model.cs.PlayerInfoType`
* clarity-protobuf wire packages: `wire.csgo.common.proto` →
  `wire.cs.common.proto`, `wire.csgo.s1.proto` → `wire.cs.csgo.proto`,
  `wire.csgo.s2.proto` → `wire.cs.cs2.proto`. Outer classes use
  plain-camel acronyms: `CSGOCommonGcMessages` → `CsCommonGcMessages`,
  `CSGOS1NetMessages` → `CsgoNetMessages`, `CSGOS2ClarityMessages` →
  `Cs2ClarityMessages`, etc.

Parsing is unchanged; downstream code updates imports and switch cases.

**Package layout**

Every horizontal concern now has the same `(root, s1, s2)` shape:

* new `skadistats.clarity.engine` for `EngineType`,
  `AbstractEngineType`, the concrete engine types (`engine.s1`,
  `engine.s2`) and the `PacketInstanceReader*` classes
* new `skadistats.clarity.state` for entity-state storage: `EntityState`,
  registries and field layout at the root, implementations in
  `state.s1` and `state.s2`
* schema types (`SendProp`, `SendTable`, `ReceiveProp`, `Serializer`,
  `Field`, `FieldType`, `FieldOp`, `S1DTClass`, `S2DTClass`, ...) move
  from `io.s1` / `io.s2` to `model.s1` / `model.s2`
* `io` keeps field reading only (`FieldReader`, `FieldChanges`,
  `MutationListener`, `S1FieldReader`, `S2FieldReader`, decoder
  factories)

The `@On*` handler parameter types (`Entity`, `FieldPath`, `GameEvent`,
`StringTable`, `DTClass`, `CombatLogEntry`) keep their packages. Code
that imported `EngineType` from `skadistats.clarity.model` or used
`model.engine.*`, `model.state.*` or `io.s1.*` / `io.s2.*` needs
import updates.

**Sealed model types**

* `DTClass permits S1DTClass, S2DTClass`, `FieldPath permits S1FieldPath,
  S2FieldPath`, `EntityState permits S1EntityState, S2EntityState`.
  Removed: `DTClass.evaluate(Function, Function)`, `DTClass.s1()`,
  `DTClass.s2()`, `FieldPath.s1()`, `FieldPath.s2()`.
  *Migration:* replace the escape hatches with an exhaustive `switch`,
  e.g. `switch (dtClass) { case S1DTClass s1 -> …; case S2DTClass s2 -> …; }`.
* `DTClass.getFieldPathForName(String)` and
  `DTClass.getNameForFieldPath(FieldPath)` are now static helpers taking
  the entity state, `DTClass.getFieldPathForName(dtClass, state, name)`,
  because S2 path resolution depends on the entity's current state.
  *Migration:* with an `Entity` at hand, use
  `entity.getFieldPathForName(name)` / `entity.getNameForFieldPath(fp)`.
* The engine-typed state methods (`write`, `decodeInto`, `applyMutation`,
  `getValueForFieldPath`) moved from `EntityState` to `S1EntityState` /
  `S2EntityState` with typed field path arguments. The static helpers
  `EntityState.getValueForFieldPath(state, fp)` and
  `EntityState.applyMutation(state, fp, mutation)` cover callers holding
  a plain `EntityState`. `S2AbstractEntityState` was merged into
  `S2EntityState`.
* `FieldReader` became the generic interface
  `FieldReader<D extends DTClass, FP extends FieldPath, S extends EntityState>`,
  and `FieldChanges` became `FieldChanges<FP extends FieldPath>`. The
  static `FieldReader.DEBUG_STREAM` moved to `FieldReader.Debug.STREAM`.
* `S2ModifiableFieldPath` was replaced by `S2FieldPathBuilder`, which is
  not itself an `S2FieldPath`; `FieldOp.execute` takes the builder.

**Smaller changes**

* The `with*` configuration methods of the file runners throw
  `IllegalStateException` once `runWith` has been called, instead of
  being silently ignored.
* Removed `ResetPhase.FORWARD`, which was never raised.
* Removed `UsagePointMarker.parameterClasses`, which was no longer read
  since the switch to typed event dispatch.
  *Migration:* delete the attribute from custom event annotations; the
  nested `Listener` interface defines the handler parameters.
* The annotation processor `EventAnnotationProcessor` was split into
  `ListenerValidationProcessor`, `ProvidesIndexProcessor` and
  `EventGenerationProcessor`; `DecoderAnnotationProcessor` is new. Builds
  that pick up processors via service discovery need no change; builds
  that name processors explicitly (`-processor`, Maven
  `<annotationProcessors>`) must list the new classes.
* `UsagePoint` is now sealed, and `EventListener` and `InitializerMethod`
  are final.
* The default entity state is now `FLAT` for both engines (previously
  `OBJECT_ARRAY` for Source 1 and `NESTED_ARRAY` for Source 2).
  `withS1EntityState` / `withS2EntityState` select the old ones.
* Strings decoded from the bit stream are no longer interned. Compare
  them with `equals`, not `==`.
* `BitStream32` and `BitStream64` were merged into a single concrete
  `BitStream`; `ClarityPlatform` no longer has the `VM_64BIT` flag or a
  pluggable bit stream constructor.
* `EntityStateFactory` and `ContextData` were removed; `Context` creates
  entity states and field readers.
* S2 pointer fields: `PointerField` → `PolymorphicPointerField`,
  `PointerDecoder` → `PolymorphicPointerDecoder`; single-serializer
  pointers now use the new `FixedPointerField` / `FixedPointerDecoder`.

### New features

**Per-class entity filter**

`withEntityFilter(Predicate<DTClass>)` on the file runners declares
which entity classes a consumer cares about before the parse starts.
Entities of rejected classes are skipped on the wire: no `Entity` is
allocated, no listener fires, and `entities.getByIndex(id)` returns
`null` for them. The default (no filter) behaves exactly as before.
Every decoder now has a `skip` counterpart to `decode`, enforced at
build time by the annotation processor, so skipping cannot desync the
bit stream.

**Primitive property accessors and sparse state snapshots**

`Entity` and `EntityState` gain `getInt`, `getLong` and `getFloat` (by
`FieldPath`, and on `Entity` also by property name) that read primitive
properties without boxing; on the flat entity states they allocate
nothing. Unset or differently-typed fields return `0`; the name-based
variants throw `IllegalArgumentException` for unknown properties, like
`getProperty`. `getObject` is the counterpart for non-primitive types.

```java
int health = hero.getInt("m_iHealth");
```

For handing entity changes to another thread (e.g. a UI),
`EntityState.captureChanged(state, fieldPaths, num)` captures only the
changed fields into an independent `StateDelta`. The receiving side
merges it into its own long-lived state with
`EntityState.applyFrom(state, delta, fp)` or `applyAll(state, delta)`
instead of taking a full `state.copy()` per update. In an
analyzer-shaped benchmark the per-update `copy()` accounted for ~78% of
all allocated bytes; clarity-analyzer now uses the delta path.

**Other additions**

* `withS1EntityState(S1EntityStateType)` and
  `withS2EntityState(S2EntityStateType)` select the entity-state
  storage: `FLAT` (default) or `OBJECT_ARRAY` for Source 1,
  `FLAT` (default), `NESTED_ARRAY` or `TREE_MAP` for Source 2.
* `withS2FieldPath(S2FieldPathType)` selects the S2 field path
  implementation; `LONG` is currently the only one.
* `ControllableRunner.setOnException(Consumer<Throwable>)` reports a
  crash of the runner thread without blocking in `seek()` / `tick()`.
* `Context.createEvent` returns the typed event, so the cast at the
  call site can go.
* `Resources.Manifest` and `Resources.Entry` have public getters for the
  parsed resource paths.

### Performance

`parse` workload of [clarity-bench](https://github.com/spheenik/clarity-bench)
(Ryzen 9 9950X, JDK 21.0.12), 4.0.3 against 5.0, each with its default
entity states (5.0: `FLAT` for both engines; 4.0.3: `OBJECT_ARRAY` /
`NESTED_ARRAY`):

| Engine | Replay               | wall-clock (4.0.3 → 5.0) | alloc/parse (4.0.3 → 5.0) |
|--------|----------------------|--------------------------|---------------------------|
| S2     | dota 8168882574      | 1828 → 1162 ms (-36%)    | 9.47 → 3.30 GB (-65%)     |
| S2     | dota 1560289528      | 470 → 273 ms (-42%)      | 3.63 → 0.66 GB (-82%)     |
| S2     | deadlock 19206063    | 1355 → 959 ms (-29%)     | 5.60 → 2.45 GB (-56%)     |
| S2     | cs2 liquid-betboom   | 1472 → 941 ms (-36%)     | 11.81 → 2.87 GB (-76%)    |
| S1     | dota S1 271145478    | 417 → 238 ms (-43%)      | 4.40 → 0.97 GB (-78%)     |

Most of this comes from the decoder and field-op dispatch rewrites, the
reader rewrite that removed the intermediate `WriteValue` records for
every state implementation, and dropping copy-on-write from the entity
states. The flat states account for 3-8% of the wall-clock and 8-13% of
the allocation gain on Source 2 compared to `NESTED_ARRAY` on 5.0.

The entity filter adds to that: in the OpenDota parser, filtering to
the classes it reads takes another 12-14% off the parse time on Dota
replays from 2023 to 2026.

### Fixes

* `ControllableRunner`: `seek()` and `tick()` no longer block forever
  once the runner thread has terminated (crash or `halt()`), including
  calls made after it ended; they throw `InterruptedException` with the
  runner's exception, if any, as cause. All waiters are woken, waits
  survive spurious wakeups, and `halt()` no longer races the thread's
  own cleanup.
* `Entities`: on demos with PVS visibility bits, an entity entering or
  leaving the client's view now raises `OnEntityEntered` /
  `OnEntityLeft` instead of silently flipping `Entity.isActive()`.
  After a seek, active-state changes of entities that survived the
  reset raise the same events.
* `Clarity.infoForFile` and `Clarity.metadataForFile` close the file
  they open.

### Documentation

* Javadoc for the user-facing API: runners, `Context`, sources, every
  `@On*` event annotation (when it fires, handler signature, attribute
  semantics), the built-in processors, the model and state types, and
  the `event` package for writing custom processors.
* `package-info.java` overviews for the public packages, including a
  minimal processor/runner example.

### Dependencies

* clarity-protobuf 7.0 (relocated protobuf runtime, CS2 package
  restructure).

## October 4, 2026: Version 4.0.3 released

**Fixes**

* fix #355: honor the `fixed8` encoder on all S2 field types. Recent
  Deadlock builds send `uint8`, `int8` and many enum and handle types
  (`MoveType_t`, `RenderMode_t`, `AnimationAlgorithm_t`, ...) as 8 raw
  bits; reading them as varints desynced the bit stream and failed with
  `Entity not found for update`. Thanks to @Rupas1k for the report.

## September 24, 2026: Version 4.0.2 released

**Fixes**

* fix #354: decode 32-bit `qangle_precise` QAngles as three raw floats.
  CS2 declares `CBodyComponentBaseModelEntity.m_angRotation` (used by
  `CFuncConveyor`) this way; decoding it with the precise encoding
  desynced the bit stream and failed with `decoder desync: vector length
  ... exceeds the structural maximum` on maps with conveyors (e.g.
  `rush_001`). Thanks to @LukasW1337.

## May 10, 2026: Version 4.0.1 released

**Fixes**

* fix #353: register S2 decoders for `CUtlBinaryBlock` (varint length +
  payload bytes) and `CGlobalSymbol` (null-terminated string). Both
  types previously fell back to the 32-bit varint decoder, under-reading
  the bit stream and causing `Entity not found for update at index N`
  on CS2 demos that exercise these field types (observed on builds
  10772+).
* fix: `CombatLogEntry` now exposes `heal_from_regen`.

**Dependency bumps**

* clarity-protobuf: `[6.0,7.0)` → `[6.1,7.0)`. Brings in
  `CCSUsrMsg_WeaponMagDrop` (CSGO_S2/389) and
  `CMsgDOTACombatLogEntry.heal_from_regen`.

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
