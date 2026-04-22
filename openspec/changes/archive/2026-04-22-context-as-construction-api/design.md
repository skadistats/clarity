## Context

The codebase has several classes whose only job is to carry state between the runner and the processor graph:

- `EntityStateFactory` — holds type enums + FieldLayoutBuilder + pointerCount; registered as a DI processor despite having no event handlers
- `ContextData` — holds buildNumber, millisPerTick, gameVersion; created eagerly, mutated after parsing starts via shared reference
- `source` in the processor list — no processor injects it; vestigial

All of this belongs on `Context`. Context is already injected everywhere; it should be the single, well-typed source of truth for everything that describes a run.

## Goals / Non-Goals

**Goals:**
- `EntityStateFactory` and `ContextData` are deleted; their state folds into `Context`
- Context has two explicit tiers: structural fields (constructor) and parse-time initialized fields (set-once)
- Context owns entity state construction and field reader construction
- `AbstractRunner` processor list is clean: infrastructure vs user processors separated via hooks, no `Object[]` flattening, no dead entries

**Non-Goals:**
- Changing the runner's public configuration API (`withS2EntityState`, `withS2FieldPath`, etc.)
- Changing any observable parse behaviour or output
- Redesigning the DI/event system

## Decisions

### EntityStateFactory is deleted; fields fold into Context

`s1EntityStateType`, `s2EntityStateType`, `s2FieldPathType` become constructor parameters on `Context`. `FieldLayoutBuilder` is instantiated privately in the constructor. No intermediate factory class is needed.

_Alternative considered_: Keep EntityStateFactory as a private Context field. Rejected — it's just a bag of fields with two methods; the bag adds no value when everything it holds can live directly on Context.

### Context.newEntityState dispatches via sealed switch

```java
public EntityState newEntityState(DTClass cls) {
    return switch (cls) {
        case S2DTClass s2 -> s2EntityStateType.createState(s2.getField(), pointerCount, layoutBuilder);
        case S1DTClass s1 -> s1EntityStateType.createState(s1);
    };
}
```

The switch is exhaustive because `DTClass` is a sealed interface. `DTClass.getEmptyState()` and `setEntityStateFactory()` are removed from the interface and both implementations — DTClass no longer participates in construction at all.

_Alternative considered_: `DTClass.getEmptyState(Context ctx)` — pass Context into DTClass, let it dispatch. Rejected — creates a model → runner package dependency (backwards), and DTClass should not know about construction strategy.

### ContextData is deleted; fields become set-once on Context

`buildNumber`, `millisPerTick`, `gameVersion`, and `pointerCount` all share the same invariant: set exactly once, early in parsing, read-only thereafter. They get a uniform set-once guard:

```java
public void setBuildNumber(int v) {
    if (buildNumberSet) throw new IllegalStateException("buildNumber already set");
    this.buildNumber = v;
    this.buildNumberSet = true;
}
```

Engine types already inject `ctx` (`@Insert Context ctx`). They replace `contextData.setBuildNumber(n)` with `ctx.setBuildNumber(n)` directly. `ContextData` class is deleted. `EngineType.getContextData()` is removed.

### source stays in the processor list

No processor in the clarity codebase declares `@Insert Source`, but downstream consumers of the library may depend on it. Removing it would break any user processor with `@Insert Source source` silently at runtime. `source` remains in the processor list via `AbstractFileRunner.infraProcessors()`.

### AbstractRunner gains infraProcessors() and createContext() hooks

```java
// AbstractRunner:
protected List<Object> infraProcessors() { return List.of(this); }

protected Context createContext(ExecutionModel em) {
    // subclass must override if it needs EntityStateFactory fields
    throw new UnsupportedOperationException();
}

protected final void initWithProcessors(Object... userProcessors) {
    var all = new ArrayList<>(infraProcessors());
    Collections.addAll(all, userProcessors);
    var em = createExecutionModel(all);
    context = createContext(em);
    em.initialize(context);
}
```

```java
// AbstractFileRunner:
@Override
protected List<Object> infraProcessors() {
    return List.of(this, engineType, engineType.getPacketReader(), source);
}

@Override
protected Context createContext(ExecutionModel em) {
    return new Context(em, s1EntityStateType, s2EntityStateType, s2FieldPathType);
}
```

The recursive `Object[]` flattening in `addProcessorsToModel` is retained — `runWith(Object... processors)` is a public API and users may pass nested arrays. Infrastructure processors no longer rely on it (they come from the flat `List` returned by `infraProcessors()`), but it remains for user-provided processors. `getRegisteredProcessors()` on `EngineType` can be removed once `AbstractFileRunner.infraProcessors()` enumerates them directly.

_Alternative considered_: Add EntityStateFactory parameter to `initWithProcessors`. Rejected — makes AbstractRunner dependent on EntityStateFactory (wrong direction); the hook pattern keeps AbstractRunner agnostic.

### FieldLayoutBuilder stays as a private Context field

The builder caches `FieldLayout` per `Serializer` across the run. It must be fresh per run (not a static singleton) and shared across all entity state creations. Context already has run scope. It's an opaque implementation detail — not exposed via any public method.

## Risks / Trade-offs

- **BREAKING DTClass interface**: `getEmptyState()` and `setEntityStateFactory()` removal affects any downstream code that calls these directly. In practice only `Entities`, `TempEntities`, and `DTClasses.onDTClass` call them. → Accept; document in release notes.

- **Context gains more fields and methods**: Context grows from a 4-method facade to the owner of construction, configuration, and parse-time state. It remains a single coherent concept ("everything about this run") — this is appropriate growth, not bloat.

- **pointerCount default before setPointerCount is called**: S1 replays never call `setPointerCount`; `pointerCount` remains at its default (`0`). S1 entity state creation doesn't use pointerCount. The guard only fires on a second call, not on a missing call. → Acceptable for now; could add an explicit S2-only validation if needed later.

## Open Questions

_(none)_
