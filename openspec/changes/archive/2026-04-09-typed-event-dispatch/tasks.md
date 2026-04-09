## 1. Reconnaissance

- [x] 1.1 Enumerate every annotation marked with `@UsagePointMarker(EVENT_LISTENER)` in `clarity` and `clarity-examples`; record arity and `parameterClasses` for each
- [x] 1.2 Enumerate every `@InsertEvent` field in the codebase; record the annotation type it injects (~27 sites in clarity, plus any in clarity-examples)
- [x] 1.3 Enumerate every `evXxx.raise(...)` call site; record the call shape per annotation
- [x] 1.4 Enumerate every `@Initializer` method that calls `setInvocationPredicate(...)` or `setParameterClasses(...)`; record what state it touches
- [x] 1.5 Enumerate every caller of `Context.createEvent(...)` (especially in `clarity-examples`, where direct callers exist in `lifestate` and `cooldowns`)
- [x] 1.6 Verify that no live code uses `@InsertEvent(override = true, parameterTypes = ...)` (grep already confirmed for `clarity` core; double-check `clarity-examples` and any other in-tree consumer)
- [ ] 1.7 Verify with a small test that `MethodHandles.privateLookupIn(processorClass, lookup())` resolves package-private and private methods on a sample processor class (deferred to step 3 implementation)

## 2. `processor` source set setup inside `clarity`

- [x] 2.1 In `clarity/build.gradle.kts`, declare a new source set `processor` with sources at `src/processor/java` and resources at `src/processor/resources`
- [x] 2.2 Verify Gradle generates a `compileProcessorJava` task that runs before `compileJava`
- [x] 2.3 Add a stub `EventAnnotationProcessor` class at `src/processor/java/skadistats/clarity/processor/EventAnnotationProcessor.java`, extending `javax.annotation.processing.AbstractProcessor`, declaring `@SupportedAnnotationTypes("*")` and `@SupportedSourceVersion(SourceVersion.RELEASE_17)`. Empty `process(...)` body for now.
- [x] 2.4 Register the stub via `src/processor/resources/META-INF/services/javax.annotation.processing.Processor` (single line containing the FQ class name)
- [x] 2.5 Wire the processor as an annotation processor for the main source set: `annotationProcessor(sourceSets["processor"].output)` in the `dependencies` block
- [x] 2.6 Make the `jar` task pull classes and resources from the `processor` source set so they ship inside `clarity.jar`: `tasks.named<Jar>("jar") { from(sourceSets["processor"].output) }`
- [x] 2.7 Build `clarity` end-to-end and verify activation: temporarily add `processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "clarity processor running")` to the stub `process()`, run `./gradlew :clarity:compileJava`, see the message in build output, then remove the temporary line
- [x] 2.8 Verify that `clarity.jar` contains both the main classes and the processor classes (`unzip -l build/libs/clarity-*.jar | grep -E "(EventAnnotation|services/javax)"` or similar)
- [x] 2.9 Verify that `clarity-examples` automatically picks up the processor when it depends on `clarity` (no extra build configuration needed beyond the existing `implementation` line); confirm by triggering the same `Diagnostic.Kind.NOTE` once temporarily during `clarity-examples` compilation — FINDING: automatic pickup does NOT work with composite builds; explicit `annotationProcessor` line needed in clarity-examples (added in task 10.1)

## 3. LMF binding infrastructure (clarity event package)

- [x] 3.1 Add a helper `EventContractDiscovery` (or similar) that, given an annotation `Class<A>`, locates `A.Listener`, `A.Filter` (optional), and `A.Event` via `getDeclaredClasses()` and validates they exist with the right shape
- [x] 3.2 Add a helper `LmfBinder` that, given a SAM `Class<S>` and a target `MethodHandle`, produces an instance of `S` via `LambdaMetafactory.metafactory(...)`
- [x] 3.3 Add a `dynamicParameters` boolean to `@UsagePointMarker` (default `false`) for the `OnMessage`-style carve-out
- [x] 3.4 Add `Object listenerSam` and `Object filterSam` fields to `EventListener` (typed at dispatch time inside the typed `Event` subclass)
- [x] 3.5 Add `setFilter(Object filter)` on `AbstractInvocationPoint`; throws `IllegalStateException` if the annotation has no nested `Filter` SAM
- [x] 3.6 Modify `AbstractInvocationPoint.bind(Context)` to: resolve the user method via `MethodHandles.privateLookupIn(...)`, bind the receiver and optional `Context`, then run `LmfBinder` to produce the typed `Listener` SAM, store in `listenerSam`
- [x] 3.7 Keep the legacy `methodHandle` + `invoke(Object[])` path alive as a fallback during the migration; mark with TODO for deletion in step 11
- [x] 3.8 Make `Event<A>.raise(Object...)` `final` and have it throw `UnsupportedOperationException("typed Event subclass must override raise")` to catch any caller that hasn't been migrated yet — ADJUSTED: kept raise working during migration with TODO; typed subclasses override with their own typed raise
- [x] 3.9 Add a protected accessor `EventListener<A>[] listeners()` so nested typed `Event` subclasses can iterate the base array

## 4. Worked example: migrate `OnEntityCreated`

- [x] 4.1 In `OnEntityCreated.java`, add `interface Listener { void invoke(Entity e); }`
- [x] 4.2 In `OnEntityCreated.java`, add `interface Filter { boolean test(Entity e); }`
- [x] 4.3 In `OnEntityCreated.java`, add `class Event extends skadistats.clarity.event.Event<OnEntityCreated>` with typed `raise(Entity e)` over the parallel `Listener[]`/`Filter[]` arrays
- [x] 4.4 In `Entities.java`, change `@InsertEvent private Event<OnEntityCreated> evCreated;` to `@InsertEvent private OnEntityCreated.Event evCreated;`
- [x] 4.5 In `Entities.java`, the `evCreated.raise(entity)` call site SHALL compile unchanged (resolving to the typed overload)
- [x] 4.6 In `Entities.java`, change the `@Initializer(OnEntityCreated.class)` body from `setInvocationPredicate(getInvocationPredicate(...))` to `setFilter((OnEntityCreated.Filter) e -> ...)` using the existing `classPatternMatchers` cache
- [x] 4.7 Verify `clarity` compiles and a representative `clarity-examples` example (`combatlog`) still runs — verified with lifestate and combatlog examples

## 5. Migrate the remaining entity annotations

- [x] 5.1 `OnEntityUpdated`: add nested `Listener`, `Filter`, `Event` with `(Entity, FieldPath[], int)`; migrate `@InsertEvent`, raise site, initializer
- [x] 5.2 `OnEntityDeleted`: same pattern with `(Entity)`
- [x] 5.3 `OnEntityEntered`: same pattern with `(Entity)`
- [x] 5.4 `OnEntityLeft`: same pattern with `(Entity)`
- [x] 5.5 `OnEntityPropertyCountChanged`: same pattern; migrate `@InsertEvent` and initializer. Also migrated `OnEntityUpdatesCompleted` (empty params, no filter).
- [ ] 5.6 Run `eventdispatchbench` and record numbers — first checkpoint
- [x] 5.7 Verify `combatlog`, `lifestate`, `matchend`, `info` examples still run — combatlog, lifestate, info pass; matchend NPE is pre-existing (replay-specific)

## 6. `OnEntityPropertyChanged` and `PropertyChange.java` rework

- [x] 6.1 In `OnEntityPropertyChanged.java`, add `interface Listener { void invoke(Entity e, FieldPath fp); }`
- [x] 6.2 In `OnEntityPropertyChanged.java`, add `interface Filter { boolean test(Entity e, FieldPath fp); }` (probably unused, but for symmetry)
- [x] 6.3 In `OnEntityPropertyChanged.java`, add `class Event extends skadistats.clarity.event.Event<OnEntityPropertyChanged>` containing the partition logic moved from `PropertyChange.java`: per-DTClass `Adapter[]` partition, per-(DTClass, FieldPath) cache for `propertyPattern` matches, the `raise(Entity, FieldPath)` dispatch loop
- [x] 6.4 Move the existing `ListenerAdapter` inner class from `PropertyChange.java` into `OnEntityPropertyChanged.Event` as `Adapter`
- [x] 6.5 Move the caches into `OnEntityPropertyChanged.Event` as private fields
- [x] 6.6 Shrink `PropertyChange.java` to a thin processor: `@InsertEvent OnEntityPropertyChanged.Event evPropertyChanged;` plus `@OnEntityCreated` and `@OnEntityUpdated` that loop `evPropertyChanged.raise(e, fp)`
- [ ] 6.7 Run `propertychangebench` to confirm the relocated dispatch performs identically (no regression)
- [x] 6.8 Verify `propertychange`-using examples still produce identical output

## 7. Migrate `OnMessage` and `OnPostEmbeddedMessage`

- [x] 7.1 In `OnMessage.java`, add `interface Listener { void invoke(GeneratedMessage msg); }` (most-general parameter type)
- [x] 7.2 In `OnMessage.java`, add `class Event extends skadistats.clarity.event.Event<OnMessage>` with internal `Map<Class<? extends GeneratedMessage>, Entry[]> byClass` partition + `Entry[] wildcardEntries`
- [x] 7.3 The partition is populated at construction time by walking the listener array, reading each listener's `annotation.value()`, and bucketing accordingly
- [x] 7.4 `Event.raise(GeneratedMessage msg)` looks up `msg.getClass()` in `byClass` and invokes the matching listeners, then wildcard listeners
- [x] 7.5 Mark `@UsagePointMarker(..., dynamicParameters = true)` on `OnMessage`
- [x] 7.6 LMF adaptation: fixed `instantiatedMethodType` in `LmfBinder` to use the implementation's actual parameter types (not the SAM's), so LMF generates the necessary downcast for specific message types
- [x] 7.7 `OnPostEmbeddedMessage`: same pattern with `(GeneratedMessage, BitStream)`, dynamicParameters=true, internal partition by `annotation.value()`
- [x] 7.8 In `InputSourceProcessor.java`, changed to `@InsertEvent OnMessage.Event evOnMessage` and `@InsertEvent OnPostEmbeddedMessage.Event evOnPostEmbeddedMessage`
- [x] 7.9 Deleted `evOnMessages`/`evOnPostEmbeddedMessages` maps and `evOnMessage()`/`evOnPostEmbeddedMessage()` accessor methods
- [x] 7.10 Dropped `setParameterClasses(...)` from both initializers; kept `unpackUserMessages` side effect
- [x] 7.11 Changed raise sites to `evOnMessage.raise(msg)` directly; added `isListenedTo(Class)` for per-class check
- [x] 7.12 Verified combatlog and lifestate examples run correctly
- [x] 7.extra: Added `raise(Object...)` override on OnMessage.Event and OnPostEmbeddedMessage.Event to route legacy callers (AbstractEngineType.emitHeader, etc.) through the typed partition

## 8. Migrate the remaining annotations

- [x] 8.1 `OnGameEvent`: nested `Listener(GameEvent)`, `Filter(GameEvent)`, `Event`; migrate `@InsertEvent` in `GameEvents.java`
- [x] 8.2 `OnGameEventDescriptor`: same pattern; migrate `@InsertEvent` in `GameEvents.java`
- [x] 8.3 `OnCombatLogEntry`: nested types; migrate `CombatLog.java`
- [x] 8.4 `OnStringTableEntry`: nested types with Filter; migrate `BaseStringTableEmitter.java`
- [x] 8.5 `OnStringTableCreated`: nested types; migrate
- [x] 8.6 `OnStringTableClear`: nested types (no-arg); migrate
- [x] 8.7 `OnPlayerInfo`: nested types; migrate `PlayerInfo.java`
- [x] 8.8 `OnTickStart`, `OnTickEnd`: nested types `(boolean)`; migrate `AbstractFileRunner.java`
- [x] 8.9 `OnReset`: nested types; migrate `InputSourceProcessor.java`
- [x] 8.10 `OnInit`, `OnInputSource`, `OnFullPacket`, `OnDTClass`, `OnDTClassesComplete`, `OnTempEntity`, `OnModifierTableEntry`, `OnEntityUpdatesCompleted`: all migrated
- [x] 8.11 `OnMessageContainer`: nested types with Filter; migrate `InputSourceProcessor.java`
- [x] 8.12 Full clarity codebase compiles
- [x] 8.13 Verified lifestate, info, allchat with full-size replay

## 9. Annotation processor implementation

- [x] 9.1 Implemented Task A (listener-signature validation): scans all methods for EVENT_LISTENER annotations, finds nested Listener SAM, compares parameter lists (tolerates leading Context, handles dynamicParameters best-effort)
- [ ] 9.2 Test the validator by introducing deliberate mismatches in throwaway test files
- [x] 9.3 Implemented Task B (@Provides indexing): collects @Provides classes across rounds, writes sorted names to META-INF/clarity/providers.txt on processingOver()
- [x] 9.4 Verified providers.txt generated correctly (18 providers in clarity, 3 in clarity-examples)
- [x] 9.5 Updated UsagePoints.java to read providers.txt via ClassLoader.getResources()
- [x] 9.6 Dropped @IndexAnnotated from Provides.java
- [x] 9.7 Removed org.atteo.classindex from clarity's build.gradle.kts
- [x] 9.8 Ported the defensive null-check for IDE inconsistencies (Class.forName catch → debug log + skip)
- [x] 9.9 Verified: no org.atteo.classindex imports remain, clarity and clarity-examples compile and run

## 10. Migrate `clarity-examples` custom event annotations

- [x] 10.1 Added `annotationProcessor("com.skadistats:clarity:3.1.4-SNAPSHOT")` to clarity-examples (replaces classindex)
- [x] 10.2 lifestate annotations: OnEntitySpawned, OnEntityDying, OnEntityDied — nested Listener + Event
- [x] 10.3 SpawnsAndDeaths.java: typed Event fields, createEvent casts, dropped parameterTypes
- [x] 10.4 cooldowns annotations: OnAbilityCooldownStart/Reset/End — nested Listener + Event
- [x] 10.5 Cooldowns.java: same migration
- [x] 10.6 OnEffectDispatch: nested Listener + Event
- [x] 10.7 EffectDispatches.java: typed Event field, createEvent cast
- [x] 10.8 Verified lifestateRun with full-size replay (1.28s, correct output)

## 11. Remove the legacy untyped path

- [x] 11.1 Removed `raise(Object...)` from `Event<A>` and the `@Override` bridges from OnMessage.Event/OnPostEmbeddedMessage.Event
- [x] 11.2 Removed `methodHandle`, `asSpreader`, legacy `invoke(Object[])` from `AbstractInvocationPoint`; kept MethodHandle-based invoke only on `InitializerMethod`
- [x] 11.3 Removed `setInvocationPredicate`; converted all callers to `setFilter` (Entities, GameEvents, BaseStringTableEmitter, InputSourceProcessor)
- [x] 11.4 Removed `setParameterClasses`, `parameterClasses`, `isInvokedForParameterClasses`, `getArity`, `isInvokedForArguments` from AbstractInvocationPoint and InvocationPoint
- [x] 11.5 Removed `@InsertEvent.override()` and `parameterTypes()`; reduced to pure marker
- [x] 11.6 Simplified `ExecutionModel`: `createEvent(Class<A>)` single-arg, returns all listeners, no parameterTypes filtering
- [x] 11.7 Simplified `Context.createEvent(Class<A>)` to single overload; fixed callers (AbstractEngineType, CsGoS1EngineType, AbstractFileRunner) to cast to typed Event
- [x] 11.8 Removed `skadistats.clarity.util.Predicate`; switched `Entities.getAllByPredicate` to `java.util.function.Predicate`
- [x] 11.9 Verified: clarity and clarity-examples compile, lifestate and allchat run correctly with full-size replay

## 12. Validation, benchmarking, documentation

- [ ] 12.1 Run `eventdispatchbench` against all three replays (dota, cs2, deadlock) and record before/after numbers per configuration
- [ ] 12.2 Run `propertychangebench` against all three replays to confirm `OnEntityPropertyChanged.Event`'s relocated dispatch matches the prior numbers within noise
- [x] 12.3 Verified lifestate, allchat, info examples with full-size replay
- [x] 12.4 Wrote `MIGRATION.md` with upgrade steps for external users
- [x] 12.5 EventContractDiscovery, LmfBinder, and EventAnnotationProcessor all have Javadoc
- [ ] 12.6 Update commit message(s) with the bench numbers

## 13. Final cleanup

- [x] 13.1 `./gradlew build` passes for both clarity and clarity-examples
- [x] 13.2 No orphaned imports of `skadistats.clarity.util.Predicate` (only in clarity-diff, separate project)
- [x] 13.3 No orphaned imports of `org.atteo.classindex` anywhere
- [x] 13.4 `asSpreader` only in InitializerMethod (intentional) and LmfBinder Javadoc
- [x] 13.5 `openspec validate typed-event-dispatch --strict` passes
