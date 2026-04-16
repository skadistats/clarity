## Why

The S2 hot path has three entangled cost centers: `FlatEntityState.copy()` walks the FieldLayout tree (O(layout-node-count)), primitive mutations autobox twice (`Integer`/`Float`/`Long` in, unbox on write), and a staging buffer holds every mutation as a `StateMutation` record between decode and commit. For a Hero entity that is hundreds of node visits per frame-copy plus N× autobox + N× record allocation per packet — all on the hot path.

The three are linked: eliminating autoboxing requires decode to write straight into state `byte[]`, which requires an atomic rollback mechanism, which requires `copy()` to be cheap enough to snapshot every entity touched by a packet. `NestedArrayEntityState` has the same problem at the slab level (N `Entry` wrappers per copy), and `ObjectArrayEntityState` (the Source 1 state) is worse still — eager O(N) `System.arraycopy` per copy. The S1 reader also routes every decoded value through `new WriteValue(...)` into the staging buffer.

After this change: every field reader (S1 and S2) writes decoded values directly to state via a single `state.write(fp, decoded)` (or zero-box `state.decodeInto(fp, decoder, bs)` for primitives). No `StateMutation` allocation on the hot path anywhere. Copies are O(1) for all first-class state types. Strings move inline into the composite byte[], eliminating their entry in the refs slab.

## What Changes

### Unified direct-write discipline
- **Add `EntityState.write(FieldPath fp, Object decoded)`** — single direct-write method. The state's own leaf traversal dispatches on layout shape: Primitive → byte[] write; Ref → refs slab write; structural (SubState-Pointer/Vector) → pointer-switch or vector-resize behavior, derived from layout + decoded-value shape. No `StateMutation` wrapper.
- **Add `EntityState.decodeInto(FieldPath fp, Decoder decoder, BitStream bs)`** — zero-box variant for primitive leaves on FLAT-family states. Returns `capacityChanged`.
- **`StateMutation` class survives only for**: `readFieldsDebug` inspection tooling, `MutationRecorder` trace capture, baseline materialization, and any programmatic/setup writes outside the decoder. It is **never** produced on any field reader's hot path (S1 or S2).
- **Field readers (`S1FieldReader`, `S2FieldReader.readFieldsFast`) write state directly.** No staging, no `StateMutation` wrapping, no `pointerOverrides[]` shadow map — subsequent `resolveField` calls within a packet read from `state.pointerSerializers[]` directly (state is already mutated).

### FLAT: owner-pointer COW + decodeInto + inline strings
- **Owner-pointer COW on every container**: `Entry.owner`, `refsOwner` (governing both `refs` and `freeSlots`), `pointerSerializersOwner`. `copy()` becomes O(1). First write checks owner, clones only what it touches.
- **Remove `markSubEntriesNonModifiable`** and all `modifiable`/`refsModifiable`/`pointerSerializersModifiable` flag machinery.
- **Switch `refs` from `ArrayList<Object>` to `Object[]` + `int refsSize`**; `freeSlots` from `Deque<Integer>` to `int[]` + `int freeSlotsTop`. Clone via `Arrays.copyOf`.
- **Strings move inline into byte[]**. Add a new layout leaf shape (`Primitive.String` with `maxLength` field, or equivalent) that reserves `2 + maxLength` bytes at the leaf's offset. `char[N]` props (S2) use the declared `N`; unbounded strings (S2 `CUtlString` and all S1 STRING props) use a uniform `maxLength = 512`, grounded in the `StringLenDecoder`'s intrinsic 9-bit wire cap (511 bytes). String decoders get a `decodeInto` that writes length-prefix + UTF-8 bytes directly. `refs` slab thereafter holds only sub-Entry instances — no mixed Strings-and-sub-Entries.
- **Primitive decoders get `decodeInto(BitStream, byte[], int offset)`** via static dispatch (mirrors existing `DecoderDispatch.decode` shape). `DecoderDispatch.decodeInto(...)` is generated.
- **`FlatEntityState.decodeInto(fp, decoder, bs)`** — traverses to the leaf, invokes `DecoderDispatch.decodeInto` at the resolved offset. For String leaves routes to the inline-string `decodeInto` variant. Zero boxing, zero record allocation.

### NESTED_ARRAY: owner-pointer COW
- Port the owner-pointer pattern to `NestedArrayEntityState`. `copy()` becomes O(1) — shares `entries` list, `freeEntries` deque, and each `Entry` by reference. First slab-mutating write clones `entries` + `freeEntries` together; first write to an `Entry` clones its wrapper + state array.
- NESTED_ARRAY does **not** get `decodeInto` — its slots are `Object[]` so primitives still box. The owner-pointer work is about making snapshots free.
- NESTED_ARRAY also implements `state.write(fp, decoded)` — dispatches internally on leaf shape, no `StateMutation` needed.

### TREE_MAP: accept the regression
- `TreeMapEntityState` retains its monolithic backing map and its O(N-populated-fields) `copy()`. Under the new snapshot-before-decode model, this is a real cost. Accepted — TREE_MAP is a test-bed implementation with no production consumers. A COW-capable TreeMap replacement is noted as future work.
- TREE_MAP also implements `state.write(fp, decoded)`.

### S1: full rewrite (Option B)
- **Delete `ObjectArrayEntityState`.** Replace with **`S1FlatEntityState`** — byte[] + optional tiny refs slab + owner-ptr COW, mirroring FLAT's pattern on a simpler (single-level, fully-static) layout.
- **S1 field layout is statically computable** at `S1DTClass` compile time: one pass over receive props assigns a byte[] offset and leaf kind (`Primitive` / `Ref`) per index. No SubState, no nesting, no dynamic growth. Byte[] size fixed at state construction.
- **Port S1 decoders** (int / long / float / vector / vectorXY / array / string) to provide `decodeInto(BitStream, byte[], int offset)` via static dispatch. Mechanical — the infrastructure already exists from the S2 port.
- **`S1FieldReader` rewrite** — decode-direct via `state.decodeInto(fp, decoder, bs)` for primitives; `state.write(fp, decoded)` for anything else. No `WriteValue` allocation, no `FieldChanges.mutations[]` staging.
- **Inline strings work the same in S1 as in S2** — metadata-derived max length reserves a fixed byte[] span per String field.

### Prerequisite audits
- **String max-length audit DONE** during design via the new `:dev:s1sendtables` example. Results: S2 `char[N]` has declared `N`; S2 `CUtlString` and all S1 STRING have no metadata bound but are wire-capped at 511 bytes by `StringLenDecoder`'s 9-bit length prefix. Design D7 commits to uniform 512-byte inline reservation for unbounded strings.
- **Entity cache audit** (pre-CP-6): enumerate state-derived caches on `Entity` that need invalidation when `setState(snapshot)` is called for rollback.
- **S1 ARRAY audit** (pre-CP-7): 11 ARRAY props with fixed `numElements`; determine whether inner decoder permits inline expansion or whether a single refs slot per array is required.

### Unified commit path
- **Snapshot-before-decode in `Entities.processAndRunPacketEntities`** — one unified path for all state types. Before any state-mutating work for an entity within a packet, snapshot current state AND `Entity.exists` into a packet-scoped scratch, and replace state with `state.copy()`. On exception during decode, restore both; on success, drop the snapshots.
- **Snapshot scratch is allocation-free per packet**: `EntityState[maxEntityIndex]` + `int[] dirtyIndices` reused on `Entities`. Zero map churn.
- **`Entity.exists` joins the snapshot set** — fixes a long-standing atomicity hole where leave/delete flips during mid-packet failure were not rolled back.
- **`FieldChanges.mutations[]` removed from the fast path** entirely. `FieldChanges` carries `fieldPaths[]` and `capacityChanged`. The `mutations[]` field survives on the debug path and baseline materialization.
- **Event dispatch queue** carries event-firing lambdas only; state is already committed when lambdas run.

### Hot-loop micro-structure
- **`isFlat` hoisted out of `S2FieldReader.readFieldsFast`** — one `instanceof FlatEntityState` at the top of the method, reused for every field.
- **`field.isPrimitiveLeaf()`** is the single per-field check that routes decodeInto vs write (the decoder's primitive-type and the field's leaf shape are equivalent by construction).

### Listener contract
- **`MutationListener.onUpdateWrite(state, fp)`** — new callback for decode-direct writes. Listeners that need the value re-read via `state.getValueForFieldPath(fp)`. `onUpdateMutation(state, fp, mut)` continues for debug/trace paths and any path that produces a materialized `StateMutation`.
- `MutationRecorder` trace capture updated to implement `onUpdateWrite`.

### Non-goals
- Public `EntityState` interface shape unchanged for consumer code (`Entity.getProperty`, `@OnEntityUpdated`, etc.). `write` / `decodeInto` are added; nothing user-visible is removed or renamed.
- Debug path (`readFieldsDebug`, `dtinspector`) continues to use staging buffer for full fidelity.
- No COW-capable TreeMap in this change.

## Capabilities

### New Capabilities
- `entity-update-commit`: Packet-scoped atomic commit semantics — snapshot state AND `Entity.exists` before decode, mutate in place, rollback on failure, event dispatch after commit. Unified across all state types.
- `s1-flat-entity-state`: S1 flat state with byte[] primitive storage, optional small refs slab, owner-ptr COW, static layout from DTClass, direct-write and decodeInto methods. Replaces `ObjectArrayEntityState`.

### Modified Capabilities
- `flat-entity-state`: O(1) `copy()` via owner-pointer. Adds `decodeInto` (primitive + inline-string) and `write` (single direct-write method). `refs` becomes `Object[]` + `int[] freeSlots`. Strings move inline into the composite byte[] (refs slab holds only sub-Entries thereafter).
- `nested-entity-state`: O(1) `copy()` via owner-pointer on slab + per-Entry. `boolean modifiable` flag removed. Implements `write(fp, decoded)` for direct dispatch without StateMutation wrapping. State mutates in place during `readFields`.
- `state-mutation`: `StateMutation` is **not** produced by any field reader on any hot path. Every reader writes via `state.write` or `state.decodeInto`. The class survives for `readFieldsDebug`, `MutationRecorder`, baseline materialization, and programmatic/setup writes.

## Impact

- `skadistats.clarity.model.state`:
  - `FlatEntityState` — `copy()` rewritten; owner pointers replace flags; `refs` → `Object[]`+`int[]`; `decodeInto` and `write` added; String handling moves to inline layout.
  - `NestedArrayEntityState` — `copy()` rewritten; owner pointers replace flags; `write` added.
  - `TreeMapEntityState` — `write` added (delegates to existing logic; no COW change).
  - `AbstractS2EntityState` — `pointerSerializersOwner` COW.
  - `S1FlatEntityState` — **new** class. byte[] + optional refs slab + owner-ptr COW + `write` + `decodeInto`.
  - `ObjectArrayEntityState` — **deleted**.
  - `FieldLayout` — new leaf shape for inline strings (or extend `Primitive` with a `maxLength` field).
  - `EntityState` interface — `write` and `decodeInto` methods added; `applyMutation` retained for non-reader callers.
- `skadistats.clarity.io.decoder`:
  - Primitive decoders gain static `decodeInto(BitStream, byte[], int offset)`.
  - `StringLenDecoder` gains static `decodeInto` writing length-prefix + UTF-8 bytes.
  - S1 decoders (`io/decoder/factory/s1/*`) gain `decodeInto`.
  - `DecoderDispatch` gains generated `decodeInto` switch.
- `skadistats.clarity.io.s1.S1FieldReader` — rewritten to decode-direct; no `WriteValue` / `FieldChanges.mutations[]`.
- `skadistats.clarity.io.s1.S1DTClass` — gains per-index offset table and leaf-kind array.
- `skadistats.clarity.io.s2.S2FieldReader`:
  - `readFieldsFast` — `isFlat` hoisted; per-field `isPrimitiveLeaf()` check routes to `decodeInto` or `state.write`. No `pointerOverrides[]`.
  - `readFieldsDebug` — unchanged (retains staging for inspection).
- `skadistats.clarity.io.FieldChanges` — `mutations[]` nullable; `capacityChanged` boolean added.
- `skadistats.clarity.io.MutationListener` — `onUpdateWrite(state, fp)` added.
- `skadistats.clarity.processor.entities.Entities`:
  - `processAndRunPacketEntities` — reusable `EntityState[]` + `int[]` + `boolean[]` snapshot scratch; unified snapshot-and-rollback covering state AND `Entity.exists`; event-only queued lambdas.
  - `Entity.setExists(boolean)` setter for rollback.
- `skadistats.clarity.bench.trace.MutationRecorder` — implements `onUpdateWrite`, reads back via `getValueForFieldPath`.
- No user-visible API changes for example/consumer code.
