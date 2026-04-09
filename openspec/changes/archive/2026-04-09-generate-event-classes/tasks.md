## 1. Foundation

- [x] 1.1 Create `EventBase` interface in `skadistats.clarity.event` with `boolean isListenedTo()`
- [x] 1.2 Create `@GenerateEvent` annotation in `skadistats.clarity.event` with `@Target(ANNOTATION_TYPE)`, `@Retention(SOURCE)`, and `Strategy` enum (`STANDARD`, `BUCKETED`)
- [x] 1.3 Make `Event<A>` implement `EventBase` (so generated classes inherit `isListenedTo()` from the base class)

## 2. Annotation Processor

- [x] 2.1 Create `EventProcessor extends AbstractProcessor` in `skadistats.clarity.event` — discover `@GenerateEvent`-annotated annotations, inspect nested `Listener` and `Filter` SAM interfaces, read `strategy` attribute
- [x] 2.2 Implement STANDARD strategy code generation: typed Listener/Filter arrays, constructor with SAM extraction, `raise()` dispatch loop with filter check and exception handling
- [x] 2.3 Implement BUCKETED strategy code generation: Entry inner class, bucket-by-value() constructor (wildcard detection via default value), `raise()` dispatching to bucket + wildcards, `isListenedTo(Class<?>)` method
- [x] 2.4 Register AP via `META-INF/services/javax.annotation.processing.Processor`
- [x] 2.5 Add JavaPoet dependency (`compileOnly`) and AP to build config (`build.gradle.kts`)

## 3. Framework Adjustments

- [x] 3.1 Extend `EventContractDiscovery.doDiscover()`: if no nested `Event` class found, attempt `Class.forName("<package>.<Name>_Event")` as fallback
- [x] 3.2 Update `ExecutionModel.injectEvent()`: change field type detection from `Event.class.isAssignableFrom(fieldType)` to `fieldType.getEnclosingClass().isAnnotation()`

## 4. Remove parameterClasses

- [x] 4.1 Refactor `InitializerMethod.bind()` to derive parameter count from `EventContractDiscovery.discover().listenerMethod().getParameterCount()`
- [x] 4.2 Remove `parameterClasses` attribute from `@UsagePointMarker` (or make it optional/unused for EVENT_LISTENER)
- [x] 4.3 Remove `parameterClasses` from all event annotation `@UsagePointMarker` declarations

## 5. Migrate Standard Events (STANDARD strategy)

- [x] 5.1 Migrate zero-param events: `OnInit`, `OnTickStart`, `OnTickEnd`, `OnDTClassesComplete`, `OnEntityUpdatesCompleted`, `OnStringTableClear` — replace nested `final class Event` with `interface Event extends EventBase`, add `@GenerateEvent`
- [x] 5.2 Migrate single-param events without filter: `OnTempEntity`, `OnDTClass`, `OnFullPacket`, `OnCombatLogEntry`, `OnModifierTableEntry`, `OnPlayerInfo`, `OnStringTableCreated`
- [x] 5.3 Migrate single-param events with filter: `OnEntityCreated`, `OnEntityDeleted`, `OnEntityEntered`, `OnEntityLeft`, `OnEntityPropertyCountChanged`, `OnGameEvent`, `OnGameEventDescriptor`
- [x] 5.4 Migrate multi-param events: `OnStringTableEntry`, `OnEntityUpdated`, `OnMessageContainer`, `OnReset`, `OnInputSource`

## 6. Migrate Bucketed Events (BUCKETED strategy)

- [x] 6.1 Migrate `OnMessage`: replace nested `final class Event` with `interface Event extends EventBase` (declaring `raise(GeneratedMessage)` and `isListenedTo(Class<?>)`), add `@GenerateEvent(strategy = BUCKETED)`
- [x] 6.2 Migrate `OnPostEmbeddedMessage`: same pattern, `raise(GeneratedMessage, BitStream)` and `isListenedTo(Class<?>)`

## 7. Verify

- [x] 7.1 Verify `OnEntityPropertyChanged` is untouched — keeps hand-written nested `Event` class
- [x] 7.2 Build project, verify all generated `_Event` classes compile and are on classpath
- [x] 7.3 Run existing examples/tests against a replay to confirm identical behavior
