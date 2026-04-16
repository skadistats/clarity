## ADDED Requirements

### Requirement: Packet-scoped snapshot-and-rollback atomicity

`Entities.processAndRunPacketEntities` SHALL provide packet-level atomicity for entity state mutations and entity existence-flag transitions via snapshot-before-decode and rollback-on-failure. The guarantee is strictly stronger than the pre-existing staged-mutation model: either all entity updates AND existence-flag flips within a packet are applied (with their events fired), or none are observable.

The snapshot mechanism applies uniformly to all `EntityState` implementations (FLAT, NESTED_ARRAY, TREE_MAP). There is NO type-specific fast/slow path branching at the `Entities` level — `entity.getState().copy()` is called on every touched entity in every packet. Cost is dictated by the concrete state's `copy()` implementation (O(1) for FLAT and NESTED_ARRAY after this change; O(N) for TREE_MAP, which is accepted — TREE_MAP is a test-bed implementation with no production users).

**Snapshot scratch (zero-allocation-per-packet).** `Entities` SHALL hold reusable snapshot scratch as instance fields, sized to `maxEntityIndex`:

- `EntityState[] snapshotScratch` — null where no snapshot; holds the pre-packet state reference otherwise
- `boolean[] existsSnapshotScratch` — captures `Entity.isExists()` alongside state
- `int[] dirtyIndices` — stack of entity indices that currently hold a snapshot
- `int dirtyTop` — size of the stack

No `HashMap` / `Int2ObjectMap` is allocated per packet. Per-packet reset is O(dirtyTop) — iterate the stack nulling `snapshotScratch[idx]`, reset `dirtyTop = 0`.

**Snapshot point.** Before any state-mutating or existence-flipping work for an entity within the current packet (update, create, recreate, enter, leave, delete), `Entities` SHALL invoke a helper equivalent to:

```java
private void snapshotAndCopy(Entity entity) {
    int idx = entity.getIndex();
    if (snapshotScratch[idx] != null) return;       // idempotent per packet
    snapshotScratch[idx] = entity.getState();
    existsSnapshotScratch[idx] = entity.isExists();
    dirtyIndices[dirtyTop++] = idx;
    entity.setState(entity.getState().copy());
}
```

Each entity index SHALL appear in the dirty stack at most once per packet — subsequent touches of the same entity SHALL NOT re-snapshot.

**Decode-direct commit.** `fieldReader.readFields` SHALL be called on the already-copied, owned-by-current-packet state. Writes happen in place on the new state. No staging buffer of mutations SHALL be produced for the commit pass on the fast path.

**Event dispatch deferral.** Events (`@OnEntityCreated`, `@OnEntityUpdated`, `@OnEntityEntered`, `@OnEntityLeft`, `@OnEntityDeleted`, `@OnEntityPropertyCountChanged`, baseline switches, alternate-baseline updates) SHALL continue to be queued into `queuedUpdates` and run only after `processPacketEntities` returns successfully. Queued lambdas carry the metadata needed to fire events (entity reference, changed `FieldPath[]`, `capacityChanged` flag) but SHALL NOT apply any state mutation — state is already committed.

**Rollback on failure.** If any `Throwable` escapes `processPacketEntities`:
- For each `idx` in `dirtyIndices[0..dirtyTop]`, the corresponding `Entity` SHALL have `setState(snapshotScratch[idx])` AND `setExists(existsSnapshotScratch[idx])` called, restoring both its pre-packet state and its pre-packet existence flag
- The `queuedUpdates` list SHALL be cleared without firing any events
- The exception SHALL be re-thrown
- No observer SHALL observe any mutation, existence change, or event from the failing packet

**Rollback on success.** On normal completion, the `finally` block iterates `dirtyIndices[0..dirtyTop]` nulling `snapshotScratch[idx]`, resets `dirtyTop = 0`, and clears `queuedUpdates`. Original state references become unreachable and GC-eligible. Events fire in queue order before the finally block, or — equivalently — the queued runnables run on the path between `processPacketEntities` returning and the finally block executing.

#### Scenario: Successful packet commits all and fires events

- **WHEN** a packet updates entities A, B, C with no decode errors
- **THEN** each entity's state is snapshotted (state + exists), then copied, then decoded into
- **AND** after `processPacketEntities` returns, `queuedUpdates` runs in order, firing `@OnEntityUpdated` for A, B, C
- **AND** the finally block clears `dirtyIndices[0..dirtyTop]` snapshot slots and resets `dirtyTop` to 0
- **AND** no allocation per packet beyond the per-entity `state.copy()` results

#### Scenario: Mid-packet decode failure rolls back all entities

- **WHEN** a packet updates entities A, B, C and `readFields` for C throws
- **THEN** entities A and B's state mutations are rolled back via `setState(originalStateA)` / `setState(originalStateB)`
- **AND** C's state (which may have been partially mutated in-place) is rolled back via `setState(originalStateC)`
- **AND** no `@OnEntityUpdated` event fires for A, B, or C
- **AND** the original exception is re-thrown

#### Scenario: Existence-flag rollback for leave/delete/create failure

- **WHEN** a packet processes `queueEntityLeave` for entity B then `queueEntityUpdate` for entity C, and the update of C throws
- **THEN** entity B's `exists` flag is restored to its pre-packet value via `setExists(true)`
- **AND** no `@OnEntityLeft` event fires for B
- **AND** the original exception is re-thrown to the caller

#### Scenario: Event observers see consistent post-packet view

- **WHEN** an `@OnEntityUpdated` handler calls `getByIndex` on another entity also updated in the same packet
- **THEN** that entity's state reflects its post-packet committed values
- **AND** never reflects a partially-decoded intermediate state

#### Scenario: Same-entity double update within packet snapshots once

- **WHEN** an entity appears twice in the same packet (rare: CREATE followed by UPDATE, or duplicate UPDATEs)
- **THEN** `snapshotAndCopy` is idempotent — the dirty stack contains exactly one entry for that entity's index
- **AND** `snapshotScratch[idx]` holds the state as it was before any packet-local mutation
- **AND** rollback restores the entity to that pre-packet state regardless of how many intra-packet mutations occurred

#### Scenario: Per-packet scratch reset is O(dirtyTop)

- **WHEN** `processAndRunPacketEntities` reaches its `finally` block
- **THEN** the reset iterates only `dirtyIndices[0..dirtyTop]`, setting those entries of `snapshotScratch` to null
- **AND** entries of `snapshotScratch` for untouched entity indices are not visited
- **AND** `existsSnapshotScratch` does not require clearing (boolean primitives are overwritten on next snapshot)
- **AND** `dirtyTop` is set to 0

### Requirement: Snapshot and copy() interaction

The snapshot-and-rollback mechanism SHALL rely on `EntityState.copy()` being O(1) for `FlatEntityState` and `NestedArrayEntityState` (per the `flat-entity-state` and `nested-entity-state` capabilities). For `TreeMapEntityState`, `copy()` remains O(N-populated-fields); the per-packet snapshot cost scales with entity count × per-copy cost. This is accepted — TREE_MAP is a test-bed implementation with no production consumers. Users who opt into TREE_MAP deliberately (via `withS2EntityState(TREE_MAP)`) accept this cost.

`setState(snapshot)` on `Entity` SHALL replace the state reference atomically. Any caches on the `Entity` object that reference state-derived data (pointer-serializer lookups, field-path name caches, etc.) SHALL either be invalidated on `setState` or be shape-invariant across `setState`. Implementation MUST audit `Entity` for such caches and either document their invariance or add explicit invalidation hooks, before the rollback path ships.

`setExists(boolean)` on `Entity` SHALL replace the existence flag atomically. It is a rollback-oriented helper; it does NOT fire the `@OnEntityEntered` / `@OnEntityLeft` events — those remain the responsibility of `queueEntityEnter` / `queueEntityLeave`'s queued lambdas.

#### Scenario: Rollback is cheap when nothing was written

- **WHEN** an entity is snapshotted and copied, but `readFields` throws before any write lands
- **THEN** the copy's internal containers are still shared with the snapshot (no owner-pointer clones triggered)
- **AND** `setState(snapshot)` restores the original state reference with no additional work
- **AND** the copy becomes unreachable and GC-eligible

#### Scenario: Rollback is correct when writes landed before failure

- **WHEN** an entity is snapshotted and copied, `readFields` writes several fields, then throws
- **THEN** the copy contains cloned internal byte[]/slab state reflecting the partial writes
- **AND** `setState(snapshot)` restores the original pre-packet state
- **AND** the partially-written copy is unreachable and GC-eligible
- **AND** no observer sees the partial writes

### Requirement: Entity-update queue carries event metadata only

`queuedUpdates` in `Entities` SHALL carry only event-firing lambdas on the hot path. It SHALL NOT carry lambdas that apply state mutations or existence flips — those happen eagerly at `queueEntity*` time, on the already-snapshotted-and-copied state.

For `queueEntityUpdate`, the lambda stored in `queuedUpdates` SHALL be equivalent to:

```java
() -> {
    logModification("UPDATE", entity);
    if (!silent) {
        if (changes.capacityChanged()) emitPropertyCountChangedEvent(entity);
        emitUpdatedEvent(entity, changes.getFieldPaths());
    }
}
```

— no `applyUpdateChanges` call, because the state is already mutated at the time `queueEntityUpdate` returns.

For `queueEntityCreate`, `queueEntityRecreate`, `queueEntityEnter`, `queueEntityLeave`, `queueEntityDelete`: similar — the state-mutating work and `Entity.exists` flip SHALL be done eagerly at queue time (on the already-snapshotted-and-copied state), and the queued lambda SHALL only fire the associated events.

Baseline switches (`baselineRegistry.switchEntityBaselines`) and alternate-baseline updates SHALL continue to be queued in `queuedUpdates` as today — they do not participate in the per-entity snapshot mechanism because they don't mutate individual entity states.

#### Scenario: Queued update fires only events

- **WHEN** a queued update Runnable executes after successful packet processing
- **THEN** it emits `@OnEntityUpdated` and optionally `@OnEntityPropertyCountChanged` events
- **AND** it does NOT call `applyUpdateChanges` or `state.applyMutation`
- **AND** it does NOT read from any staging buffer

#### Scenario: queuedUpdates cleared on rollback

- **WHEN** a packet rollback occurs
- **THEN** `queuedUpdates` is cleared in the `finally` block
- **AND** no queued lambda runs
- **AND** no `@OnEntityUpdated` / `@OnEntityPropertyCountChanged` / `@OnEntityLeft` / `@OnEntityEntered` / `@OnEntityDeleted` / `@OnEntityCreated` event fires for any entity in the failed packet

### Requirement: MutationListener contract for decode-direct path

The internal `MutationListener` interface (consumed primarily by trace-capture infrastructure such as `MutationRecorder`) SHALL accommodate the decode-direct path where no `StateMutation.WriteValue` record is materialized.

When `S2FieldReader.readFieldsFast` uses `flatState.decodeInto(fp, decoder, bs)` for a primitive write, the listener SHALL be invoked via a `onUpdateWrite(EntityState state, FieldPath fp)` callback *after* the decode completes. Listeners needing the written value SHALL read it from `state.getValueForFieldPath(fp)` — this reconstructs a boxed value from the underlying byte[] and is paid only when a listener is attached.

For structural mutations (`SwitchPointer`, `ResizeVector`) and for reference-typed writes (String) on the FLAT path, and for all mutations on non-FLAT paths, the listener SHALL continue to be invoked via `onUpdateMutation(EntityState state, FieldPath fp, StateMutation mutation)` with the materialized mutation as today.

The existing `onSetupMutation(EntityState state, FieldPath fp, StateMutation mutation)` contract for setup-path mutations (baseline materialization, entity creation) is unchanged — setup mutations always carry a materialized `StateMutation`.

Trace-capture implementations SHALL be updated to implement `onUpdateWrite` and to retrieve the written value via `getValueForFieldPath` when needed.

#### Scenario: onUpdateWrite after primitive decode-direct

- **WHEN** `flatState.decodeInto(fp, intDecoder, bs)` completes successfully with a `MutationListener` attached
- **THEN** `listener.onUpdateWrite(flatState, fp)` is invoked after the write
- **AND** if the listener needs the value, it calls `flatState.getValueForFieldPath(fp)` to retrieve the boxed `Integer`

#### Scenario: onUpdateMutation for structural mutations

- **WHEN** a `ResizeVector` or `SwitchPointer` is produced during `readFieldsFast` on a FLAT state
- **THEN** the listener is invoked via `listener.onUpdateMutation(state, fp, mutation)` with the materialized `StateMutation` before `state.applyMutation` is called

#### Scenario: onUpdateMutation for non-FLAT states

- **WHEN** `readFieldsFast` processes a field on a non-FLAT state (NESTED_ARRAY, TREE_MAP)
- **THEN** the listener is invoked via `listener.onUpdateMutation(state, fp, mutation)` as before
- **AND** `onUpdateWrite` is NOT invoked for that field
