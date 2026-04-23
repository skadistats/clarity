## Context

The flat state introduced by `flat-entity-state` and mutated in place by `inline-field-mutation-apply` already stores primitives — there is no object-per-field layer between decode and storage on the flat impls. The read side is the last mile:

- `EntityState.getValueForFieldPath(state, fp)` returns `Object`, forcing `Integer.valueOf` / `Float.valueOf` on every call (even for flat impls that hold the primitive directly).
- `Entity.getProperty(FieldPath)` / `getProperty(String name)` funnel through the same path.
- Listener dispatch via `@OnEntityPropertyChanged<T>` boxes by declaration.

Two distinct use cases benefit from de-boxing:

1. **Consumer-driven reads** (pull): `@OnEntityUpdated` handlers, JavaFX bindings, per-tick aggregation loops. The consumer calls; clarity answers. This is the 80% case.
2. **Deferred / cross-thread reads** (capture-then-read): clarity-analyzer's FX thread needs a stable view of a state that the parse thread is about to mutate further. Today: `state.copy()` freezes everything; FX reads later. Tomorrow: capture only what changed, FX merges into a long-lived own state, reads as needed.

A pull-side primitive API serves (1) directly. A sparse `StateDelta` serves (2) without forcing the full-state allocation per update.

## Sketch

The actual type in tree is `skadistats.clarity.state.EntityState` — a sealed
interface `permits S1EntityState, S2EntityState`. Each branch is itself a
sealed abstract class with concrete impls:

- `S1EntityState` → `S1FlatEntityState`, `S1ObjectArrayEntityState`
- `S2EntityState` → `S2FlatEntityState`, `S2NestedArrayEntityState`,
  `S2NestedEntityState`, `S2TreeMapEntityState`

Every concrete impl participates in the new API. There is no "single
production implementation" shortcut — each needs its own primitive-getter
body, matching the engine-specific `FieldPath` subtype
(`S1FieldPath` / `S2FieldPath`). The engine-agnostic entry points mirror
the existing `EntityState.getValueForFieldPath(state, fp)` static
dispatcher pattern:

```java
// existing shape, for reference
public sealed interface EntityState permits S1EntityState, S2EntityState {
    static <T> T getValueForFieldPath(EntityState s, FieldPath fp) {
        return (T) switch (s) {
            case S1EntityState s1 -> s1.getValueForFieldPath((S1FieldPath) fp);
            case S2EntityState s2 -> s2.getValueForFieldPath((S2FieldPath) fp);
        };
    }
    EntityState copy();
}

// new additions
public sealed interface EntityState permits S1EntityState, S2EntityState {
    // ... existing ...

    // engine-agnostic static dispatchers (same pattern as getValueForFieldPath)
    static int   getInt  (EntityState s, FieldPath fp) { /* switch to subtype */ }
    static long  getLong (EntityState s, FieldPath fp) { /* switch to subtype */ }
    static float getFloat(EntityState s, FieldPath fp) { /* switch to subtype */ }
    static Object getObject(EntityState s, FieldPath fp) { /* switch to subtype */ }

    static StateDelta captureChanged(EntityState s, FieldPath[] fps, int num) {
        /* switch to subtype */
    }

    static void applyFrom(EntityState s, StateDelta delta, FieldPath fp) {
        /* switch to subtype */
    }

    static void applyAll(EntityState s, StateDelta delta) {
        /* switch to subtype; impl walks delta.fields() internally */
    }
}

// each sealed subtype adds the engine-specific abstract methods
public abstract sealed class S2EntityState implements EntityState /*…*/ {
    public abstract int   getInt  (S2FieldPath fp);
    public abstract long  getLong (S2FieldPath fp);
    public abstract float getFloat(S2FieldPath fp);
    public abstract Object getObject(S2FieldPath fp);
    public abstract StateDelta captureChanged(S2FieldPath[] fps, int num);
    public abstract void applyFrom(StateDelta delta, S2FieldPath fp);
    public abstract void applyAll(StateDelta delta);
}

public interface StateDelta {
    FieldPath[] fields();
    int   getInt  (FieldPath fp);   // returns 0 if fp not in set
    long  getLong (FieldPath fp);
    float getFloat(FieldPath fp);
    Object getObject(FieldPath fp); // returns null if fp not in set or not an object field
}
```

`Entity` (not `EntityState`) gets convenience delegates with a single
`FieldPath` arg; it resolves to the right engine internally via the
same mechanism it already uses for `getProperty`.

### `applyAll` vs `applyFrom`

`applyAll(StateDelta)` is the ergonomic primitive: "absorb everything this
delta covers." It walks `delta.fields()` and merges each field into the
target. This is the shape the analyzer companion change wants (`for (fp
: changed) fxState.applyFrom(delta, fp)` collapses to a single call).

`applyFrom(StateDelta, FieldPath)` stays as the lower-level primitive for
consumers who want to absorb a specific subset or interleave merges with
other work. Both are callable; `applyAll` is the expected default.

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

Chosen: on `EntityState` (as static dispatchers) plus the engine-specific
`S1EntityState` / `S2EntityState` abstract classes (as abstract methods
with typed `FieldPath` args). `Entity` gets convenience delegates. A
separate `PrimitiveReadableState` interface was considered but rejected:
the sealed hierarchy already defines the full set of impls, and splitting
the contract would just duplicate the sealing boilerplate. Every concrete
impl — `S1FlatEntityState`, `S1ObjectArrayEntityState`,
`S2FlatEntityState`, `S2NestedArrayEntityState`, `S2NestedEntityState`,
`S2TreeMapEntityState` — implements the new methods; there is no
"one production impl" shortcut.

### Allocation-free reads: flat vs. nested impls

The "no allocation at the accessor boundary" contract is what the API
promises — it does **not** promise that reading a primitive from every
impl hits a pure primitive load.

- Flat impls (`S1FlatEntityState`, `S2FlatEntityState`) store primitives
  inline in a `byte[]` with a VarHandle read → true primitive load, no
  allocation ever.
- Nested/tree impls (`S2NestedArrayEntityState`, `S2NestedEntityState`,
  `S2TreeMapEntityState`, `S1ObjectArrayEntityState`) store
  already-boxed wrapper references. `getInt` there is a navigation +
  `((Integer) ref).intValue()`. The unbox is free; the wrapper already
  exists; no new allocation at the accessor boundary. Decode-side
  allocation (the box written into the slot at decode time) is a
  property of the storage impl, not of this API.

The spec scenarios reflect this — see "Read-side contract is
storage-shape agnostic" in the capability spec.

### `getValueForFieldPath` vs. `getObject`

For object-typed fields (`Vector`, `String`, handles, etc.) these return
the same reference. `getValueForFieldPath` remains as the generic-typed
escape hatch (returns `Object`, boxes primitive fields). `getObject`
signals caller intent: "I know this is an object field." For primitive
fields, `getObject` returns the boxed value (same as
`getValueForFieldPath`), but callsites using `getObject` on a primitive
field are almost certainly a bug — code review should flag them.

### Does `StateDelta` extend `EntityState` or stand alone?

Chosen: stand alone. A delta is sparse and has no notion of "the full field set"; operations like `getFieldPathIterator` don't make sense on it. Shared primitive accessors are a coincidence of vocabulary, not a subtype relationship.

### Merge direction

Chosen: `applyFrom(state, delta, fp)` / `applyAll(state, delta)` live on `EntityState`, not `StateDelta`. The target is the mutable party; the delta is read-only after creation. Keeps `StateDelta` safely publishable across threads without synchronization.

## Risks and mitigations

- **FieldPath identity vs. equality.** `captureChanged` looks up a slot per `FieldPath` in the input array; this must use the same identity model as the live state. Clarity's `S1FieldPath` / `S2FieldPath` already have correct `equals`/`hashCode` (used by listener dispatch), so a small identity-compatible index suffices; no new invariant.
- **Mistyped access producing silent zero.** Mitigated by test coverage on the delta impl (scenario: pulling the wrong primitive type returns the default, documented behavior). The cost of supporting this is low and the alternative (exception) is worse at the consumer callsite.
- **Sealed hierarchy fan-out.** `EntityState` is sealed, as are its `S1EntityState` / `S2EntityState` branches. Adding abstract methods forces an implementation in all six concrete impls (no default-method shortcut once the method is abstract and typed). This is accepted cost — the spec contract applies uniformly across impls.
- **Binary compatibility.** `EntityState` is a clarity-internal sealed interface; no out-of-tree `EntityState` implementations can exist (the seal prevents it). Downstream consumers only need recompile.

## Alternatives considered

- **Push-side primitive dispatch (`@OnEntityIntPropertyChanged`).** Larger API surface, longer-horizon. Non-goal for this change; see proposal.
- **Caller-supplied buffer (`captureChanged(fps, num, intBuf, longBuf, floatBuf, objBuf)`).** Zero-alloc capture at the cost of ugly signature and buffer-lifecycle complexity. Rejected for now — the one-object-plus-arrays cost of `StateDelta` is tiny and consumer-side allocation can pool deltas later if needed without API change.
- **Per-type deltas (`IntDelta`, `FloatDelta`, …).** Rejected — multiplies allocation, forces consumers to know which delta to pull from, and provides no real compile-time safety since the field-type knowledge is already local to the callsite.
