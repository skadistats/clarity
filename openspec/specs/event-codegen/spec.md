## ADDED Requirements

### Requirement: @GenerateEvent meta-annotation
The system SHALL provide a `@GenerateEvent` annotation with `@Target(ANNOTATION_TYPE)` and `@Retention(SOURCE)` in package `skadistats.clarity.event`. It SHALL have a `strategy` attribute of enum type `GenerateEvent.Strategy` with values `STANDARD` (default) and `BUCKETED`. When placed on an event annotation, it SHALL trigger the annotation processor to generate the Event implementation class using the specified strategy.

#### Scenario: Annotation present triggers generation
- **WHEN** an annotation type is marked with `@GenerateEvent` and contains a nested `Listener` interface
- **THEN** the AP SHALL generate a top-level class named `<AnnotationSimpleName>_Event` in the same package as the annotation

#### Scenario: Annotation absent skips generation
- **WHEN** an annotation type is NOT marked with `@GenerateEvent`
- **THEN** the AP SHALL NOT generate any Event class for that annotation

### Requirement: EventBase interface
The system SHALL provide a `skadistats.clarity.event.EventBase` interface declaring `boolean isListenedTo()`. Each annotation's nested `Event` interface SHALL extend `EventBase`.

#### Scenario: isListenedTo accessible through Event interface
- **WHEN** a processor holds a reference typed as `OnX.Event`
- **THEN** it SHALL be able to call `isListenedTo()` without casting

### Requirement: Event interface declaration in annotations
Each event annotation with `@GenerateEvent` SHALL declare a nested `interface Event extends EventBase` that declares exactly one method: `raise(...)` with parameter types matching the `Listener` SAM method's parameter types.

#### Scenario: Event interface for single-param event
- **WHEN** `OnEntityEntered.Listener` declares `void invoke(Entity e)`
- **THEN** `OnEntityEntered.Event` SHALL declare `void raise(Entity e)`

#### Scenario: Event interface for zero-param event
- **WHEN** `OnInit.Listener` declares `void invoke()`
- **THEN** `OnInit.Event` SHALL declare `void raise()`

#### Scenario: Event interface for primitive-param event
- **WHEN** `OnTickStart.Listener` declares `void invoke(boolean synthetic)`
- **THEN** `OnTickStart.Event` SHALL declare `void raise(boolean synthetic)`

### Requirement: Generated Event class implements dispatch
The generated `<Annotation>_Event` class SHALL implement `<Annotation>.Event` and extend `skadistats.clarity.event.Event<Annotation>`. Its constructor SHALL extract typed `Listener[]` and (if present) `Filter[]` arrays from `EventListener.getListenerSam()` / `getFilterSam()`, casting once at construction time. Its `raise(...)` method SHALL iterate the typed arrays with no per-invocation casts.

#### Scenario: Dispatch without filter
- **WHEN** the annotation has a `Listener` but no `Filter` interface
- **THEN** `raise()` SHALL loop through all listeners, calling `invoke(...)` on each, and call `handleListenerException(i, t)` for any `Throwable`

#### Scenario: Dispatch with filter
- **WHEN** the annotation has both `Listener` and `Filter` interfaces
- **THEN** `raise()` SHALL check `filter.test(...)` before each `invoke(...)`, skipping listeners whose filter returns `false` or whose filter is `null`

#### Scenario: No boxing of primitives
- **WHEN** the Listener SAM has primitive parameters (e.g., `boolean`, `int`)
- **THEN** the generated `raise()` method SHALL use the same primitive types — no autoboxing SHALL occur

### Requirement: BUCKETED strategy generates message-class dispatch
When `@GenerateEvent(strategy = BUCKETED)` is used, the AP SHALL generate an Event class that buckets listeners by the annotation's `value()` attribute. The annotation's `value()` method MUST return `Class<? extends X>` for some base type `X`. Listeners whose `value()` equals the base type (e.g., `GeneratedMessage.class`) SHALL be treated as wildcards.

#### Scenario: Bucketed dispatch to specific class
- **WHEN** `@OnMessage(SomeMessage.class)` is registered and `raise(msg)` is called where `msg.getClass() == SomeMessage.class`
- **THEN** the listener SHALL be invoked

#### Scenario: Wildcard listener receives all messages
- **WHEN** `@OnMessage` (default value `GeneratedMessage.class`) is registered and `raise(msg)` is called for any message class
- **THEN** the wildcard listener SHALL be invoked

#### Scenario: Bucketed Event has typed isListenedTo overload
- **WHEN** the strategy is `BUCKETED` and the annotation's `value()` returns `Class<? extends X>`
- **THEN** the generated Event SHALL implement `boolean isListenedTo(Class<? extends X> clazz)` that returns `true` if a bucket exists for `clazz` or if wildcard listeners are registered

#### Scenario: Bucketed Event interface declares both methods
- **WHEN** the strategy is `BUCKETED`
- **THEN** the annotation's `Event` interface SHALL declare both `raise(...)` and `boolean isListenedTo(Class<? extends X>)`

### Requirement: ExecutionModel.injectEvent field type detection
`ExecutionModel.injectEvent()` SHALL detect the annotation type from fields typed as `OnX.Event` by checking whether the field type's enclosing class is an annotation (`fieldType.getEnclosingClass().isAnnotation()`), instead of checking `Event.class.isAssignableFrom(fieldType)`. This SHALL work for both interface and class Event types.

#### Scenario: Interface Event field injection
- **WHEN** a processor declares `private OnEntityEntered.Event ev` where `OnEntityEntered.Event` is an interface
- **THEN** `injectEvent()` SHALL resolve the annotation type as `OnEntityEntered` and inject the generated `OnEntityEntered_Event` instance

### Requirement: EventContractDiscovery fallback
`EventContractDiscovery` SHALL first look for a nested class named `Event` inside the annotation (existing behavior). If none is found, it SHALL attempt to load `<annotation-package>.<AnnotationSimpleName>_Event` via `Class.forName()`. If found and assignable to `Event`, it SHALL use that class.

#### Scenario: Nested Event class takes priority
- **WHEN** an annotation contains both a nested `class Event` and a generated `_Event` class exists on the classpath
- **THEN** the nested class SHALL be used

#### Scenario: Generated class used as fallback
- **WHEN** an annotation has no nested `Event` class but `OnX_Event` exists on the classpath
- **THEN** `OnX_Event` SHALL be used as the event class

### Requirement: Remove parameterClasses from UsagePointMarker
`@UsagePointMarker` SHALL no longer require a `parameterClasses` attribute for `EVENT_LISTENER` usage points. `InitializerMethod` SHALL derive the parameter count from the Listener SAM method via `EventContractDiscovery`.

#### Scenario: InitializerMethod derives parameter count
- **WHEN** `InitializerMethod.bind()` needs the parameter count for `asSpreader`
- **THEN** it SHALL call `EventContractDiscovery.discover(annotationType).listenerMethod().getParameterCount()` instead of reading `parameterClasses().length`

### Requirement: Custom events opt out of generation
Annotations that do NOT carry `@GenerateEvent` SHALL continue to use their hand-written nested `Event` class. The AP SHALL not process them, and `EventContractDiscovery` SHALL find the nested class via existing logic.

#### Scenario: OnEntityPropertyChanged keeps custom Event
- **WHEN** `OnEntityPropertyChanged` has no `@GenerateEvent` and contains a nested `final class Event`
- **THEN** the hand-written Event class SHALL be used, including its Adapter pattern and caching logic

### Requirement: Processor modularization
The single `EventAnnotationProcessor` class SHALL be split into three separate annotation processors, each registered independently in `META-INF/services/javax.annotation.processing.Processor`. Each processor SHALL handle exactly one concern. All existing behavior SHALL be preserved — this is a structural refactor only.

#### Scenario: Listener validation is separate processor
- **WHEN** the project is compiled and a method bears an annotation marked with `@UsagePointMarker(EVENT_LISTENER)`
- **THEN** `ListenerValidationProcessor` SHALL validate the method signature against the annotation's nested `Listener` SAM, emitting compile errors on mismatch

#### Scenario: Provides indexing is separate processor
- **WHEN** the project is compiled and classes are annotated with `@Provides`
- **THEN** `ProvidesIndexProcessor` SHALL collect their qualified names and write `META-INF/clarity/providers.txt`

#### Scenario: Event generation is separate processor
- **WHEN** the project is compiled and annotations are marked with `@GenerateEvent`
- **THEN** `EventGenerationProcessor` SHALL generate `*_Event` classes using JavaPoet, with identical output to the current monolithic processor

#### Scenario: All processors registered in services file
- **WHEN** `META-INF/services/javax.annotation.processing.Processor` is read
- **THEN** it SHALL list `ListenerValidationProcessor`, `ProvidesIndexProcessor`, and `EventGenerationProcessor` as separate entries
