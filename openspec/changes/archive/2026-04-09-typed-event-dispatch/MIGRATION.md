# Migration Guide: Typed Event Dispatch

This guide is for external users who have custom event annotations (defined outside the clarity core).

## What Changed

Every `@UsagePointMarker(EVENT_LISTENER)` annotation must now declare nested `Listener`, optional `Filter`, and `Event` types. The framework dispatches through these typed SAMs instead of `Object[]`.

## Step-by-step Migration

### 1. Add nested types to your annotation

**Before:**
```java
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER, parameterClasses = { Entity.class })
public @interface OnMyEvent {
}
```

**After:**
```java
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER, parameterClasses = { Entity.class })
public @interface OnMyEvent {

    interface Listener {
        void invoke(Entity e);
    }

    // Optional — only needed if you use setFilter() in an @Initializer
    interface Filter {
        boolean test(Entity e);
    }

    final class Event extends skadistats.clarity.event.Event<OnMyEvent> {
        private final Listener[] typedListeners;
        private final Filter[] typedFilters;  // omit if no Filter

        public Event(Runner runner, Class<OnMyEvent> eventType, Set<EventListener<OnMyEvent>> listeners) {
            super(runner, eventType, listeners);
            var els = listeners();
            typedListeners = new Listener[els.length];
            typedFilters = new Filter[els.length];
            for (int i = 0; i < els.length; i++) {
                typedListeners[i] = (Listener) els[i].getListenerSam();
                typedFilters[i] = (Filter) els[i].getFilterSam();
            }
        }

        public void raise(Entity e) {
            for (int i = 0; i < typedListeners.length; i++) {
                var f = typedFilters[i];
                if (f != null && !f.test(e)) continue;
                try {
                    typedListeners[i].invoke(e);
                } catch (Throwable t) {
                    handleListenerException(i, t);
                }
            }
        }
    }
}
```

Required imports: `EventListener`, `Runner`, `Set`.

### 2. Change `@InsertEvent` field declarations

**Before:**
```java
@InsertEvent
private Event<OnMyEvent> evMyEvent;
```

**After:**
```java
@InsertEvent
private OnMyEvent.Event evMyEvent;
```

Raise sites (`evMyEvent.raise(entity)`) compile unchanged.

### 3. Change `createEvent` calls

**Before:**
```java
evMyEvent = ctx.createEvent(OnMyEvent.class, Entity.class);
```

**After:**
```java
evMyEvent = (OnMyEvent.Event) ctx.createEvent(OnMyEvent.class);
```

The `parameterTypes` varargs overload no longer exists.

### 4. Change `setInvocationPredicate` to `setFilter`

**Before:**
```java
@Initializer(OnMyEvent.class)
public void init(EventListener<OnMyEvent> listener) {
    listener.setInvocationPredicate(args -> ((Entity) args[0]).getDtClass()...);
}
```

**After:**
```java
@Initializer(OnMyEvent.class)
public void init(EventListener<OnMyEvent> listener) {
    listener.setFilter((OnMyEvent.Filter) e -> e.getDtClass()...);
}
```

`setInvocationPredicate`, `setParameterClasses`, and `isInvokedForParameterClasses` no longer exist.

### 5. Add annotation processor to your build

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.skadistats:clarity:<version>")
    annotationProcessor("com.skadistats:clarity:<version>")  // NEW — enables compile-time validation
}
```

The processor validates that listener method signatures match the annotation's `Listener` SAM at compile time. Mismatches become compile errors.

## Removed APIs

| Removed | Replacement |
|---------|-------------|
| `Event.raise(Object...)` | Typed `OnXxx.Event.raise(...)` |
| `setInvocationPredicate(Predicate<Object[]>)` | `setFilter(OnXxx.Filter)` |
| `setParameterClasses(Class...)` | Partition logic in typed Event subclass |
| `isInvokedForParameterClasses(Class...)` | Removed |
| `InvocationPoint.getArity()` | Removed |
| `InvocationPoint.invoke(Object...)` | Removed (LMF SAM dispatch) |
| `InvocationPoint.isInvokedForArguments(Object...)` | Removed |
| `@InsertEvent(override, parameterTypes)` | `@InsertEvent` (pure marker) |
| `Context.createEvent(Class, Class...)` | `Context.createEvent(Class)` |
| `skadistats.clarity.util.Predicate` | `java.util.function.Predicate` |
| `org.atteo.classindex` dependency | Built-in annotation processor |
