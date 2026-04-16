## Why

The S2 hot path has two entangled cost centers: primitive mutations autobox twice (`Integer`/`Float`/`Long` in, unbox on write), and a staging buffer holds every mutation as a `StateMutation` record between decode and commit. For a Hero entity that is N× autobox + N× record allocation per packet.

After this change: `S2FieldReader.readFieldsFast` writes decoded values straight to state via `state.decodeInto(fp, decoder, bs)` (zero-box primitive path) or `state.write(fp, decoded)` (structural-leaf path). No `StateMutation` allocation on the S2 hot path. Strings move inline into the composite byte[], eliminating their refs-slab entry. Owner-pointer COW was introduced in CP-1/CP-2 to make per-packet state snapshots cheap, but CP-6 implementation revealed that the decode-direct model does not actually need per-packet snapshots (see design pivot note in tasks.md §6) — throw-aborts-replay is the effective atomicity contract. The COW machinery is retained for now; a follow-up change (`lightspeed-eager-copy`) will strip it.

**Measured result** (Dota 2 replay, FLAT impl): **17.2% faster** than pre-change baseline (1988 → 1646 ms). FLAT is now the fastest impl across CS2, Deadlock, and Dota 2 benches, overtaking NESTED_ARRAY.

**S1 is deferred to a follow-up change** (`accelerate-s1-flat-state`). The S1 port will land after `lightspeed-eager-copy` so it is built directly on the eager-copy model rather than temporarily adopting COW only to have it stripped immediately after.

## What Changes

### Unified direct-write discipline (S2)
- **Add `EntityState.write(FieldPath fp, Object decoded)`** — single direct-write method. The state's own leaf traversal dispatches on layout shape: Primitive → byte[] write; Ref → refs slab write; structural (SubState-Pointer/Vector) → pointer-switch or vector-resize behavior, derived from layout + decoded-value shape. No `StateMutation` wrapper.
- **Add `EntityState.decodeInto(FieldPath fp, Decoder decoder, BitStream bs)`** — zero-box variant for primitive leaves on FLAT-family states. Returns `capacityChanged`.
- **`StateMutation` class survives only for**: `readFieldsDebug` inspection tooling, `MutationRecorder` trace capture, baseline materialization, and any programmatic/setup writes outside the decoder. It is **never** produced on the `S2FieldReader.readFieldsFast` hot path.
- **`S2FieldReader.readFieldsFast` writes state directly.** No staging, no `StateMutation` wrapping, no `pointerOverrides[]` shadow map — subsequent `resolveField` calls within a packet read from `state.pointerSerializers[]` directly (state is already mutated).

### FLAT: owner-pointer COW + decodeInto + inline strings
- **Owner-pointer COW on every container**: `Entry.owner`, `refsOwner` (governing both `refs` and `freeSlots`), `pointerSerializersOwner`. `copy()` becomes O(1). First write checks owner, clones only what it touches. (Follow-up `lightspeed-eager-copy` strips this.)
- **Remove `markSubEntriesNonModifiable`** and all `modifiable`/`refsModifiable`/`pointerSerializersModifiable` flag machinery.
- **Switch `refs` from `ArrayList<Object>` to `Object[]` + `int refsSize`**; `freeSlots` from `Deque<Integer>` to `int[]` + `int freeSlotsTop`. Clone via `Arrays.copyOf`.
- **Strings move inline into byte[]**. `FieldLayout.InlineString(offset, maxLength)` reserves `3 + maxLength` bytes at the leaf's offset (flag byte + 2-byte LE length prefix + UTF-8 bytes). `char[N]` props use the declared `N`; unbounded strings (S2 `CUtlString`) use a uniform `maxLength = 512`, grounded in the `StringLenDecoder`'s intrinsic 9-bit wire cap (511 bytes). String decoders get `decodeIntoInline` that writes length-prefix + UTF-8 bytes directly. `refs` slab thereafter holds only sub-Entry instances.
- **Primitive decoders get `decodeInto(BitStream, byte[], int offset)`** via static dispatch (mirrors existing `DecoderDispatch.decode` shape). `DecoderDispatch.decodeInto(...)` is generated.
- **`FlatEntityState.decodeInto(fp, decoder, bs)`** — traverses to the leaf, invokes `DecoderDispatch.decodeInto` at the resolved offset. For InlineString leaves routes to the inline-string decoder variant. Zero boxing, zero record allocation.

### NESTED_ARRAY: owner-pointer COW + write
- Port the owner-pointer pattern to `NestedArrayEntityState`. `copy()` becomes O(1) — shares `entries` list, `freeEntries` deque, and each `Entry` by reference. First slab-mutating write clones `entries` + `freeEntries` together; first write to an `Entry` clones its wrapper + state array. (Follow-up `lightspeed-eager-copy` strips this.)
- NESTED_ARRAY does **not** get `decodeInto` — its slots are `Object[]` so primitives still box.
- NESTED_ARRAY implements `state.write(fp, decoded)` — dispatches internally on leaf shape, no `StateMutation` needed.

### TREE_MAP: accept the per-copy cost
- `TreeMapEntityState` retains its monolithic backing map and its O(N-populated-fields) `copy()`. Accepted — TREE_MAP is a test-bed implementation with no production consumers.
- TREE_MAP also implements `state.write(fp, decoded)`.

### Commit path (decode-direct, no snapshot)
- **`S2FieldReader.readFieldsFast` mutates state in place during decode.** `S2FieldReader.readFieldsDebug` is unchanged (retains staging for inspection).
- **`queueEntityUpdate` has no snapshot/rollback.** State is mutated eagerly during decode. On `readFields` throw, `processAndRunPacketEntities` clears `queuedUpdates` in its `finally` block and propagates the exception — the replay run is expected to abort.
- **Baseline states are copied up front** by `queueEntityCreate` / `queueEntityRecreate` (`newState = copyState(baseline)`) before `readFields` mutates them. Shared per-DTClass baselines are never modified.
- **`FieldChanges.mutations[]` removed from the fast path** entirely. `FieldChanges` carries `fieldPaths[]` and `capacityChanged`. The `mutations[]` field survives on the debug path and baseline materialization.
- **Event dispatch queue** carries event-firing lambdas only; state is already committed when lambdas run.

### Hot-loop micro-structure
- **`isFlat` hoisted out of `S2FieldReader.readFieldsFast`** — one `instanceof FlatEntityState` at the top of the method, reused for every field.
- **`field.isPrimitiveLeaf()`** is the single per-field check that routes decodeInto vs write (ValueField → true; PointerField / VectorField → false).
- **`field.prepareForWrite(decoded, depth)`** transforms the raw decoded value for `state.write` (PointerField resolves `Pointer → Serializer`; VectorField validates the Integer count).

### Listener contract
- **`MutationListener.onUpdateWrite(state, fp)`** — new callback for decode-direct writes. Listeners that need the value re-read via `state.getValueForFieldPath(fp)`. `onUpdateMutation(state, fp, mut)` continues for debug/trace paths and any path that produces a materialized `StateMutation`.
- `MutationRecorder` trace capture updated to implement `onUpdateWrite`.

### Non-goals
- Public `EntityState` interface shape unchanged for consumer code (`Entity.getProperty`, `@OnEntityUpdated`, etc.). `write` / `decodeInto` are added; nothing user-visible is removed or renamed.
- Debug path (`readFieldsDebug`, `dtinspector`) continues to use staging buffer for full fidelity.
- No COW-capable TreeMap in this change.
- **S1 is out of scope.** `ObjectArrayEntityState`, `S1FieldReader`, and S1 decoders are unchanged. A follow-up change will handle them.
- Stripping the owner-pointer COW machinery is out of scope — follow-up `lightspeed-eager-copy`.

## Capabilities

### New Capabilities
- `entity-update-commit`: Eager in-place state mutation during packet processing. Throw during processing aborts the replay run; queued updates cleared; no state rollback.

### Modified Capabilities
- `flat-entity-state`: O(1) `copy()` via owner-pointer. Adds `decodeInto` (primitive + inline-string) and `write` (single direct-write method). `refs` becomes `Object[]` + `int[] freeSlots`. Strings move inline into the composite byte[] (refs slab holds only sub-Entries thereafter).
- `nested-entity-state`: O(1) `copy()` via owner-pointer on slab + per-Entry. `boolean modifiable` flag removed. Implements `write(fp, decoded)` for direct dispatch without StateMutation wrapping. State mutates in place during `readFields`.
- `state-mutation`: `StateMutation` is **not** produced by `S2FieldReader.readFieldsFast`. Every primitive-leaf write on FLAT goes through `state.decodeInto`; every other write goes through `state.write`. The class survives for `readFieldsDebug`, `MutationRecorder`, baseline materialization, and programmatic/setup writes.

## Impact

- `skadistats.clarity.model.state`:
  - `FlatEntityState` — `copy()` rewritten; owner pointers replace flags; `refs` → `Object[]`+`int[]`; `decodeInto` and `write` added; String handling moves to inline layout.
  - `NestedArrayEntityState` — `copy()` rewritten; owner pointers replace flags; `write` added.
  - `TreeMapEntityState` — `write` added (delegates to existing dispatch; no COW change).
  - `AbstractS2EntityState` — `pointerSerializersOwner` COW.
  - `ObjectArrayEntityState` — `write` added (delegates to existing slot assignment). Other S1 work deferred.
  - `FieldLayout` — `InlineString(offset, maxLength)` leaf shape added.
  - `EntityState` interface — `write` and `decodeInto` methods added; `applyMutation` retained for non-reader callers.
- `skadistats.clarity.io.decoder`:
  - Primitive decoders gain static `decodeInto(BitStream, byte[], int offset)`.
  - `StringLenDecoder` / `StringZeroTerminatedDecoder` gain `decodeIntoInline` writing length-prefix + UTF-8 bytes.
  - `DecoderDispatch` gains generated `decodeInto` switch.
- `skadistats.clarity.io.s2.Field`: `isPrimitiveLeaf()` and `prepareForWrite(decoded, depth)` hooks added.
- `skadistats.clarity.io.s2.field.*`: `ValueField.isPrimitiveLeaf() = true`; `PointerField.prepareForWrite` resolves serializer; `VectorField.prepareForWrite` validates count.
- `skadistats.clarity.io.s2.S2FieldReader`:
  - `readFieldsFast` — `isFlat` hoisted; per-field `isPrimitiveLeaf()` check routes to `decodeInto` or `state.write`. No `pointerOverrides[]`. No staging.
  - `readFieldsDebug` — unchanged (retains staging; uses split `resolveFieldDebug` that reads the `pointerOverrides[]` shadow).
- `skadistats.clarity.io.FieldChanges` — `capacityChanged` boolean added; fast-path constructor leaves `mutations == null`; `applyTo` short-circuits when `mutations == null`.
- `skadistats.clarity.io.MutationListener` — `onUpdateWrite(state, fp)` added.
- `skadistats.clarity.processor.entities.Entities`:
  - `queueEntityUpdate` mutates state in place (no snapshot).
  - `queueEntityCreate` / `queueEntityRecreate` allocate `newState = copyState(baseline)` before `readFields`.
  - `processAndRunPacketEntities` clears `queuedUpdates` on throw; no rollback.
- `skadistats.clarity.bench.trace.MutationRecorder` — implements `onUpdateWrite`, reads back via `getValueForFieldPath`.
- No user-visible API changes for example/consumer code.
