## Context

The flat state introduced by `flat-entity-state` and mutated in place by `inline-field-mutation-apply` already stores primitives — there is no object-per-field layer between decode and storage. The read side is the last mile:

- `State.getValueForFieldPath(FieldPath)` returns `Object`, forcing `Integer.valueOf` / `Float.valueOf` on every call.
- `Entity.getProperty(FieldPath)` / `getProperty(String name)` funnel through the same path.
- Listener dispatch via `@OnEntityPropertyChanged<T>` boxes by declaration.

Two distinct use cases benefit from de-boxing:

1. **Consumer-driven reads** (pull): `@OnEntityUpdated` handlers, JavaFX bindings, per-tick aggregation loops. The consumer calls; clarity answers. This is the 80% case.
2. **Deferred / cross-thread reads** (capture-then-read): clarity-analyzer's FX thread needs a stable view of a state that the parse thread is about to mutate further. Today: `state.copy()` freezes everything; FX reads later. Tomorrow: capture only what changed, FX merges into a long-lived own state, reads as needed.

A pull-side primitive API serves (1) directly. A sparse `StateDelta` serves (2) without forcing the full-state allocation per update.

## Sketch

```java
// clarity-core/model/state/
public interface State {
    // existing ...
    Object getValueForFieldPath(FieldPath fp);        // unchanged; now layered

    // new primitive getters
    int   getInt  (FieldPath fp);
    long  getLong (FieldPath fp);
    float getFloat(FieldPath fp);
    Object getObject(FieldPath fp);                   // Vector, String, handle, etc.

    // new sparse capture
    StateDelta captureChanged(FieldPath[] fps, int num);

    // new in-place merge
    void applyFrom(StateDelta delta, FieldPath fp);
}

public interface StateDelta {
    FieldPath[] fields();
    int   getInt  (FieldPath fp);   // returns 0 if fp not in set
    long  getLong (FieldPath fp);
    float getFloat(FieldPath fp);
    Object getObject(FieldPath fp); // returns null if fp not in set or not an object field
}
```

The concrete `StateDelta` implementation is sized to `num`. Its backing storage is shaped to match the source state it was captured from:

- **Captured from flat state**: three parallel primitive arrays (`int[]`, `long[]`, `float[]`) plus an `Object[]` for non-scalar fields (vectors, strings, handles). Primitive reads on the delta are direct array loads; `applyFrom` onto a flat target copies primitives directly.
- **Captured from nested state**: an `Object[]` carrying the already-boxed wrapper references straight from the nested leaves, plus type tagging per slot. Primitive reads on the delta unbox on demand (allocation-free); `applyFrom` onto a nested target writes the stored reference back — reusing the source wrapper, so no reboxing.

In both cases `StateDelta` carries an index table mapping each input `FieldPath` position to its slot. Allocation cost: one object plus arrays of size proportional to changed-field count. No new wrappers are introduced that the source state didn't already hold.

`captureChanged` dispatches by source storage shape: on flat state it reads the primitive directly; on nested state it reads the leaf's existing wrapper reference. `applyFrom` dispatches by target storage shape: on flat targets it writes primitives, on nested targets it writes wrapper references. A mixed pipeline (flat→nested or nested→flat) would force a single box-or-unbox on merge, but in practice the source and target always share a shape (the same process' state impl), so the mixed case is pathological and tolerated rather than optimized.

## Decisions

### Single `StateDelta` type or per-primitive variants?

Chosen: single `StateDelta` with all four slot types internally. A caller pulling `getInt(fp)` on a field that happens to be a `float` gets `0` and this is documented as "caller responsibility to know the field type." Rationale: the alternative (four typed deltas) multiplies allocation pressure and callsite verbosity; consumers always know the type at the callsite because they wrote the binding. Matches the existing `getValueForFieldPath` contract: caller knows the type.

### Default on unknown field paths: zero/null vs. exception

Chosen: zero/null. Throwing would force consumers to wrap every access in a contains-check, and the realistic caller (analyzer, bindings) only ever queries paths it put into `captureChanged` or knows are live on the state. An exception here would be defensive plumbing, not a safety feature. Matches `feedback_no_defensive_checks_hot_path`.

### Where do the primitive getters live?

Chosen: on `State` directly. `Entity` gets one-line delegates. Putting the methods on a separate interface (`PrimitiveReadableState`) was considered but rejected — there is exactly one production implementation, and all other state-ish things (copies, test doubles) implement the same contract.

### Does `StateDelta` extend `State` or stand alone?

Chosen: stand alone. A delta is sparse and has no notion of "the full field set"; operations like `getFieldPathIterator` don't make sense on it. Shared primitive accessors are a coincidence of vocabulary, not a subtype relationship.

### Merge direction

Chosen: `State.applyFrom(delta, fp)` lives on `State`, not `StateDelta`. The target is the mutable party; the delta is read-only after creation. Keeps `StateDelta` safely publishable across threads without synchronization.

## Risks and mitigations

- **FieldPath identity vs. equality.** `captureChanged` looks up a slot per `FieldPath` in the input array; this must use the same identity model as the live state. Clarity's `FieldPath` already has correct `equals`/`hashCode` (used by listener dispatch), so a small identity-compatible index suffices; no new invariant.
- **Mistyped access producing silent zero.** Mitigated by test coverage on the delta impl (scenario: pulling the wrong primitive type returns the default, documented behavior). The cost of supporting this is low and the alternative (exception) is worse at the consumer callsite.
- **Binary compatibility.** `State` is an interface implemented only inside clarity; adding default methods on it is a recompile of downstream consumers. No `analyzer`-side `State` implementations exist. Confirmed via memory note on analyzer structure.

## Alternatives considered

- **Push-side primitive dispatch (`@OnEntityIntPropertyChanged`).** Larger API surface, longer-horizon. Non-goal for this change; see proposal.
- **Caller-supplied buffer (`captureChanged(fps, num, intBuf, longBuf, floatBuf, objBuf)`).** Zero-alloc capture at the cost of ugly signature and buffer-lifecycle complexity. Rejected for now — the one-object-plus-arrays cost of `StateDelta` is tiny and consumer-side allocation can pool deltas later if needed without API change.
- **Per-type deltas (`IntDelta`, `FloatDelta`, …).** Rejected — multiplies allocation, forces consumers to know which delta to pull from, and provides no real compile-time safety since the field-type knowledge is already local to the callsite.
