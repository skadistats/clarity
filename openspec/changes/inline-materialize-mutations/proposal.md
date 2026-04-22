## Why

`S2FieldReader` maintains two resolver methods — `resolveField` (fast path) and `resolveFieldDebug` (materialize/debug path) — plus a `pointerOverrides[]` scratch array. The dual-resolver exists solely because `readFieldsMaterialized` defers writing to state, which means same-packet polymorphic pointer switches aren't visible during field resolution. Applying mutations inline eliminates that deferral and collapses the two code paths into one.

## What Changes

- `readFieldsMaterialized` applies each mutation to state immediately after decoding, then calls the `MutationListener` callback inline — no separate apply phase
- `resolveFieldDebug` is removed; all paths use `resolveField`
- `pointerOverrides[]` array and all associated logic is removed
- `FieldChanges.applyTo(state, callback)` is no longer called by `readFieldsMaterialized`; setup/update apply helpers in `Entities` become dead code for the materialize branch and can be simplified

## Capabilities

### New Capabilities
- `inline-field-mutation-apply`: Mutations in the materialize and debug read paths are applied to state as they are decoded, keeping state current throughout the field-path loop.

### Modified Capabilities

## Impact

- `skadistats.clarity.io.s2.S2FieldReader` — primary change site
- `skadistats.clarity.processor.entities.Entities` — `applySetupChanges` / `applyUpdateChanges` paths for materialize mode may simplify
- `skadistats.clarity.io.FieldChanges` — `applyTo(state, callback)` may become unused
- No API changes visible to library consumers; `MutationListener` interface is unchanged
