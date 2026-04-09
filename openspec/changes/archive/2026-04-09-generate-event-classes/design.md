## Context

The typed-event-dispatch migration moved clarity's event system from reflection-based dispatch to type-safe SAM-based dispatch. Every event annotation now contains three nested types: a `Listener` interface (SAM for the handler), an optional `Filter` interface (SAM for filtering), and a `final class Event` that wires up typed arrays and implements the dispatch loop. The Event class is ~20-30 lines of boilerplate that is nearly identical across 26 of 29 annotations — only the parameter types and filter presence vary. Only `OnEntityPropertyChanged` has truly custom dispatch logic (Adapter/caching for class+property pattern matching). Two further annotations (`OnMessage`, `OnPostEmbeddedMessage`) use a bucketing pattern that is itself templateable.

The Event class is referenced as a field type by processors (`private OnEntityEntered.Event evEntered;`) and the framework injects the instance. Call sites use `evEntered.raise(entity)`.

## Goals / Non-Goals

**Goals:**
- Eliminate duplicated Event class boilerplate across ~27 event annotations
- Preserve exact runtime behavior: same typed arrays, same dispatch loop, no boxing of primitives
- Keep processor-facing API unchanged: `OnX.Event` field type, `.raise(...)` calls, `.isListenedTo()`
- Remove redundant `parameterClasses` from `@UsagePointMarker`

**Non-Goals:**
- Changing the Listener/Filter SAM pattern — those stay as hand-written interfaces
- Modifying `OnEntityPropertyChanged` — it keeps its hand-written implementation
- Changing how LMF binding works — `LmfBinder` and the SAM creation pipeline are untouched
- Adding new event features (priorities, async, etc.)

## Decisions

### 1. Event inner type becomes an interface, generated class implements it

**Decision**: `OnX.Event` becomes a nested `interface` inside the annotation. An annotation processor generates `OnX_Event` (top-level class) that `implements OnX.Event`.

**Rationale**: The processor field type stays `OnX.Event` — no breaking change. The interface only declares `raise(...)` and inherits `isListenedTo()` from a shared `EventBase` interface. The generated class contains the actual dispatch logic. This cleanly separates contract (interface, hand-written) from implementation (class, generated).

**Alternative considered**: Generating the Event as a nested class inside the annotation. Rejected because annotation processors cannot generate nested classes — they can only create new top-level compilation units.

**Alternative considered**: Abstract intermediate classes (`FilterableEvent1<A, T>`). Rejected because Java generics cannot represent primitives (`FilterableEvent1<OnTickStart, boolean>` is illegal), requiring either boxing (performance regression) or specialized classes per primitive combination (combinatorial explosion).

### 2. Base interface `EventBase` for shared methods

**Decision**: Introduce `skadistats.clarity.event.EventBase` interface:

```java
public interface EventBase {
    boolean isListenedTo();
}
```

Each annotation's `Event` interface extends `EventBase` and adds only `raise(...)`:

```java
interface Event extends EventBase {
    void raise(Entity e);
}
```

**Rationale**: `isListenedTo()` is used at call sites for short-circuit checks. It must be accessible through the interface. Putting it in a base interface avoids repeating it in every annotation.

### 3. AP inspects Listener/Filter SAMs to generate dispatch code

**Decision**: The AP reads the `Listener` interface's SAM method to determine `raise()` parameter types (including primitives) and reads the optional `Filter` interface to determine if filter logic is needed. It then generates the standard constructor + dispatch loop with the exact types.

**Rationale**: The Listener interface is already the single source of truth for the event's parameter signature. The AP just translates it mechanically into the dispatch code. No configuration, no extra annotations beyond `@GenerateEvent`.

### 4. `EventContractDiscovery` fallback for generated classes

**Decision**: Modify `EventContractDiscovery.doDiscover()`:
1. First, look for a nested class named `Event` (existing behavior — supports custom implementations).
2. If not found, look for `<AnnotationName>_Event` in the same package via `Class.forName()`.

**Rationale**: This provides a clean opt-out: annotations that need custom dispatch logic simply don't use `@GenerateEvent` and provide their own nested `Event` class. The discovery prefers nested classes, so existing custom implementations always win.

### 5. Remove `parameterClasses` from `@UsagePointMarker`

**Decision**: Remove the `parameterClasses` attribute. The single consumer (`InitializerMethod.bind()`) uses it only for `parameterClasses().length` to size the `asSpreader` call. Replace with `EventContractDiscovery.discover(annotation.value()).listenerMethod().getParameterCount()`.

**Rationale**: `parameterClasses` duplicates information already present in the Listener SAM. Removing it eliminates a source of inconsistency (parameter types declared in two places).

### 6. AP lives in the clarity module itself, uses JavaPoet

**Decision**: The annotation processor is part of the clarity module, not a separate module. `@GenerateEvent` and the processor class live in `skadistats.clarity.event`. The AP uses [JavaPoet](https://github.com/palantir/javapoet) for source generation.

**Rationale**: The AP only processes annotations within clarity itself. It has no external consumers. A separate module would add build complexity for no benefit. The AP is registered via `META-INF/services/javax.annotation.processing.Processor`. JavaPoet is a compile-time-only dependency (no runtime footprint) that handles imports, indentation, and type references automatically — critical for the BUCKETED strategy which generates nested classes, generic types (`Map<Class<? extends X>, Entry[]>`), and multiple methods.

### 7. BUCKETED strategy for message-class dispatch

**Decision**: `@GenerateEvent` gains a `strategy` attribute with two values: `STANDARD` (default) and `BUCKETED`. The AP generates different dispatch code per strategy:

- **STANDARD**: Typed Listener[]/Filter[] arrays, linear dispatch loop with optional filter check.
- **BUCKETED**: Listeners bucketed by the annotation's `value()` attribute (which returns `Class<? extends X>`). The default value (e.g., `GeneratedMessage.class`) marks wildcard listeners. The generated class contains:
  - A `Map<Class<?>, Entry[]>` for class-specific listeners
  - An `Entry[]` for wildcard listeners
  - `raise(...)` dispatches to matching bucket + wildcards
  - `isListenedTo(Class<?>)` checks bucket presence or wildcard existence

**Rationale**: `OnMessage` and `OnPostEmbeddedMessage` use identical bucketing logic — only the Listener parameter count differs (1 vs 2). This is a clean, templateable pattern. The AP reads the annotation's `value()` return type to determine the bucketing key type.

The `Event` interface for bucketed events includes the extra method:

```java
interface Event extends EventBase {
    void raise(GeneratedMessage msg);
    boolean isListenedTo(Class<? extends GeneratedMessage> messageClass);
}
```

### 8. `ExecutionModel.injectEvent()` adapted for interface field types

**Decision**: Change the field type detection in `injectEvent()` from `Event.class.isAssignableFrom(fieldType)` to checking whether the field type's enclosing class is an annotation:

```java
if (fieldType.getEnclosingClass() != null && fieldType.getEnclosingClass().isAnnotation()) {
    eventType = (Class<? extends Annotation>) fieldType.getEnclosingClass();
}
```

**Rationale**: The current check (`Event.class.isAssignableFrom`) works only when `OnX.Event` is a class extending `Event<A>`. When `OnX.Event` becomes an interface, this check fails because interfaces cannot extend classes. The enclosing-class check works for both the old (class) and new (interface) patterns, and is actually more direct — it asks "is this type nested inside an annotation?" rather than "is this type a subclass of Event?".

## Risks / Trade-offs

**[Generated code is harder to debug]** → Developers can inspect generated sources in `build/generated/sources/annotationProcessor/`. The generated code is intentionally simple (no abstraction layers) — it reads like the hand-written code it replaces.

**[Two conventions for Event discovery]** → Nested class takes priority, so existing custom events work without changes. The fallback (`_Event` suffix) is a simple, predictable convention. EventContractDiscovery logs which path was taken at debug level.

**[AP ordering with classindex]** → The project already uses classindex's annotation processor. The event AP must run after classindex (or be independent). Since the event AP only generates new source files and doesn't consume classindex output, ordering is not a concern — they are independent.

**[Interface instead of class changes bytecode]** → `OnX.Event` changing from class to interface means existing compiled code referencing it would need recompilation. This is acceptable because clarity is versioned and downstream code recompiles against new versions. The source-level API is unchanged.
