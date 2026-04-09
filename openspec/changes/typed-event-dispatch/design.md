## Context

Clarity's event subsystem (`skadistats.clarity.event`) routes every listener through an `Object[]`-flavored pipeline:

- `Event<A>.raise(Object... args)` allocates an `Object[]` per call (post-commit `7fd2e5b` it iterates a flat sorted listener array, but the array still passes through `Object[]`).
- Each listener stores a `MethodHandle` produced by `unreflect(method).bindTo(receiver).asSpreader(Object[].class, arity)`. Dispatch is `methodHandle.invokeExact(args)`, which unpacks the `Object[]` back into positional args at call time.
- Filters are `Predicate<Object[]>` SAMs that always do `(SomeType) args[0]` as their first action. After commit `ec91afd` the entity-event filter caches its per-DTClass match result, so the filter call itself is fast — but it still goes through the `Object[]` interface.
- `@UsagePointMarker(parameterClasses = ...)` declares the listener's expected parameter types statically, but those types are only used at init time for compatibility checks.
- `AbstractInvocationPoint.setParameterClasses(...)` and `isInvokedForParameterClasses(...)` exist to specialize a listener's expected parameters at runtime. The only callers are `InputSourceProcessor.initOnMessageListener` and `initOnPostEmbeddedMessageListener`. This mechanism, combined with `ExecutionModel.computeListenersForEvent(eventType, parameterTypes)`, enables `OnMessage` to maintain a *family* of `Event<OnMessage>` instances — one per concrete message class — each containing only the listeners that subscribed to that specific class.
- `@InsertEvent.override()` and `@InsertEvent.parameterTypes()` exist on the annotation but **no caller in the codebase ever sets `override = true`**. Dead code.
- `PropertyChange.java` already does dispatch correctly: it bypasses `Event.raise` entirely and dispatches via a hand-rolled per-DTClass-partitioned `ListenerAdapter[]`. It is the model for what dispatch should look like across the board, but its logic is currently wrapped in a stand-alone processor rather than living next to its annotation.
- `org.atteo.classindex:classindex` is consumed for one purpose only: `UsagePoints.java`'s static initializer calls `ClassIndex.getAnnotated(Provides.class)` to discover all processor classes. classindex's annotation processor writes a META-INF resource file at compile time; the runtime call reads it.
- `clarity-examples` contains three processors that define their own custom event annotations: `lifestate` (3 annotations), `cooldowns` (3 annotations), `s2effectdispatch` (1 annotation). They are the canonical examples of "external user defines a custom event" and exercise the same `@UsagePointMarker` + `Context.createEvent(...)` machinery.

Recent commits laid the groundwork:
- `ec91afd` — per-`(classPattern, DTClass)` cache for entity-event filters; lazy `Pattern.compile`.
- `7fd2e5b` — `Event<A>` listener storage flattened from `TreeMap<Integer, HashSet>` to a sorted `EventListener[]`.

The bench `eventdispatchbench` (added as part of the same work) provides the baseline to evaluate this change against.

Stakeholders: clarity maintainers (SPI break to internal event types), external users with custom event annotations (notably `clarity-examples` itself, plus any out-of-tree processor library).

## Goals / Non-Goals

**Goals:**

- Eliminate per-dispatch `Object[]` allocation on the event hot path.
- Eliminate `asSpreader` adapter overhead; replace with `LambdaMetafactory`-generated typed SAMs.
- Make every event annotation self-describing: the annotation file holds the marker, the listener SAM, the optional filter SAM, and the typed `Event` subclass that owns dispatch. One file = the entire contract for that event.
- Make listener method signatures compile-time-checked against the annotation's `Listener` SAM. Mismatches become `javac` errors, not runtime failures.
- Delete the `setParameterClasses` / `isInvokedForParameterClasses` / `@InsertEvent.override` plumbing entirely. Move `OnMessage`'s per-message-class dispatch into the typed `OnMessage.Event` subclass as an internal partition.
- Move `PropertyChange`'s dispatch logic into `OnEntityPropertyChanged.Event` so it follows the same "annotation owns its dispatch" rule as everything else. `PropertyChange.java` survives only as a thin `@OnEntityUpdated → for-each-fp → raise` adapter.
- Replace the `org.atteo.classindex` dependency with our own annotation processor that handles both `@Provides` indexing and the new listener-signature validation in one place.
- Migrate `clarity-examples`'s custom event annotations as part of the same change so the codebase compiles and runs end-to-end.

**Non-Goals:**

- Adding DTClass-partitioned dispatch to entity events that don't currently have it (`OnEntityCreated`, `OnEntityDeleted`, etc.). The bench shows these are not bottlenecks for typical usage; the existing per-classPattern cache (`ec91afd`) already handles them adequately.
- Generalizing the partition-by-discriminator pattern into a framework primitive. Only `OnMessage` and `OnEntityPropertyChanged` need it, and their requirements are different enough (single-key vs. two-level filter) that a shared abstraction would be larger than the two specific implementations.
- Restructuring `Context.createEvent` callers in user code beyond removing the `parameterTypes` varargs. The factory still exists and is still callable; only its signature simplifies.
- Migrating to a SAM-based listener registration API (`r.on(OnXxx).listen((arg) -> ...)`). The annotation-driven registration model stays.
- Renaming `@Initializer`. Its job narrows but its name and basic role are unchanged.
- Touching the `MethodHandle` infrastructure outside the event package. LMF is built on `MethodHandle`; we are changing how the binding code uses it, not removing `MethodHandle` from the codebase.

## Decisions

### Decision 1: Three nested types per annotation — `Listener`, `Filter`, `Event`

Each `@UsagePointMarker(EVENT_LISTENER)`-bearing annotation gains three nested types:

- `interface Listener` (required) — typed dispatch SAM. Single abstract method `invoke(...)` whose parameter list matches `parameterClasses` from the marker, including primitives.
- `interface Filter` (optional) — typed filter SAM. Same arity and parameter types as `Listener`, but returns `boolean`. Omitted for annotations whose listeners are never filtered.
- `class Event extends skadistats.clarity.event.Event<ThisAnnotation>` (required) — typed dispatch class. Holds the parallel `Listener[]` and `Filter[]` arrays and exposes a typed `raise(...)` method matching the SAM signature.

**Rationale**: nested types in `@interface` declarations are valid Java (verified via spike). Co-location makes each annotation self-describing. Eliminates the "scroll between five files" problem. Primitive parameters survive end-to-end without boxing because the SAM uses real types, not generics.

**Alternatives considered**:
- *Arity-generic SAMs* (`Listener1<A>`, `Listener2<A,B>`, `Listener3<A,B,C>`). Rejected — primitive parameters get boxed at the SAM boundary, `@InsertEvent` field types become unreadable (`Event3<OnEntityUpdated, Entity, FieldPath[], Integer>`), and the boilerplate savings are smaller than they look once you account for the per-annotation parameter-class declarations that still have to live somewhere.
- *Hand-written `Event` subclasses in a separate package*. Rejected — splits the source of truth, no benefit over nested types.

### Decision 2: Convention-based discovery (no marker parameter)

`@UsagePointMarker` does **not** gain `listenerInterface` / `eventClass` / `filterInterface` parameters. The framework discovers nested types by fixed convention via `annotationClass.getDeclaredClasses()`:

- A nested `interface` named `Listener` → the listener SAM (required).
- A nested `interface` named `Filter` → the filter SAM (optional).
- A nested `class` named `Event` extending `skadistats.clarity.event.Event<...>` → the typed Event class (required).

Missing required types fail at processor registration time with a clear error.

**Rationale**: convention is self-documenting. Anyone reading `OnEntityUpdated.java` immediately sees the contract. No redundant cross-references for the compiler to fail to verify.

### Decision 3: Filter SAMs mirror Listener arity

`Filter` has the same parameter list as `Listener`. Filter implementations that only need the first arg simply ignore the rest in their lambda body — the JIT eliminates dead arguments after inlining at zero runtime cost.

**Rationale**: symmetry simplifies binding (one pipeline for both SAMs). Future-proofs the API for custom event annotations whose filters might want access to non-discriminator arguments. Custom event annotations defined outside clarity (notably in `clarity-examples`) get full flexibility.

**Alternatives considered**:
- *First-arg-only filters*. Rejected — asymmetric with Listener, special-case binding code, limits external user flexibility for zero realized benefit.

### Decision 4: `LambdaMetafactory` for both Listener and Filter binding

At listener-bind time, `AbstractInvocationPoint.bind(Context)` resolves the user method via `MethodHandles.privateLookupIn(processorClass, MethodHandles.lookup()).unreflect(method)`, binds the receiver (and optionally `Context`), and then runs `LambdaMetafactory.metafactory(...)` with the annotation's nested `Listener` SAM as the target. Result: a typed SAM instance whose JIT-compiled `invoke(...)` calls the user method directly (no `asSpreader`, no `Object[]`).

Filter binding works identically, targeting the nested `Filter` SAM instead.

**Rationale**: LMF is the JDK's blessed mechanism for invokedynamic-style lambda creation. Generates hidden classes that JIT inlines as direct virtual calls. This is what we should have been doing from the start.

**Alternatives considered**:
- *Keep `MethodHandle.invokeExact` + `asSpreader`*. The status quo. Rejected — that's what we're trying to leave behind.
- *ASM/cglib bytecode generation*. Reinvents what LMF already does. Needless dependency.

### Decision 5: Typed `Event` subclass holds parallel `Listener[]` and `Filter[]` arrays

Inside each annotation's nested `Event` class:

```java
class Event extends skadistats.clarity.event.Event<OnEntityUpdated> {
    private Listener[] listeners;
    private Filter[] filters;  // null entries for unfiltered listeners

    public void raise(Entity e, FieldPath[] fps, int n) {
        for (int i = 0; i < listeners.length; i++) {
            Filter f = filters[i];
            if (f != null && !f.test(e, fps, n)) continue;
            try {
                listeners[i].invoke(e, fps, n);
            } catch (Throwable t) {
                handleListenerException(i, t);
            }
        }
    }
}
```

**Exception routing**: `handleListenerException` takes the listener index (not an `Object[]` of arguments). The base class uses the index to identify the `EventListener` that threw; the exception handler has access to the listener's metadata (annotation, processor class, method name) which is what matters for diagnostics. The typed arguments are not needed for exception routing — they are on the stack frame if a debugger is attached, and logging them is the exception handler's choice, not the dispatch loop's job. This keeps the dispatch loop `Object[]`-free on both the happy and the error path.

The base `Event<A>` retains the existing flat `EventListener<A>[]` (used for ordering, exception routing, listener metadata), and the typed subclass populates its parallel `Listener[]`/`Filter[]` arrays from those `EventListener<A>` instances at construction time (via the SAM instances stored on each `EventListener`).

**Rationale**: parallel arrays are the simplest fast representation. No wrapper objects, no per-element allocation. Base class still owns ordering and exception routing — the typed subclass is just a thin typed wrapper.

### Decision 6: `OnMessage` partitions by message class internally

`OnMessage.Event` (and similarly `OnPostEmbeddedMessage.Event`) maintains an internal `Map<Class<? extends GeneratedMessage>, Listener[]>` keyed by `annotation.value()`. At init time, when listeners are populated, each listener is bucketed by its annotation's `value()` field. At dispatch time, `raise(GeneratedMessage msg)` looks up `msg.getClass()` in the map and invokes only the matching listeners:

```java
class Event extends skadistats.clarity.event.Event<OnMessage> {
    private final Map<Class<? extends GeneratedMessage>, Listener[]> byClass;

    public void raise(GeneratedMessage msg) {
        var bucket = byClass.get(msg.getClass());
        if (bucket != null) for (var l : bucket) l.invoke(msg);
    }
}
```

**Result**: The external `evOnMessages` map in `InputSourceProcessor` goes away. There is exactly one `Event` instance per annotation type. The setParameterClasses/isInvokedForParameterClasses machinery has no reason to exist anymore.

**Wildcard listeners**: a user may declare `@OnMessage public void on(GeneratedMessage msg)` without specifying a `value()`. Such listeners are stored in a separate `Listener[] wildcardListeners` array (not in `byClass`). On every `raise(GeneratedMessage msg)`, the wildcard listeners are invoked unconditionally after the class-specific bucket (if any). This matches the current behavior where an unfiltered `OnMessage` listener receives every message.

**Listener-method type adaptation**: a user's `@OnMessage(SomeSpecificMsg.class) public void on(SomeSpecificMsg msg)` is bound via LMF using `instantiatedMethodType` so the SAM signature is `(GeneratedMessage)` but the underlying `MethodHandle` is the user's specific method. The `byClass` partition guarantees the cast succeeds (the listener is only in the bucket for messages of its declared type or subtypes).

**Rationale**: the partition is a private implementation detail of `OnMessage.Event`, not a generic framework primitive. Other annotations don't need it. Keeping it local is simpler than abstracting it.

### Decision 7: `PropertyChange` dispatch logic moves into `OnEntityPropertyChanged.Event`

The hand-rolled `ListenerAdapter`, `adaptersByClass`, and per-DTClass property-name match cache currently in `PropertyChange.java` move into `OnEntityPropertyChanged.Event` as nested implementation details. The dispatch method becomes:

```java
class Event extends skadistats.clarity.event.Event<OnEntityPropertyChanged> {
    private final IdentityHashMap<DTClass, Listener[]> byClass = new IdentityHashMap<>();
    // ... per-listener propertyPattern caches as before

    public void raise(Entity e, FieldPath fp) {
        var listeners = byClass.computeIfAbsent(e.getDtClass(), this::buildBucket);
        for (var l : listeners) {
            // propertyPattern check (same caching as before)
            l.invoke(e, fp);
        }
    }
}
```

`PropertyChange.java` shrinks to a thin processor:

```java
public class PropertyChange {
    @InsertEvent
    private OnEntityPropertyChanged.Event evPropertyChanged;

    @OnEntityUpdated
    public void onUpdate(Entity e, FieldPath[] fps, int num) {
        for (int i = 0; i < num; i++) {
            evPropertyChanged.raise(e, fps[i]);
        }
    }
}
```

**Rationale**: brings `OnEntityPropertyChanged` into line with the convention "annotation owns its dispatch". Eliminates a dispatch path that special-cases everything. Performance is identical — same code, just relocated.

**Alternatives considered**:
- *Leave `PropertyChange.java` as-is*. Rejected — creates a dauerhaft asymmetry where one event has a different dispatch shape than every other. The migration is mechanical (move code, change file), not risky.

### Decision 8: New annotation processor in a `processor` source set inside `clarity`

A new Gradle source set `processor` inside the existing `clarity` project contains a `javax.annotation.processing.Processor` implementation that handles two compile-time tasks:

**Task A: Listener-signature validation.** Walks the round environment's annotated elements; for each method, checks if any of its annotations is itself marked with `@UsagePointMarker(EVENT_LISTENER)`; if so, locates the annotation's nested `Listener` SAM via `javax.lang.model.element.TypeElement.getEnclosedElements()` and compares the SAM's parameter list to the user method's. Tolerates a leading `Context` parameter on the user method. On mismatch, emits an error via `Messager.printMessage(Diagnostic.Kind.ERROR, msg, methodElement)`.

**Task B: `@Provides` indexing.** For each class annotated with `@Provides`, writes its fully-qualified name to a `META-INF/clarity/providers.txt` resource file via `Filer.createResource(...)`. Replaces classindex's role.

The processor declares `@SupportedAnnotationTypes("*")` (so it sees every annotation, since custom event annotations live outside its compile-time knowledge) and handles its work efficiently by short-circuiting elements that don't match either task.

**Where it lives**: a new Gradle source set `processor` inside the existing `clarity` project. The processor sources sit at `clarity/src/processor/java/...`. The Gradle build compiles them via `compileProcessorJava` *before* the main source set, then wires them as `annotationProcessor(sourceSets["processor"].output)` for `compileJava`. The `jar` task pulls from both source sets so processor classes ship inside `clarity.jar`. The `META-INF/services/javax.annotation.processing.Processor` resource lives at `clarity/src/processor/resources/META-INF/services/...` and goes into the same jar.

**No new published artifact.** Downstream consumers (including `clarity-examples` and external users) add `annotationProcessor("skadistats:clarity:<version>")` to their build alongside their existing `implementation("skadistats:clarity:<version>")`. The same jar serves both roles via separate Gradle configurations. This is the exact packaging pattern `org.atteo.classindex` uses today (one jar, runtime API + processor + service file), so we are replicating a known-good model.

**Runtime side**: `UsagePoints.java`'s static initializer changes from `ClassIndex.getAnnotated(Provides.class)` to reading `META-INF/clarity/providers.txt` from the classpath via `ClassLoader.getResources(...)` and looking up each named class via `Class.forName(...)`. Same semantics, no runtime dependency on classindex.

**Rationale**: one processor for two related tasks is cheaper than two separate processors and lets us delete classindex. Listener-signature validation is the one Java-level mechanism that can enforce annotation/method consistency at compile time. The single-jar packaging means no new release surface — clarity continues to ship as one artifact, just with the processor bundled in.

**Alternatives considered**:
- *A separate Gradle sub-project `clarity-processor` with its own published Maven artifact*. Rejected — introduces a new released artifact with version-coordination overhead and forces downstream users to add a second dependency. The source-set approach gives the same compile-time-isolation guarantees with none of the release/packaging cost.

**Alternatives considered**:
- *Use classindex for indexing, add our own processor only for validation*. Rejected — leaves a dependency we don't need.
- *Skip validation, runtime errors only*. Rejected — the typed-dispatch refactor specifically aims to surface contract violations earlier, and compile-time is the natural place.
- *Defer the validator/processor work to a follow-up change*. Rejected — the validator depends on the nested-`Listener` convention introduced by this change, and the migration moment is exactly when validation matters most.

### Decision 9: Validator best-effort for OnMessage-style late-typed annotations

`OnMessage` and `OnPostEmbeddedMessage` have a quirk: the user's listener method takes a *specific* message type (`CSVCMsg_GameEventList`), but the annotation marker declares the most-general type (`GeneratedMessage`). After the refactor, the nested `OnMessage.Listener` SAM uses `GeneratedMessage` and LMF's `instantiatedMethodType` adapts the user's specific method to it.

For these annotations, the validator does a **best-effort** check: the user method's parameter type must be assignable to/from the SAM's declared parameter type. It does not attempt to read the `value()` argument from the annotation invocation and verify a more specific match — that level of validation is left to the LMF bind step at runtime, where the failure is clear.

**How the validator distinguishes**: an annotation can opt in by setting `@UsagePointMarker(..., dynamicParameters = true)`. Default is `false`. When `true`, the validator skips strict parameter-list matching and does only the assignability check.

**Rationale**: keeps the validator simple. The handful of dynamic-parameter annotations (3-4 in the codebase) get a known-weaker check; runtime continues to be the source of truth for the specific case.

### Decision 10: `@Initializer` survives with a narrowed role

`@Initializer` methods continue to exist and run after listeners have been registered but before dispatch begins. Their job narrows:

- They no longer call `setParameterClasses(...)` (the method is gone).
- They no longer call `setInvocationPredicate(Predicate<Object[]>)`. Instead they call `setFilter(...)` with the annotation's nested `Filter` SAM.
- Side effects on processor state (`requestedTables.add(...)`, `unpackUserMessages |= ...`, etc.) work exactly as today.

**Why not auto-install filters from the annotation?** Considered. Two reasons against:
1. Side-effect callbacks need `@Initializer` to exist anyway (e.g., `BaseStringTableEmitter` records which tables to track based on listener annotations).
2. Most filters use processor-local state (the per-classPattern cache in `Entities`, the `tableName` set in `BaseStringTableEmitter`). Auto-installing them would require either passing processor state into the annotation's nested Event class (architectural mess) or restricting filters to be a pure function of the annotation alone (loses flexibility).

The current design — keep `@Initializer`, just retype its filter API — is the smallest change that gets us the win.

### Decision 11: `@InsertEvent.override()` and `parameterTypes()` are dead code, deleted

Grep confirms: zero call sites set `override = true` anywhere in clarity. The two annotation members exist purely as plumbing for the `setParameterClasses` use case (which is itself going away). Both are removed; `@interface InsertEvent` becomes a pure marker.

`ExecutionModel.injectEvent` simplifies correspondingly — no more `if (fieldAnnotation.override())` branch, no more `parameterTypes` propagation into `createEvent`.

### Decision 12: `Context.createEvent` collapses to a single overload

```java
// before:
public <A extends Annotation> Event<A> createEvent(Class<A> eventType, Class... parameterTypes);

// after:
public <A extends Annotation> Event<A> createEvent(Class<A> eventType);
```

The factory locates the annotation's nested `Event` class via `eventType.getDeclaredClasses()` and instantiates it via reflection. The returned object is statically typed `Event<A>` but is actually the concrete nested subclass — callers that need the typed `raise(...)` cast it (or inject it via `@InsertEvent OnXxx.Event` instead, which is the recommended pattern).

`clarity-examples`'s `lifestate` and `cooldowns` are the canonical callers of the old `createEvent(Class, Class...)` signature; both are migrated as part of this change.

### Decision 13: Migration as one logical change, structured internally as ordered steps

The migration is one PR / commit chain on `master`. It is structured internally as a sequence of incremental steps (see Migration Plan), each of which leaves the codebase compiling and testable, but they ship together because there is no useful intermediate state. A half-migrated event system would have to support both the typed and the legacy paths in parallel, doubling complexity.

`MIGRATION.md` is added to the change directory with explicit upgrade steps for external users with custom event annotations.

### Decision 14: Typed `Event` subclasses are `final`

Each annotation's nested `Event` class is declared `final`. Subclassing the typed dispatch class is not a supported extension point.

**Rationale**: `final` enables JIT devirtualization of the `raise(...)` call (the JIT can inline the dispatch loop without a type guard). It also makes the contract explicit — the annotation owns its dispatch, and there is no inheritance-based customization path. If a custom event annotation needs different dispatch behavior, it implements that in its own `Event` class, not by subclassing someone else's.

## Risks / Trade-offs

- **Risk**: `LambdaMetafactory` requires that the user method be reachable from a `Lookup` with adequate access. Some processor methods are package-private. → **Mitigation**: use `MethodHandles.privateLookupIn(processorClass, MethodHandles.lookup())` from inside the clarity event package, which gives full access. Verify on a sample annotation during step 2 of the migration.

- **Risk**: nested `Event` subclasses inside `@interface` declarations create a self-reference (the class refers to its enclosing annotation as the type parameter to `Event<A>`). → **Mitigation**: spike confirmed this compiles and runs (JLS §9.6.1 explicitly allows nested types in annotation declarations).

- **Risk**: `clarity-examples` migration touches three example projects. If we get it wrong, examples stop working. → **Mitigation**: each affected example is verified by running its `*Run` Gradle task post-migration as part of the change validation step.

- **Risk**: the new annotation processor breaks IDEs that don't run annotation processors correctly (notably some IntelliJ configurations). → **Mitigation**: defensive programming — wrap processor logic in try/catch, never crash, always emit diagnostics through the standard `Messager` channel. classindex already runs in this codebase without issues, so the IDE-integration story is already validated. Our processor just becomes a second consumer of the same pipeline.

- **Risk**: `Context.createEvent` reflective lookup adds a small one-time cost per event (vs. the previous direct `new Event<>(...)` call). → **Mitigation**: this is paid once per `(annotation, replay)` and is utterly negligible compared to the per-dispatch wins. No hot-path concern.

- **Risk**: the LMF binding pipeline must handle `OnMessage`'s `instantiatedMethodType` adaptation correctly for the specific message types (`CSVCMsg_GameEventList`, etc.). This is exactly what `instantiatedMethodType` is for, but it needs verification. → **Mitigation**: explicit test in step 6 of the migration plan, with `OnMessage` as the worked example. If LMF can't make it work cleanly, fall back to a hand-written wrapper inside `OnMessage.Event` that does the cast.

- **Risk**: performance gain may be smaller than expected. The JIT has been working hard to hide `asSpreader` overhead. → **Mitigation**: `eventdispatchbench` is in place; we bench after each major step and document honest numbers in the commit message. If the gain is truly negligible, the type-safety and dead-code-removal value still stands — but we should be honest about it.

- **Trade-off**: each annotation grows by ~10–15 lines (nested types). For ~15 core annotations + ~7 in clarity-examples that's ~250–350 lines of new code. Co-located, regular, self-documenting boilerplate, but still boilerplate. We accept it for the contract clarity.

- **Trade-off**: one new source set to maintain (`processor`). It's small and stable (annotation processors don't change often), but it's a non-zero maintenance surface. We trade it against removing the `org.atteo.classindex` dependency, which had its own maintenance and IDE-quirk surface.

- **Trade-off**: `Event<A>` base class is more abstract — no `raise(...)` method on the base, only on the typed subclass. Code that wants polymorphic `raise(...)` over the base loses that. The only legitimate user is exception routing, which uses `Event.handleListenerException(...)` and stays at the base level.

## Migration Plan

The migration is one logical change but structured as ordered steps. Each step leaves the codebase compiling and testable.

1. **Reconnaissance**: enumerate every event annotation, every `@InsertEvent`, every `@Initializer` setting a predicate, every `Context.createEvent` caller, every annotation that uses `setParameterClasses`. Document target signatures and any quirks (`OnMessage`'s late typing, `Context`-prepended user methods, etc.).
2. **Source set setup**: create the `processor` source set inside the `clarity` Gradle project. Wire it into `clarity`'s `annotationProcessor` configuration. Empty processor at first — just verify the build pipeline is healthy and the processor classes get discovered.
3. **LMF binding infrastructure** in `clarity` event package: helper to look up nested SAMs by reflection, helper to produce a SAM instance via `LambdaMetafactory.metafactory(...)`, new fields on `EventListener` for the typed listener and filter SAMs, new `setFilter(Object)` method, modified `bind(Context)` that uses `privateLookupIn` and LMF. Keep the legacy `Object[]` path alive in parallel as a fallback.
4. **Worked example**: pick `OnEntityCreated` (single arg, simplest). Add nested `Listener` and `Event`. Migrate `Entities`'s `@InsertEvent` field, `evCreated.raise(...)` call site, and `@Initializer(OnEntityCreated.class)` (filter via `setFilter`). Verify `clarity-examples` still runs.
5. **Migrate the rest of the entity annotations**: `OnEntityUpdated`, `OnEntityDeleted`, `OnEntityEntered`, `OnEntityLeft`, `OnEntityPropertyCountChanged`. Same pattern. Bench after this step and record numbers.
6. **`OnEntityPropertyChanged` + `PropertyChange.java` rework**: add nested `Listener`, `Filter`, `Event` with the partition logic moved in. Shrink `PropertyChange.java` to the thin adapter form. Bench again.
7. **Migrate `OnMessage` and `OnPostEmbeddedMessage`**: the trickiest. Add nested `Listener` (with most-general parameter type), nested `Event` with internal `Map<Class<?>, Listener[]>` partition. Verify `instantiatedMethodType` LMF adaptation works for specific message types. Migrate `InputSourceProcessor`'s initializers (drop `setParameterClasses`, drop the external `evOnMessages` map, switch the raise sites). Add `dynamicParameters = true` to these annotations' `@UsagePointMarker`.
8. **Migrate remaining annotations**: `OnGameEvent`, `OnGameEventDescriptor`, `OnCombatLogEntry`, `OnStringTableEntry`, `OnStringTableCreated`, `OnStringTableClear`, `OnPlayerInfo`, `OnTickStart`, `OnTickEnd`, `OnReset`, `OnInit`, `OnInputSource`, `OnFullPacket`, `OnSyncTick`, `OnDTClass`, `OnDTClassesComplete`, `OnTempEntity`, `OnModifierTableEntry`, `OnMessageContainer`. Same pattern.
9. **Annotation processor implementation**: implement Task A (listener-signature validation) and Task B (`@Provides` indexing) in the `processor` source set. Update `UsagePoints.java` to read `META-INF/clarity/providers.txt` instead of calling `ClassIndex.getAnnotated(...)`. Drop `@IndexAnnotated` from `Provides.java`. Remove `org.atteo.classindex` from `clarity`'s Gradle dependencies.
10. **Remove the legacy untyped path**: delete `Event.raise(Object...)`, `EventListener.invoke(Object...)`, `setInvocationPredicate(Predicate<Object[]>)`, `setParameterClasses(...)`, `isInvokedForParameterClasses(...)`, the `parameterClasses` field, `InvocationPoint.getArity()`, `@InsertEvent.override()`, `@InsertEvent.parameterTypes()`, `skadistats.clarity.util.Predicate`, the `methodHandle` + `asSpreader` setup. Simplify `Context.createEvent(...)` to its single-arg form. Verify everything still compiles.
11. **Migrate `clarity-examples`**: `lifestate` (3 annotations + processor), `cooldowns` (3 annotations + processor), `s2effectdispatch` (1 annotation + processor). Run each affected example via its `*Run` Gradle task to verify behavioral parity.
12. **Bench and document**: run `eventdispatchbench` against all three replays. Record before/after numbers per configuration. Run `propertychangebench` to confirm the relocated PropertyChange dispatch performs identically. Run a representative subset of clarity-examples (`combatlog`, `lifestate`, `matchend`, `info`, `cooldowns`, `s2effectdispatch`).
13. **Documentation**: write `MIGRATION.md` in the change directory with the upgrade steps for external users with custom event annotations. Add brief Javadoc to the LMF binding helper and the annotation processor explaining the convention.

**Rollback**: this is a single PR / chain on `master`. Rollback is `git revert`. No runtime configuration toggle.

## Open Questions

- Does `MethodHandles.privateLookupIn(...)` handle every visibility scenario in the codebase, or are there edge cases (e.g., methods on non-public processor classes loaded via reflection)? Empirical — answered during step 3.
- For `OnMessage`-style annotations, is `instantiatedMethodType` sufficient to make LMF bind specific message types to a `GeneratedMessage`-typed SAM, or does the user method need an explicit cast wrapper? Empirical — answered during step 7.
- The annotation processor needs to be discoverable both for `clarity`'s own build and for downstream consumers (e.g. `clarity-examples`). Verify the `META-INF/services` registration path actually triggers in both contexts during step 2.
- Should `MIGRATION.md` live inside `openspec/changes/typed-event-dispatch/` (where it's part of the change record) or at the repo root (where users find it post-merge)? Probably both: the change directory has the source-of-truth version; a copy or link sits at the repo root. Decide during step 13.
