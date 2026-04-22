## Context

`S2FieldReader.readFieldsMaterialized` currently splits its work into two phases:

1. **Read phase**: decode all fields, build `StateMutation[]`, track `pointerOverrides[]` for within-packet polymorphic pointer switches
2. **Apply phase**: `FieldChanges.applyTo(state, callback)` applies mutations and calls `MutationListener`

The split was introduced so `MutationListener` could receive `(fp, mutation)` pairs at apply-time. But it creates a problem: pointer switches in the read phase aren't reflected in state, requiring a separate `resolveFieldDebug` that consults `pointerOverrides[]` instead of `state.getPointerSerializer()`.

The fast path (`readFieldsFast`) has no such split — it writes immediately and only needs `resolveField`.

## Goals / Non-Goals

**Goals:**
- Remove `resolveFieldDebug` and `pointerOverrides[]`
- Unify both code paths to use `resolveField`
- Keep `MutationListener` interface unchanged
- Keep benchmark trace capture working correctly

**Non-Goals:**
- Changing the `MutationListener` API
- Changing `FieldChanges` structure (field paths still needed for event emission)
- Performance optimization (this is a cleanup)

## Decisions

### Apply mutations inline during the read loop

In `readFieldsMaterialized`, after decoding each field:
```
mutation = field.createMutation(decoded, depth)
mutation.applyTo(state, fp)           // write immediately — state stays current
listener.onMutation(state, fp, mutation)  // notify inline with the typed object
```

This preserves the `StateMutation` object (needed for bench replay) while keeping state current, so `resolveField` works throughout.

**Alternative considered**: Apply first, reconstruct `StateMutation` from state. Rejected because `SwitchPolymorphicPointer` carries a `Serializer` reference not otherwise recoverable from state.

**Alternative considered**: Keep collect-then-apply but also track pointer switches in state eagerly. Rejected — same complexity, no cleanup gain.

### `readFieldsDebug` follows the same pattern

Debug path also uses `resolveFieldDebug` today. Switch it to the same inline approach so it too uses `resolveField`.

### `FieldChanges.applyTo(state, callback)` becomes unused from `Entities`

With inline application, `applySetupChanges` and `applyUpdateChanges` in `Entities` no longer need to call `applyTo` for the materialize branch. The field paths in `FieldChanges` are still returned and used for `emitUpdatedEvent`. The `applyTo(state, callback)` overload can be removed if nothing else calls it.

## Risks / Trade-offs

**`FieldChanges` capacityChanged flag** — `readFieldsFast` sets `capacityChanged` from `state.write()` return values. `readFieldsMaterialized` currently sets it in `applyTo`. With inline apply, `readFieldsMaterialized` must also accumulate `capacityChanged` from inline writes, then include it in the returned `FieldChanges`. → Check the two-arg `FieldChanges` constructor: it currently hardcodes `capacityChanged = false`. Need to plumb the flag through.

**Debug path still materializes** — `readFieldsDebug` currently also needs to track pointer switches. Inline apply fixes this for free.

**No rollback needed** — this is internal to `S2FieldReader`; no external API changes.
