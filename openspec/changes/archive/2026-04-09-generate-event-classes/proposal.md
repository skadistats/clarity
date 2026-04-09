## Why

The typed-event-dispatch migration replaced reflection-based event dispatch with type-safe SAM-based dispatch. This delivered major performance gains, but introduced significant boilerplate: every event annotation now contains a nested `final class Event` with an identical constructor pattern (extracting typed Listener/Filter arrays from `EventListener` SAMs) and an identical `raise()` loop (filter check → invoke → exception handling). With 29 event annotations, this is ~600 lines of near-identical code that must be kept in sync manually.

## What Changes

- **Event inner classes become interfaces**: Each annotation's nested `Event` changes from a `final class` to an `interface` that declares only `raise(...)` and inherits `isListenedTo()` from a common base interface. This preserves the typed field declarations (`OnEntityEntered.Event evEntered`) and call sites (`evEntered.raise(entity)`) unchanged.
- **Annotation processor generates implementations**: A new `@GenerateEvent` meta-annotation triggers an AP that inspects the `Listener` (and optional `Filter`) SAM interfaces, then generates a top-level `<AnnotationName>_Event` class implementing the interface. Two strategies: `STANDARD` (default) — typed arrays + loop with optional filter; `BUCKETED` — listeners bucketed by annotation `value()` class, with wildcard support and extra `isListenedTo(Class<?>)`. Primitives in SAM signatures work naturally — no boxing, no generic type parameter limitations.
- **`parameterClasses` removed from `@UsagePointMarker`**: The Listener SAM already defines parameter types and count. The single runtime consumer (`InitializerMethod.asSpreader`) can derive the count from the Listener SAM method via `EventContractDiscovery`. This eliminates a redundant, error-prone declaration.
- **`EventContractDiscovery` extended**: Falls back to locating `<AnnotationName>_Event` on the classpath when no nested `Event` class is found inside the annotation.
- **`ExecutionModel.injectEvent()` updated**: The field type check changes from `Event.class.isAssignableFrom(fieldType)` (fails for interfaces) to checking whether the field type is nested inside an annotation.
- **Custom events remain hand-written**: Only `OnEntityPropertyChanged` omits `@GenerateEvent` and keeps its hand-written nested `Event` class (Adapter/caching pattern with per-DTClass memoization).

## Capabilities

### New Capabilities
- `event-codegen`: Annotation processor that generates Event implementation classes from Listener/Filter SAM interfaces declared on event annotations.

### Modified Capabilities

## Impact

- **clarity-event module**: `Event` base class changes (may become abstract or gain a base interface), `EventContractDiscovery` gets fallback logic, `UsagePointMarker.parameterClasses` removed, `InitializerMethod` refactored.
- **All event annotations** (~28): Nested `Event` class replaced with `Event` interface + `@GenerateEvent`. Only `OnEntityPropertyChanged` keeps its hand-written class.
- **`ExecutionModel`**: `injectEvent()` field type detection adapted for interface Event types.
- **All event-raising processors**: No change — field types remain `OnX.Event`, call sites remain `ev.raise(...)`.
- **Build system**: New annotation processor dependency/module for the AP.
- **clarity-examples and downstream users**: Transparent — the `OnX.Event` type is still the same type they reference, just now an interface.
