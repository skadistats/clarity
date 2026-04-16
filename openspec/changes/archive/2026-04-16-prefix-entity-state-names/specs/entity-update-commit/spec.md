## MODIFIED Requirements

### Requirement: Eager in-place state mutation during packet processing

`Entities.processPacketEntities` SHALL dispatch decoded field mutations directly to the target entity's state at queue time. Field readers (`S2FieldReader.readFieldsFast`, `S1FieldReader.readFields`) write through `EntityState.decodeInto(fp, decoder, bs)` (zero-box, primitive-leaf path) or `EntityState.write(fp, decoded)` (structural-leaf path) into the entity's live `EntityState` instance. No `StateMutation` record is produced on the fast path; no staging buffer of mutations is maintained for later application.

Queued `Runnable`s in `queuedUpdates` SHALL carry only event-firing work — they observe state as already-committed when they run. For `queueEntityUpdate`, the queued lambda emits `@OnEntityUpdated` (and optionally `@OnEntityPropertyCountChanged`) using the `FieldChanges.capacityChanged` flag and `FieldChanges.getFieldPaths()` captured at decode time. Structural lifecycle work (`queueEntityCreate`, `queueEntityRecreate`, `queueEntityEnter`, `queueEntityLeave`, `queueEntityDelete`) continues to run inside its queued lambda — state-visible flips (`Entity.setActive`, `Entity.setExistent`, `ClientFrame.setEntity`, baseline registrations) happen there, just as they did before.

For `queueEntityCreate` and `queueEntityRecreate`, the caller SHALL copy the per-DTClass baseline state via `copyState(baseline)` BEFORE invoking `fieldReader.readFields`. The reader then mutates that copy in place. This preserves the invariant that the shared baseline state is never modified.

#### Scenario: S2 fast path — primitive field writes straight to state

- **WHEN** `S2FieldReader.readFieldsFast` processes a primitive-leaf field on a `S2FlatEntityState`
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
