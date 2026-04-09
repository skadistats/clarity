## ADDED Requirements

### Requirement: Annotation-nested event contract

Every annotation marked with `@UsagePointMarker(value = EVENT_LISTENER, ...)` SHALL declare its event contract through nested types in the annotation declaration itself.

The contract consists of:
- A required nested `interface Listener` whose single abstract method takes the parameter list declared by the annotation's `@UsagePointMarker(parameterClasses = ...)` and returns `void`. The method MAY be named `invoke`.
- An optional nested `interface Filter` whose single abstract method takes the same parameter list as `Listener` and returns `boolean`. Annotations whose listeners are never filtered MAY omit this.
- A required nested `class Event` extending `skadistats.clarity.event.Event<ThisAnnotation>`. It SHALL expose a public `raise(...)` method whose signature matches the `Listener.invoke(...)` parameter list.

The nested `Listener.invoke` and the typed `Event.raise` SHALL use primitive types for primitive parameters declared in `parameterClasses` (no autoboxing at the SAM boundary).

#### Scenario: Annotation declares full contract

- **WHEN** an annotation `@OnEntityUpdated` is declared with `@UsagePointMarker(parameterClasses = { Entity.class, FieldPath[].class, int.class })`
- **THEN** `OnEntityUpdated.Listener` SHALL exist with method `void invoke(Entity e, FieldPath[] fps, int n)`
- **AND** `OnEntityUpdated.Filter` SHALL exist with method `boolean test(Entity e, FieldPath[] fps, int n)`
- **AND** `OnEntityUpdated.Event` SHALL exist as a class extending `skadistats.clarity.event.Event<OnEntityUpdated>` with method `void raise(Entity e, FieldPath[] fps, int n)`

#### Scenario: Annotation without filter omits Filter SAM

- **WHEN** an annotation `@OnTickStart` declares only `Listener` and `Event` (no nested `Filter`)
- **THEN** the framework SHALL bind listeners normally
- **AND** any attempt to call `setFilter(...)` on a listener for this annotation SHALL throw `IllegalStateException`

#### Scenario: Primitive parameters are not boxed

- **WHEN** a `Listener` SAM declares a primitive `int` parameter
- **THEN** the typed `Event.raise(...)` method SHALL accept a primitive `int` argument
- **AND** the dispatch path SHALL pass that argument as a primitive all the way to the user's annotated method without `Integer.valueOf` or `Integer.intValue` calls

### Requirement: Convention-based discovery

The framework SHALL discover the nested `Listener`, `Filter`, and `Event` types of an event annotation by inspecting `annotationClass.getDeclaredClasses()` and matching by exact simple name (`Listener`, `Filter`, `Event` respectively). The framework SHALL NOT require any additional parameter on `@UsagePointMarker` to point at these types.

If an annotation marked as `EVENT_LISTENER` is missing the required `Listener` or `Event` nested types, the framework SHALL fail at processor registration time with an error naming the annotation and the missing nested type.

#### Scenario: Missing Listener type

- **WHEN** an annotation marked `EVENT_LISTENER` does not declare a nested `Listener` interface
- **THEN** processor registration SHALL fail with an error message naming the annotation and the missing nested type

#### Scenario: Missing Event class

- **WHEN** an annotation marked `EVENT_LISTENER` does not declare a nested `Event` class extending `skadistats.clarity.event.Event`
- **THEN** processor registration SHALL fail with an error message naming the annotation and the missing nested type

#### Scenario: Missing Filter is allowed

- **WHEN** an annotation marked `EVENT_LISTENER` declares `Listener` and `Event` but no `Filter`
- **THEN** processor registration SHALL succeed
- **AND** any `@Initializer` for that annotation that attempts to register a filter SHALL fail with `IllegalStateException`

### Requirement: Listener binding via LambdaMetafactory

When a processor's annotated event-handler method is bound to a runtime context, the framework SHALL produce a `Listener` SAM instance via `java.lang.invoke.LambdaMetafactory` whose target is the user's method (with the receiver and any leading `Context` parameter pre-bound).

The framework SHALL NOT use `MethodHandle.asSpreader(Object[].class, ...)` for event-listener dispatch.

The framework SHALL NOT allocate an `Object[]` to invoke a typed listener at dispatch time.

#### Scenario: User method with primitive parameter

- **WHEN** a user defines `@OnEntityUpdated public void onUpdate(Entity e, FieldPath[] fps, int n) { ... }`
- **AND** the framework binds this listener
- **THEN** the framework SHALL produce an `OnEntityUpdated.Listener` SAM instance
- **AND** invoking `listener.invoke(entity, fps, count)` SHALL call the user's method with `n` as a primitive `int`, with no `Integer` allocation in the dispatch path

#### Scenario: User method with leading Context parameter

- **WHEN** a user defines `@OnEntityCreated public void onCreate(Context ctx, Entity e) { ... }`
- **AND** the framework binds this listener
- **THEN** the framework SHALL produce an `OnEntityCreated.Listener` SAM instance whose `invoke(Entity)` calls the user's method with the runtime `Context` already bound

#### Scenario: User method with package-private visibility

- **WHEN** a user's annotated method is `package-private`
- **THEN** the framework SHALL still successfully bind it (using `MethodHandles.privateLookupIn` or equivalent)

### Requirement: Filter binding via LambdaMetafactory

When a processor's `@Initializer` registers a filter for an event listener, the filter SHALL be provided as an instance of the annotation's nested `Filter` SAM (or `null` for "no filter").

The framework SHALL bind the filter via the same `LambdaMetafactory` mechanism used for listeners, except that the SAM target is the annotation's `Filter` interface instead of `Listener`.

The framework SHALL NOT use `Predicate<Object[]>` for event-listener filtering.

#### Scenario: Filter is provided

- **WHEN** an `@Initializer` registers a non-null filter for a listener
- **THEN** the typed `Event.raise(...)` SHALL invoke `filter.test(...)` with the same arguments it would pass to `listener.invoke(...)`
- **AND** the listener SHALL be invoked only if `filter.test(...)` returns `true`

#### Scenario: Filter is not provided

- **WHEN** an `@Initializer` does not call `setFilter(...)` for a listener (or passes `null`)
- **THEN** the typed `Event.raise(...)` SHALL invoke `listener.invoke(...)` directly without consulting any filter

#### Scenario: Filter only inspects first argument

- **WHEN** a filter implementation uses only the first argument (e.g., `(entity, fps, n) -> entity.getDtClass() == target`)
- **THEN** the filter SHALL function correctly
- **AND** the unused arguments SHALL not cause additional allocation or boxing in the dispatch path

### Requirement: Typed dispatch in `Event.raise`

The nested `Event` class for each annotation SHALL be declared `final`. Subclassing the typed dispatch class is not a supported extension point.

The nested `Event` class for each annotation SHALL implement `raise(...)` as a single loop that iterates parallel `Listener[]` and `Filter[]` arrays, calling each filter (if non-null) followed by each listener with the typed arguments passed to `raise`.

The dispatch loop SHALL preserve the order established by `EventListener.order` (the `@Order` annotation on the user method, defaulting to 0).

The dispatch loop SHALL route any exception thrown by a listener to the runner's exception handler via `Event.handleListenerException(listenerIndex, throwable)`. The dispatch loop SHALL NOT allocate an `Object[]` on the error path.

#### Scenario: Multiple listeners ordered by @Order

- **WHEN** three listeners `L1`, `L2`, `L3` are registered for an event with `@Order` values `5`, `1`, `3` respectively
- **THEN** dispatch order on each `raise` SHALL be `L2`, `L3`, `L1`

#### Scenario: Listener throws exception

- **WHEN** a listener throws a `RuntimeException` during dispatch
- **THEN** the framework SHALL route the exception through `Event.handleListenerException(listenerIndex, throwable)`
- **AND** dispatch SHALL continue with the remaining listeners

### Requirement: Per-discriminator partition for `OnMessage` and `OnPostEmbeddedMessage`

The nested `Event` class for `OnMessage` SHALL maintain an internal partition keyed by `annotation.value()` (the message class), so that dispatch only visits listeners that subscribed to the actual concrete message class being raised.

`OnPostEmbeddedMessage.Event` SHALL behave analogously.

The partition SHALL be populated at listener-registration time, not at dispatch time. Dispatch SHALL be O(1) in the number of listeners (a single map lookup followed by iteration over the matching bucket).

The framework SHALL NOT maintain a separate `Event` instance per message class. The `evOnMessages` map (and equivalent for `OnPostEmbeddedMessage`) in `InputSourceProcessor` SHALL be removed.

#### Scenario: Specific-class listener with matching message

- **WHEN** a user defines `@OnMessage(CSVCMsg_GameEventList.class) public void on(CSVCMsg_GameEventList msg)`
- **AND** a `CSVCMsg_GameEventList` is raised
- **THEN** the listener SHALL be invoked

#### Scenario: Specific-class listener with non-matching message

- **WHEN** a user defines `@OnMessage(CSVCMsg_GameEventList.class) public void on(CSVCMsg_GameEventList msg)`
- **AND** a `CSVCMsg_PacketEntities` is raised
- **THEN** the listener SHALL NOT be invoked

#### Scenario: Wildcard listener receives all messages

- **WHEN** a user defines `@OnMessage public void on(GeneratedMessage msg)` (no `value` set)
- **AND** any message is raised
- **THEN** the listener SHALL be invoked for every raised message
- **AND** the listener SHALL be stored separately from class-specific listeners (not duplicated into every class-specific bucket)

### Requirement: PropertyChange dispatch lives in `OnEntityPropertyChanged.Event`

The dispatch logic for `OnEntityPropertyChanged` (per-DTClass partition by `classPattern`, per-(DTClass, FieldPath) caching of `propertyPattern` matches, dispatch loop) SHALL live inside the nested `OnEntityPropertyChanged.Event` class.

`PropertyChange.java` SHALL remain only as a thin processor that listens to `@OnEntityUpdated` and forwards each `(Entity, FieldPath)` pair to `evPropertyChanged.raise(...)`. It SHALL NOT contain any partition logic, listener-adapter classes, or property-pattern matching code.

#### Scenario: Property change matches classPattern

- **WHEN** a listener is registered with `@OnEntityPropertyChanged(classPattern = "CDOTA_Unit_Hero_.*", propertyPattern = "m_iHealth")`
- **AND** a hero entity's `m_iHealth` field is updated
- **THEN** `OnEntityPropertyChanged.Event.raise(entity, fp)` SHALL invoke the listener
- **AND** the dispatch SHALL not consult `PropertyChange.java` for any partitioning logic

#### Scenario: Property change does not match classPattern

- **WHEN** a listener is registered with `@OnEntityPropertyChanged(classPattern = "CDOTA_Unit_Hero_.*", ...)`
- **AND** a non-hero entity's field is updated
- **THEN** the listener SHALL NOT be invoked
- **AND** the listener SHALL not appear in the per-DTClass bucket for that entity's class

### Requirement: `Context.createEvent` simplification

`Context.createEvent` SHALL have a single overload: `<A extends Annotation> Event<A> createEvent(Class<A> eventType)`.

The factory SHALL locate the annotation's nested `Event` class via reflection and instantiate it. The returned object SHALL be statically typed as `Event<A>` but SHALL be an instance of the concrete nested `Event` subclass.

The framework SHALL NOT expose a `createEvent(Class<A>, Class<?>... parameterTypes)` overload.

#### Scenario: Caller asks for the event of an annotation type

- **WHEN** a processor calls `ctx.createEvent(OnEntityUpdated.class)`
- **THEN** the returned event SHALL be an instance of `OnEntityUpdated.Event`
- **AND** the returned event SHALL be assignable to `skadistats.clarity.event.Event<OnEntityUpdated>`

#### Scenario: Legacy parameterTypes overload is gone

- **WHEN** code calls `ctx.createEvent(OnMessage.class, CSVCMsg_GameEventList.class)`
- **THEN** compilation SHALL fail because the overload no longer exists

### Requirement: Removal of legacy untyped dispatch path

The `skadistats.clarity.event.Event` base class SHALL NOT expose a `raise(Object... args)` method.

`AbstractInvocationPoint` SHALL NOT expose `setInvocationPredicate(Predicate<Object[]>)`. It SHALL expose `setFilter(Object filter)` that accepts a typed SAM matching the annotation's nested `Filter` interface.

`AbstractInvocationPoint.setParameterClasses(...)` SHALL NOT exist.

`AbstractInvocationPoint.parameterClasses` field SHALL NOT exist.

`AbstractInvocationPoint.isInvokedForParameterClasses(...)` and `InvocationPoint.getArity()` SHALL NOT exist.

`@InsertEvent.override()` and `@InsertEvent.parameterTypes()` SHALL NOT exist. `@InsertEvent` SHALL be a pure marker annotation with no members.

`skadistats.clarity.util.Predicate` SHALL be removed if it has no other usages.

#### Scenario: Legacy raise method is gone

- **WHEN** code calls `event.raise(new Object[]{...})` on the base `Event<A>` class
- **THEN** compilation SHALL fail because `raise(Object[])` no longer exists

#### Scenario: Legacy setInvocationPredicate is gone

- **WHEN** code calls `listener.setInvocationPredicate(args -> ...)`
- **THEN** compilation SHALL fail because `setInvocationPredicate` no longer exists

#### Scenario: Legacy setParameterClasses is gone

- **WHEN** code calls `listener.setParameterClasses(SomeClass.class)`
- **THEN** compilation SHALL fail because `setParameterClasses` no longer exists

#### Scenario: Legacy InsertEvent members are gone

- **WHEN** code declares `@InsertEvent(override = true, parameterTypes = { Foo.class }) Event<...> field`
- **THEN** compilation SHALL fail because `override` and `parameterTypes` are no longer members of `@InsertEvent`

### Requirement: `@Initializer` continues to exist with narrowed role

`@Initializer` methods SHALL continue to be supported. They run after all listeners have been registered for an event but before any dispatch.

`@Initializer` methods MAY:
- Call `setFilter(...)` on the supplied `EventListener` to install a typed filter
- Read the supplied `EventListener`'s annotation values to drive processor-state side effects (e.g., recording which string tables to track, which message classes to unpack)

`@Initializer` methods SHALL NOT call `setInvocationPredicate(...)` (the method does not exist) or `setParameterClasses(...)` (the method does not exist).

#### Scenario: Initializer installs a typed filter

- **WHEN** `@Initializer(OnEntityUpdated.class) void init(EventListener<OnEntityUpdated> el)` reads the annotation's `classPattern`
- **AND** computes a typed filter based on the pattern
- **AND** calls `el.setFilter((OnEntityUpdated.Filter) (e, fps, n) -> ...)`
- **THEN** the filter SHALL be installed and consulted on every subsequent `raise` for this listener

#### Scenario: Initializer performs side effects

- **WHEN** `@Initializer(OnStringTableEntry.class) void init(EventListener<OnStringTableEntry> el)` reads the annotation's `value` (the requested table name)
- **AND** records the name into the processor's `requestedTables` set
- **THEN** the side effect SHALL persist for the lifetime of the processor

### Requirement: Compile-time validation of listener method signatures

A new in-house annotation processor (running at `javac` time) SHALL validate that every method bearing an `EVENT_LISTENER`-marked annotation has a parameter list compatible with that annotation's nested `Listener` SAM.

The validator SHALL:
- Walk the round environment's annotated elements.
- For each method element, walk its annotations and check if any annotation type itself bears `@UsagePointMarker(EVENT_LISTENER)`.
- For in-scope methods, locate the annotation type's nested `Listener` interface via `javax.lang.model.element.TypeElement.getEnclosedElements()`.
- Compare the SAM's parameter list to the annotated method's parameter list.
- Tolerate a leading `Context` parameter on the user method.
- For annotations marked `@UsagePointMarker(..., dynamicParameters = true)`, perform a best-effort check (the user method's first parameter must be assignable to/from the SAM's first parameter type) instead of strict matching.
- On mismatch, emit an error via `Messager.printMessage(Diagnostic.Kind.ERROR, msg, methodElement)` so the IDE underlines the offending method and `javac` exits non-zero.

The validator SHALL NOT crash on unrelated input. Any internal exception SHALL be caught and reported as a `Diagnostic.Kind.WARNING` rather than propagated.

#### Scenario: Listener with matching signature compiles

- **WHEN** a method `@OnEntityUpdated public void on(Entity e, FieldPath[] fps, int n)` is compiled
- **THEN** the validator SHALL accept it

#### Scenario: Listener with leading Context parameter compiles

- **WHEN** a method `@OnEntityUpdated public void on(Context ctx, Entity e, FieldPath[] fps, int n)` is compiled
- **THEN** the validator SHALL accept it

#### Scenario: Listener with wrong arity fails compilation

- **WHEN** a method `@OnEntityUpdated public void on(Entity e)` is compiled
- **THEN** the validator SHALL emit an error naming the annotation, the user method, and the expected SAM signature
- **AND** `javac` SHALL exit with a non-zero status

#### Scenario: Listener with wrong type fails compilation

- **WHEN** a method `@OnEntityUpdated public void on(String foo, FieldPath[] fps, int n)` is compiled
- **THEN** the validator SHALL emit an error indicating that parameter 1 should be `Entity`, not `String`

#### Scenario: OnMessage listener with specific message type compiles

- **WHEN** a method `@OnMessage(CSVCMsg_GameEventList.class) public void on(CSVCMsg_GameEventList msg)` is compiled
- **AND** `OnMessage` is marked `dynamicParameters = true`
- **THEN** the validator SHALL accept it (the user's parameter type is assignable to/from `GeneratedMessage`)

### Requirement: `@Provides` indexing via the same annotation processor

The same annotation processor that performs listener-signature validation SHALL also index `@Provides`-annotated classes into a `META-INF/clarity/providers.txt` resource file written via `Filer.createResource(...)`.

The file format SHALL be one fully-qualified class name per line. The order is unspecified but SHALL be stable across builds (alphabetical or registration order, processor's choice).

`UsagePoints.java` SHALL read this resource at static-initializer time using `ClassLoader.getResources("META-INF/clarity/providers.txt")` and resolve each named class via `Class.forName(...)`. Classes that fail to resolve SHALL be skipped with a debug log entry (consistent with the existing classindex behavior of tolerating IDE-induced inconsistencies).

The framework SHALL NOT depend on `org.atteo.classindex` at compile time or runtime.

#### Scenario: Processor class indexed at compile time

- **WHEN** a class `Entities` annotated with `@Provides({...})` is compiled
- **THEN** the annotation processor SHALL append `skadistats.clarity.processor.entities.Entities` to `META-INF/clarity/providers.txt`

#### Scenario: Indexed class loaded at runtime

- **WHEN** `UsagePoints` static initializer runs
- **THEN** it SHALL read `META-INF/clarity/providers.txt` from the classpath
- **AND** call `Class.forName(...)` for each entry
- **AND** populate its provider registry with the resolved classes

#### Scenario: Class fails to resolve

- **WHEN** an entry in `META-INF/clarity/providers.txt` cannot be loaded (e.g., the IDE wrote an inconsistent file)
- **THEN** `UsagePoints` SHALL skip the entry with a debug log message and continue
- **AND** SHALL NOT throw an exception

### Requirement: Custom event annotations follow the same contract

Annotations defined outside the clarity core (e.g., in `clarity-examples`) marked with `@UsagePointMarker(EVENT_LISTENER)` SHALL declare the same nested `Listener` / optional `Filter` / `Event` types as core annotations.

The framework SHALL apply the same discovery, binding, and validation rules to such custom annotations as to core annotations, including the compile-time validator (provided the consuming module declares the `clarity-processor` as an `annotationProcessor` in its build).

#### Scenario: clarity-examples lifestate event

- **WHEN** `clarity-examples`'s `@OnEntitySpawned` annotation is declared with nested `Listener` and `Event` types
- **THEN** processors that `@InsertEvent OnEntitySpawned.Event` SHALL receive a typed dispatch instance
- **AND** processors that `@OnEntitySpawned` annotate a method SHALL have their method bound via LMF exactly as for core annotations
- **AND** the compile-time validator SHALL check the listener method's signature against `OnEntitySpawned.Listener.invoke(Entity)`

#### Scenario: Custom annotation missing nested types fails compilation

- **WHEN** an external user declares `@interface MyEvent` with `@UsagePointMarker(EVENT_LISTENER)` but no nested `Listener` interface
- **AND** the user's module declares the clarity annotation processor
- **THEN** compilation SHALL fail at the first method annotated with `@MyEvent` with an error explaining the missing nested type
