## Why

Clarity's event system funnels every listener invocation through `Object[]`: a varargs raise call allocates a new `Object[]` per dispatch, the listener's `MethodHandle` uses `asSpreader` to unpack it back into positional args, filters are `Predicate<Object[]>` with `(Foo) args[0]` casts, and the link between an annotation declaration and the listener method signature is verified only at runtime when the LMF bind happens.

On top of that the framework carries plumbing — `setParameterClasses`, `isInvokedForParameterClasses`, `@InsertEvent.override()` / `parameterTypes()`, the `Event<A>` family with one instance per `(annotation, parameterClasses)` tuple — that exists almost entirely to support a single use case (`OnMessage`'s per-message-class dispatch). Two of these (`override` / `parameterTypes`) are not used anywhere in the codebase at all.

`PropertyChange` is the one place where dispatch is *actually* fast — it bypasses `Event.raise` and uses a hand-rolled per-DTClass partition. That model — the typed event class owns its dispatch logic, including any partition by discriminator — is what every event should look like.

`classindex` (third-party annotation processor used solely to discover `@Provides`-annotated classes) is a stand-alone dependency for ~15 lines of indexing.

This change rewrites the event dispatch contract so that every annotation owns its own typed dispatch, deletes the dead `Object[]`-era plumbing, validates listener signatures at compile time, and folds the `@Provides` discovery into a single in-house annotation processor — removing the `classindex` dependency.

## What Changes

- **BREAKING**: Each annotation marked with `@UsagePointMarker(EVENT_LISTENER)` declares its own dispatch contract through nested types: `interface Listener` (typed dispatch SAM), optional `interface Filter` (typed filter SAM, same arity as `Listener`), and `class Event` (typed dispatch class extending `skadistats.clarity.event.Event`). The base `Event<A>` becomes a thin abstract base; the typed `raise(...)` lives in the nested subclass.
- **BREAKING**: `@InsertEvent` field declarations change from `Event<OnXxx>` to `OnXxx.Event`. `evXxx.raise(...)` call sites stay syntactically identical but now resolve to a typed overload.
- **BREAKING**: Listener binding switches from `MethodHandle.asSpreader(Object[].class, arity)` + `invokeExact(args)` to `LambdaMetafactory`-generated typed SAM instances. Filter binding uses the same mechanism.
- **BREAKING**: `setInvocationPredicate(Predicate<Object[]>)` becomes `setFilter(Object filter)` accepting the annotation's nested `Filter` SAM. The `skadistats.clarity.util.Predicate` interface is removed.
- **BREAKING**: `Context.createEvent(...)` no longer takes a `parameterTypes` varargs. The single overload is `createEvent(Class<A> annotationType)`. The factory locates and instantiates the nested `OnXxx.Event` subclass via reflection.
- **BREAKING (dead code removal)**: `setParameterClasses(...)`, `isInvokedForParameterClasses(...)`, the `parameterClasses` field on `AbstractInvocationPoint`, `InvocationPoint.getArity()`, and the `@InsertEvent.override()` / `parameterTypes()` annotation members are deleted. `ExecutionModel.computeListenersForEvent` collapses to "all listeners for this annotation".
- `OnMessage` and `OnPostEmbeddedMessage` (the two consumers of `setParameterClasses`) move their per-message-class dispatch into their own nested `Event` subclasses. `OnMessage.Event` internally partitions listeners by `annotation.value()` and dispatches in O(1) via a `Map<Class<? extends GeneratedMessage>, Listener[]>` lookup on `msg.getClass()`. The external `evOnMessages` map in `InputSourceProcessor` goes away.
- `PropertyChange`'s hand-rolled dispatch (the `ListenerAdapter`, `adaptersByClass`, per-DTClass property-name match cache) moves into `OnEntityPropertyChanged.Event`. `PropertyChange.java` shrinks to a thin processor that listens to `@OnEntityUpdated` and forwards each `(Entity, FieldPath)` to `evPropertyChanged.raise(...)`. This is the canonical example of "the typed Event class owns its dispatch".
- A new in-house annotation processor (lives in a dedicated `processor` Gradle source set inside `clarity`, compiled before the main source set, packaged into the same `clarity.jar`) handles two compile-time tasks:
  - Validates that every method bearing an `EVENT_LISTENER`-marked annotation has a parameter list compatible with that annotation's nested `Listener` SAM. Mismatches become **compile errors** with the offending method underlined in the IDE.
  - Indexes `@Provides`-annotated processor classes into a `META-INF/clarity/providers.txt` resource file, replacing the runtime `ClassIndex.getAnnotated(...)` call in `UsagePoints`.
- The `org.atteo.classindex:classindex` dependency is removed.
- `@Initializer` is retained, with a narrower job: install typed filters via `setFilter(...)` (no longer `setInvocationPredicate`) and run any per-listener processor-state side effects (`requestedTables.add(...)`, `unpackUserMessages |= ...`, etc.).
- `clarity-examples` custom event annotations (`lifestate`'s `OnEntitySpawned` / `OnEntityDying` / `OnEntityDied`, `cooldowns`'s `OnAbilityCooldownStart` / `OnAbilityCooldownReset` / `OnAbilityCooldownEnd`, `s2effectdispatch`'s `OnEffectDispatch`) are migrated to the new convention as part of this change. Their consumer processors (`SpawnsAndDeaths`, `Cooldowns`, `EffectDispatches`) are updated correspondingly.
- A short `MIGRATION.md` is added to the change to document the upgrade steps for external users with their own custom event annotations.

## Capabilities

### New Capabilities
- `event-dispatch`: Defines how clarity dispatches event listeners and filters from raise sites to user-annotated methods. Covers the typed annotation contract (nested `Listener` / `Filter` / `Event` triple), the LMF-based binding pipeline, the dispatch hot path including per-discriminator partition where applicable, the rules custom event annotations must follow, the compile-time validation pass that enforces listener-signature consistency, and the in-house annotation-processor mechanism that also indexes `@Provides` classes.

### Modified Capabilities
<!-- None — there are no existing specs in openspec/specs/ to modify. -->

## Impact

### Code surfaces affected (clarity)

- **Event package**: `Event.java`, `EventListener.java`, `AbstractInvocationPoint.java`, `InvocationPoint.java`, `UsagePointMarker.java`, `Predicate.java` (deletion), `InsertEvent.java` (annotation members deletion).
- **Event annotations** (~15 files): `OnEntityCreated`, `OnEntityUpdated`, `OnEntityDeleted`, `OnEntityEntered`, `OnEntityLeft`, `OnEntityPropertyChanged`, `OnEntityPropertyCountChanged`, `OnGameEvent`, `OnGameEventDescriptor`, `OnCombatLogEntry`, `OnMessage`, `OnPostEmbeddedMessage`, `OnMessageContainer`, `OnStringTableEntry`, `OnStringTableCreated`, `OnStringTableClear`, `OnPlayerInfo`, `OnTickStart`, `OnTickEnd`, `OnReset`, `OnInit`, `OnInputSource`, `OnFullPacket`, `OnSyncTick`, `OnDTClass`, `OnDTClassesComplete`, `OnTempEntity`, `OnModifierTableEntry`, `OnEffectDispatch`, etc.
- **Processors with `@InsertEvent`** (~13 files, 27 sites): `Entities`, `PropertyChange`, `BaseStringTableEmitter`, `PlayerInfo`, `Modifiers`, `CombatLog`, `GameEvents`, `S1DTClassEmitter`, `S2DTClassEmitter`, `InputSourceProcessor`, `AbstractRunner`, `AbstractFileRunner`, `TempEntities`.
- **Processors with `@Initializer` calling `setInvocationPredicate`** (4 files, 9 sites): `Entities` (6×), `BaseStringTableEmitter` (1×), `GameEvents` (2×), `InputSourceProcessor` (1× — the `OnMessageContainer` predicate; `OnMessage` and `OnPostEmbeddedMessage` initializers also drop their `setParameterClasses` calls).
- **Runner**: `ExecutionModel.injectEvent`, `ExecutionModel.computeListenersForEvent`, `Context.createEvent` — collapsed/simplified.
- **Discovery**: `UsagePoints.java` (replaces `ClassIndex.getAnnotated(...)` with reading the new `META-INF/clarity/providers.txt` resource), `Provides.java` (drop `@IndexAnnotated`).
- **PropertyChange**: `PropertyChange.java` shrinks dramatically; its dispatch logic moves into `OnEntityPropertyChanged.Event`.

### Code surfaces affected (clarity-examples)

- `lifestate`: 3 custom annotations + `SpawnsAndDeaths` processor migrated. ~30 LOC.
- `cooldowns`: 3 custom annotations + `Cooldowns` processor migrated. ~35 LOC.
- `s2effectdispatch`: 1 custom annotation + `EffectDispatches` processor migrated. ~12 LOC.
- All `*Run` Gradle tasks must continue to execute correctly post-migration; this is verified as part of the change.

### Build / dependencies

- **No new published artifact.** The annotation processor lives in a new `processor` source set inside the existing `clarity` Gradle project (`src/processor/java/...`). Build order: `compileProcessorJava` runs first (compiles the processor in isolation), then `compileJava` runs the processor on the main source set as `annotationProcessor(sourceSets["processor"].output)`. The `jar` task includes both source sets so the processor classes ship inside `clarity.jar`. Downstream consumers add a single `annotationProcessor("skadistats:clarity:<version>")` line to their build to enable the processor on their own code — the same artifact serves both roles via separate Gradle configurations.
- This is the same packaging strategy that `org.atteo.classindex` already uses today (one jar containing both runtime API and the annotation processor, discovered via `META-INF/services/javax.annotation.processing.Processor`). We replicate it in-house and remove the external dependency.
- **Removed dependency**: `org.atteo.classindex:classindex`.
- **No new external dependencies**: `LambdaMetafactory` and `MethodHandles` are JDK built-ins (`java.lang.invoke`), available since Java 8; clarity targets Java 17.
- **Service file**: the new processor is registered via `META-INF/services/javax.annotation.processing.Processor` inside `clarity.jar` (generated from a resource in the `processor` source set).

### Performance (expected)

The bench baseline is `eventdispatchbench`. Expected gains relative to the post-`Event.java`-flattening commit (`7fd2e5b`):

- **Object[] allocation eliminated** from every typed `raise(...)`: ~1 ns per dispatch saved.
- **`asSpreader` unpack eliminated** in favor of LMF-generated direct virtual call: ~2–3 ns per listener visit saved.
- **`Integer` boxing eliminated** for `OnEntityUpdated`'s `int n` parameter: ~0 ns measurable (cached for small values), but cleaner profiling traces.
- **OnMessage internal partition** preserves the existing per-class dispatch performance — no regression.
- **PropertyChange-into-OnEntityPropertyChanged.Event** preserves existing performance — the same code, just relocated.

Realistic expected wins on `eventdispatchbench`:
- `updated-1`: ~15–25 ms saved (cs2/deadlock).
- `updated-8`: ~50–100 ms saved (cs2/deadlock).
- `updated-8-filt`: similar to `updated-8` (the per-class predicate cache from commit `ec91afd` already paid this off; the SAM-typed filter just removes a cast).

These are modest in absolute terms. The structural value — type safety, dead-code removal, dependency reduction, single dispatch convention — is the bigger story.

### Risk

- LMF must work for all parameter shapes currently in use: primitives (`int`, `boolean`), arrays (`FieldPath[]`), parameterized types (`Class<? extends GeneratedMessage>`), and the `Context`-prepended convention. None of these are unusual for LMF, but each needs verification on a representative annotation during the migration.
- Annotation processors run during `javac`; if the new processor crashes or emits noisy output, it can break IDEs and CI for everyone. Defensive programming and robust error handling are essential.
- `clarity-examples` is migrated as part of this change; any external user with custom event annotations gets a hard break and needs the migration steps documented in `MIGRATION.md`.
