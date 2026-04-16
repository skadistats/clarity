## Purpose

Defines the contract for committing decoded field changes to entity state during packet processing. Field readers mutate entity state eagerly and in place; queued lambdas observe already-committed state and emit lifecycle events. Aborts on decode failure rather than rolling back.

## Requirements

### Requirement: Eager in-place state mutation during packet processing

`Entities.processPacketEntities` SHALL dispatch decoded field mutations directly to the target entity's state at queue time. Field readers (`S2FieldReader.readFieldsFast`, `S1FieldReader.readFields`) write through `EntityState.decodeInto(fp, decoder, bs)` (zero-box, primitive-leaf path) or `EntityState.write(fp, decoded)` (structural-leaf path) into the entity's live `EntityState` instance. No `StateMutation` record is produced on the fast path; no staging buffer of mutations is maintained for later application.

Queued `Runnable`s in `queuedUpdates` SHALL carry only event-firing work — they observe state as already-committed when they run. For `queueEntityUpdate`, the queued lambda emits `@OnEntityUpdated` (and optionally `@OnEntityPropertyCountChanged`) using the `FieldChanges.capacityChanged` flag and `FieldChanges.getFieldPaths()` captured at decode time. Structural lifecycle work (`queueEntityCreate`, `queueEntityRecreate`, `queueEntityEnter`, `queueEntityLeave`, `queueEntityDelete`) continues to run inside its queued lambda — state-visible flips (`Entity.setActive`, `Entity.setExistent`, `ClientFrame.setEntity`, baseline registrations) happen there, just as they did before.

For `queueEntityCreate` and `queueEntityRecreate`, the caller SHALL copy the per-DTClass baseline state via `copyState(baseline)` BEFORE invoking `fieldReader.readFields`. The reader then mutates that copy in place. This preserves the invariant that the shared baseline state is never modified.

#### Scenario: S2 fast path — primitive field writes straight to state

- **WHEN** `S2FieldReader.readFieldsFast` processes a primitive-leaf field on a `FlatEntityState`
- **THEN** `flatState.decodeInto(fp, decoder, bs)` is invoked, writing the decoded bytes straight into the state's composite byte[]
- **AND** no `StateMutation`, `WriteValue`, or autoboxed primitive wrapper is allocated

#### Scenario: S2 fast path — structural and non-primitive writes via state.write

- **WHEN** `S2FieldReader.readFieldsFast` processes a PointerField, VectorField, or non-primitive leaf
- **THEN** the reader decodes via `DecoderDispatch.decode`, transforms as needed via `field.prepareForWrite(decoded, depth)`, and calls `state.write(fp, decoded)`
- **AND** the state's own leaf traversal dispatches on layout shape to do the pointer-switch / vector-resize / ref-slot write — no `StateMutation` wrapper

#### Scenario: Create and recreate decode into a fresh baseline copy

- **WHEN** `queueEntityCreate` or `queueEntityRecreate` runs
- **THEN** it allocates `newState = copyState(baseline)` before `fieldReader.readFields`
- **AND** `fieldReader.readFields` mutates `newState` in place; the shared per-DTClass baseline is not modified
- **AND** the queued lambda attaches `newState` to the entity via `entity.setState(newState)`

### Requirement: Throw during packet processing aborts the replay run

If any `Throwable` escapes `processPacketEntities` or a queued update lambda, `processAndRunPacketEntities` SHALL clear `queuedUpdates` in its `finally` block and rethrow. Entity state mutations that already landed before the throw REMAIN in place — they are not rolled back. The `Entities` class does not retain pre-packet snapshots and does not implement rollback.

This design relies on the invariant that Clarity aborts the replay run on the first decode failure: no caller of `processAndRunPacketEntities` catches a throw and attempts to resume. In particular, the deferred-message mechanism at `Entities.onPacketEntities` does NOT re-deliver a failed packet — it defers packets strictly on the `entitiesServerTick < message.getDeltaFrom()` condition evaluated BEFORE any decode, so deferred-then-replayed packets have their delta-from prerequisite met and are expected to decode correctly.

Observable guarantees on throw:
- `queuedUpdates` is cleared; no queued events fire
- `entitiesServerTick` is NOT advanced (the assignment at the start of `processAndRunPacketEntities` runs only after `processPacketEntities` returns cleanly)
- No explicit state rollback; the replay run is expected to abort in response to the exception

Pre-existing non-rollbackable eager mutations in the packet loop (`eEnt.setActive(...)` for PVS bits at `processPacketEntities`, `baselineRegistry.updateEntityAlternateBaselineIndex` for alternate baselines) continue to be non-rollbackable. They are not in scope for this change.

#### Scenario: Decode failure clears queue and rethrows

- **WHEN** `fieldReader.readFields` throws during `processPacketEntities`
- **THEN** the `finally` block in `processAndRunPacketEntities` clears `queuedUpdates`
- **AND** the exception propagates to the caller
- **AND** `entitiesServerTick` is not advanced
- **AND** no `@OnEntity*` event fires for the failing packet

#### Scenario: Partial state after throw is acceptable

- **WHEN** a packet has already mutated entity A's state before entity C's decode throws
- **THEN** entity A's state reflects the partial packet-local mutations
- **AND** no event fires for A (the queued lambda never runs)
- **AND** the caller is expected to abort the replay run; continuing to parse subsequent packets is outside the contract
