## Context

Three cost centers on the S2 hot path are entangled:

1. **`FlatEntityState.copy()` walks the FieldLayout tree.** `Entry.copy()` calls `markSubEntriesNonModifiable` which recurses through every `Composite`/`Array` node to find `SubState` nodes and flip `modifiable = false` on each reachable sub-Entry. Hundreds of node visits per copy for a Hero-sized entity.

2. **Every primitive mutation autoboxes twice.** `DecoderDispatch.decode` returns `Integer`/`Float`/`Long`; `S2FieldReader.readFieldsFast` wraps in `WriteValue(Object)`; `FlatEntityState.applyMutation` casts back and unboxes. Per primitive mutation: one autobox + one `WriteValue` record + one unbox.

3. **A staging buffer holds every mutation between decode and commit.** `FieldChanges.mutations[]` exists to preserve packet-level atomicity: if decode throws on entity N, entities 0..N-1 never see their state mutated.

The three are linked: eliminating autoboxing means decode writes directly into state byte[], which means the staging buffer cannot exist, which means a different atomicity mechanism is needed, which must be cheap per entity — forcing O(1) `copy()`.

`NestedArrayEntityState` has the same shape of problem at the slab level (N `Entry` wrappers per `copy()`). `ObjectArrayEntityState` (Source 1) is worse: eager O(N) `System.arraycopy` per copy, and `S1FieldReader` also routes through `new WriteValue(...)` + staging. All four concerns (hot-path atomicity, autoboxing, staging, copy cost) resolve together or not at all.

The `decouple-field-from-state` and `flat-entity-state` changes (archived 2026-04-13 and 2026-04-14) introduced `StateMutation`, `EntityState.applyMutation`, and `FlatEntityState`. This change removes `StateMutation` from every reader's hot path and unifies commit semantics across all state types.

## Goals / Non-Goals

**Goals:**
- Every field reader (S1 and S2) writes decoded values directly to state. No `StateMutation` produced on the hot path.
- `copy()` O(1) for `FlatEntityState`, `NestedArrayEntityState`, and `S1FlatEntityState`.
- Primitive mutations on FLAT incur zero autoboxing and zero `WriteValue` allocation.
- Primitive mutations on S1FlatEntityState incur zero autoboxing and zero `WriteValue` allocation (after port).
- Packet-level atomicity preserved and strengthened to cover `Entity.exists` flips.
- Snapshot-before-decode incurs zero allocation per packet.
- No user-visible API changes.

**Non-Goals:**
- Typed user-facing read API.
- Removing `StateMutation` as a class — it survives for debug/trace/baseline/programmatic-writes.
- A COW-capable `TreeMapEntityState` (documented as future work).
- Changing decoder output types (Integer/Float/String etc. stay boxed when decode is called, just not on the hot path).
- Optimizing `readFieldsDebug`.

## Decisions

### D1: Owner-pointer COW — FlatEntityState

Each `Entry` carries `FlatEntityState owner` instead of `boolean modifiable`. First write checks `entry.owner == this`; mismatch clones `byte[]`, constructs a new Entry with `owner = this`, updates referring slot.

The `refs` container and `pointerSerializers` array use the same pattern via `refsOwner` and `pointerSerializersOwner`. Data shape: `refs` is `Object[]` + `int refsSize`; `freeSlots` is `int[]` + `int freeSlotsTop`. Clone via `Arrays.copyOf`. `refs` and `freeSlots` are always cloned together.

### D2: Owner-pointer COW — NestedArrayEntityState

Same pattern as D1. `Entry.modifiable` → `Entry.owner` (of type `NestedArrayEntityState`). `entriesOwner` field governs the `entries` list and `freeEntries` deque as a pair; both clone together on first slab-mutating write. `copy()` shares everything by reference; no `markFree` loop, no Entry wrapper allocations.

### D3: `copy()` is pure reference sharing

Both FLAT and NESTED_ARRAY copy constructors share containers by reference and leave owner pointers unset (null / mismatching). Sub-entries reached via traversal are handled by per-Entry owner check at write time. Constant-time regardless of entity size.

### D4: Decoder static `decodeInto` dispatch

Each primitive decoder gets `public static void decodeInto(BitStream bs, <Self> d, byte[] data, int offset)`. Generated `DecoderDispatch.decodeInto(bs, decoder, data, offset)` switches on decoder id.

Per-decoder semantics:
- Scalar (INT/FLOAT/LONG) → `*_VH.set(data, offset, value)`.
- Bool → `data[offset] = value ? (byte)1 : (byte)0`.
- Vector / VectorXY / Array → compose inner `decodeInto` at `offset + i * stride`. No intermediate `Vector` / `Object[]`.
- String (`StringLenDecoder`) → write length prefix (1–2 bytes) + UTF-8 bytes starting at `offset + prefixBytes`. Writes into a pre-reserved `maxLength + prefixBytes` span (D7).

Reference-producing decoders that aren't inlineable (none in S2; TBD in S1 after audit) stay on the boxing path.

### D5: State-level `decodeInto` and `write`

Every `EntityState` implementation gains two methods that supersede `applyMutation` on the reader hot path:

```java
interface EntityState {
    boolean decodeInto(FieldPath fp, Decoder decoder, BitStream bs);
    boolean write(FieldPath fp, Object decoded);
    // applyMutation retained for debug / trace / baseline / programmatic writes
}
```

`decodeInto`: zero-box variant. Only implemented by FLAT-family states (FlatEntityState, S1FlatEntityState) for Primitive leaves (including inline-string leaves). Returns `capacityChanged`.

`write`: single direct-write method. State's own leaf traversal dispatches on layout shape:
- **Primitive leaf** → write value into byte[] via `PrimitiveType.write(data, offset, value)`. For String leaves, write length + bytes.
- **Ref leaf** → slot alloc (if needed) + store in refs slab + set flag byte.
- **SubState-Pointer leaf** → `decoded` is a Serializer (or null). Switch pointer: clear existing sub-Entry in refs, set `pointerSerializers[pfId] = decoded`, lazily create the new sub-Entry shell on the next traversal.
- **SubState-Vector leaf** → `decoded` is an `Integer` length. Resize the vector sub-Entry's capacity.

The state never needs a `StateMutation` wrapper to know what to do — layout + decoded-value shape are sufficient.

### D6: `S2FieldReader.readFieldsFast` shape

`isFlat` hoisted out of the loop; `field.isPrimitiveLeaf()` is the single per-field check.

```java
boolean isFlat = state instanceof FlatEntityState;
FlatEntityState fes = isFlat ? (FlatEntityState) state : null;

for (var r = 0; r < n; r++) {
    var fp    = fieldPaths[r].s2();
    var field = resolveField(state, dtClass, fp);

    if (isFlat && field.isPrimitiveLeaf()) {
        capacityChanged |= fes.decodeInto(fp, field.getDecoder(), bs);
    } else {
        var decoded = DecoderDispatch.decode(bs, field.getDecoder());
        capacityChanged |= state.write(fp, decoded);
    }
}
return new FieldChanges(fieldPaths, n, capacityChanged);
```

`pointerOverrides[]` is removed. Under immediate-write semantics, `resolveField` reads current serializers directly from `state.pointerSerializers[]`, which was updated the moment the pointer field was written. No shadow map needed.

### D7: Inline strings in byte[] — uniform 512-byte reservation

Strings move from the refs slab into the composite byte[]. Two cases based on what the wire format guarantees:

- **S2 `char[N]`** (e.g. `char[128]`, `char[256]`, `char[512]`): the type name carries an explicit declared bound `N`. Inline leaf reserves `2 + N` bytes (2-byte length prefix + N data bytes).
- **Unbounded strings** — all S2 `CUtlString` and all S1 `PropType.STRING`: per-prop metadata carries no max length. The shared `StringLenDecoder` enforces a wire-level cap of 511 bytes (hardcoded 9-bit length prefix). Inline leaf reserves a uniform **514 bytes** (2-byte prefix + 512 bytes — rounded up from the decoder's 511 ceiling for alignment).

This grounds the design in what the decoder actually guarantees, not in metadata that isn't there. Audit results (see §0 of tasks.md) confirm the footprint: max ~2 KB of extra byte[] per entity type in the worst case (game-mode serializers with 4× unbounded strings); typically 0–1.5 KB. Immaterial vs overall entity-state memory.

FieldLayout gains a leaf shape for inline strings — either a dedicated `Primitive.String(offset, prefixBytes, maxLength)` or extending the existing `Primitive` leaf with a `maxLength` field that is `> 0` for strings and `0` for fixed-width primitives.

`decodeInto` for a String leaf writes `[length-prefix][UTF-8 bytes up to maxLength]` at the leaf's offset. `write` for a call that targets an inline-string leaf goes the same route (encode String to UTF-8, write length + bytes).

`getValueForFieldPath` on an inline-string leaf reads the length prefix and allocates a `String`. This is a read-time allocation (vs the old refs-slab approach's cached reference). Accepted — the decode-time saving dominates. If read-time allocation shows up in profiles, a per-leaf cached-String companion array can be added as a follow-up.

**Consequence**: S2 `refs` slab holds only sub-Entry instances. S1 refs slab is empty unless the Array-decoder audit (§0.6) identifies ARRAY props that decode to a per-element `Object[]` stored at one idx — if so, those stay in refs; otherwise the S1 refs slab disappears entirely and `S1FlatEntityState` reduces to `byte[] data + owner + layout`.

### D8: S1 full rewrite

`ObjectArrayEntityState` is deleted. `S1FlatEntityState` replaces it.

Structure (simpler than S2 FLAT because S1 is fully static and non-nested):
```java
final class S1FlatEntityState implements EntityState {
    private final S1FieldLayout layout;      // per-DTClass, shared
    private S1FlatEntityState owner;         // owner pointer on the byte[]
    private byte[] data;
    // Optional, only if any non-inline refs survive the audit:
    private Object[] refs;                    // or absent entirely
    private S1FlatEntityState refsOwner;     // if refs present
}
```

Single-level layout: each field's `int idx` maps to `(leafKind, offset, maxLength)` at DTClass compile time. No traversal — direct array lookup. No SubState, no nesting, no polymorphism (S1 has none).

`copy()` shares `layout` (always immutable), `data` (owner-ptr COW), and optional `refs` (owner-ptr COW). O(1).

`decodeInto(fp, decoder, bs)`: `makeWritable()`, then `DecoderDispatch.decodeInto(bs, decoder, data, layout.offsetOf(fp))`. One-liner once owner check passes.

`write(fp, decoded)`: dispatch on `layout.kindOf(fp)` — Primitive / Ref (only if audit keeps any refs).

S1 decoders ported to have `decodeInto` via the same static-dispatch pattern as S2. The S1 decoder package (`io/decoder/factory/s1/*`) is mechanical — maybe 8–10 concrete decoders.

`S1FieldReader.readFields` rewritten:
```java
for (var r = 0; r < n; r++) {
    var o = fieldPaths[r].s1().idx();
    var decoder = receiveProps[o].getDecoder();
    if (decoder.getPrimitiveType() != null || isInlineString(receiveProps[o])) {
        state.decodeInto(fp, decoder, bs);
    } else {
        state.write(fp, DecoderDispatch.decode(bs, decoder));
    }
}
```

### D9: `Entities` — unified snapshot-and-rollback with reusable scratch

```java
private EntityState[] snapshotScratch;        // [maxEntityIndex]
private boolean[] existsSnapshotScratch;
private int[] dirtyIndices;
private int dirtyTop;
```

`snapshotAndCopy(entity)` is idempotent per packet — records state + exists, pushes index, replaces with `state.copy()`. Called at the top of every `queueEntity*` method (update, create, recreate, enter, leave, delete).

On `Throwable` from `processPacketEntities`: iterate `dirtyIndices[0..dirtyTop]`, restore via `setState` + `setExists`, clear queuedUpdates, rethrow.

On normal completion: queued events fire in order; finally block iterates dirty indices nulling `snapshotScratch[idx]` and resets `dirtyTop = 0`. Zero allocation per packet.

### D10: `Entity.exists` is part of the snapshot set

Pre-existing atomicity hole (not introduced by this change): leave/delete/create flips `Entity.exists` but no rollback reverts it. The unified snapshot mechanism fixes it — `existsSnapshotScratch` captures the flag alongside state, `Entity.setExists(boolean)` restores it on rollback.

### D11: `FieldChanges` simplified

```java
public final class FieldChanges {
    private final FieldPath[] fieldPaths;
    private final int length;
    private final StateMutation[] mutations;    // null on fast path
    private final boolean capacityChanged;
}
```

Fast-path constructor omits `mutations`. `applyTo(state)` is a no-op returning `capacityChanged` when `mutations == null`. Legacy constructor retained for debug / baseline paths.

### D12: `MutationListener` contract shift

- `onUpdateWrite(state, fp)` — new callback. Invoked after `decodeInto` completes, for listeners attached to the reader. Listener re-reads value via `state.getValueForFieldPath(fp)` if needed.
- `onUpdateMutation(state, fp, mutation)` — unchanged contract. Invoked by code paths that still produce a materialized `StateMutation` (debug / trace / baseline / programmatic).
- `MutationRecorder` implements `onUpdateWrite`.

### D13: Debug path unchanged

`readFieldsDebug` continues to populate both `fieldPaths[]` and `mutations[]`, call `field.createMutation`, and invoke `state.applyMutation`. Full inspection fidelity. Snapshot/rollback at the `Entities` level works with either reader — the scratch mechanism is agnostic.

### D14: Entity cache audit (prerequisite)

Before CP-5 ships, enumerate caches on `Entity` that derive from state-held data:
- Field-path → property-name caches
- Pointer-serializer lookups
- `dtClass` pointer (invariant; not a concern)
- Event-dispatch caches

Outcome: document findings inline. If any cache must invalidate on `setState`, add an explicit hook. Otherwise confirm invariance.

### D15: StateMutation class scope

Retained, but narrowed:
- Not produced by `S1FieldReader` on the hot path.
- Not produced by `S2FieldReader.readFieldsFast`.
- Still produced by `S2FieldReader.readFieldsDebug` for inspection.
- Still produced by baseline materialization (not hot).
- Still used by `MutationRecorder` for trace capture (records decode events as serializable records).
- Still available for programmatic writes (any code outside the reader that wants to express a mutation declaratively).

`EntityState.applyMutation(fp, mutation)` is retained for these callers. Only the reader stops using it.

## Checkpoints and benches

**CP-0 — Baseline (`FlatCopyBench` precursor)**
- `FlatCopyBench` micro lands independently against current master. Captures "before" number.
- `EntityStateParseBench` + `MutationTraceBench` baseline runs on current master.

**CP-1 — Owner-pointer COW on FlatEntityState (D1, D3)**
- `FlatCopyBench`: ≥10× improvement.
- `EntityStateParseBench FLAT`: no regression.
- Acceptance: zero allocation in `copy()` beyond the FlatEntityState object.

**CP-2 — Owner-pointer COW on NestedArrayEntityState (D2)**
- `NestedArrayCopyBench`: O(1), zero Entry allocations under `-prof gc`.
- `EntityStateParseBench NESTED_ARRAY`: no regression.

**CP-3 — Decoder `decodeInto` + DecoderDispatch (D4)**
- `DecodeIntoBench` micro per decoder family: zero allocation, ns/op ≥ existing path.

**CP-4 — `FlatEntityState.decodeInto` and `write` (D5)**
- `FlatWriteBench` micro: decodeInto vs applyMutation-WriteValue. Zero allocation on primitive writes, ≥30% ns/op improvement.
- Unit: parity test (`decodeInto` vs `applyMutation(WriteValue(decode))`) for all primitive decoders.

**CP-5 — Inline strings (D7)**
- Prerequisite audit complete (string max-length metadata + histogram).
- Layout + decoder + state support for inline strings.
- `InlineStringBench` micro: decode + read roundtrip vs refs-slab path.
- Gate: memory footprint not bloated beyond an accepted threshold (documented post-histogram).

**CP-6 — Unified `readFieldsFast` + Entities snapshot/rollback (D6, D9, D10, D11)**
- `EntityStateParseBench FLAT`: substantial improvement vs CP-0 (target ≥20% wall-clock).
- `EntityStateParseBench NESTED_ARRAY`: neutral or improved.
- `EntityStateParseBench TREE_MAP`: regression accepted; measured and documented.
- `-prof gc` on FLAT parse: autobox allocation near-zero.
- Integration: full replay, bit-identical event stream vs CP-0 for FLAT + NESTED_ARRAY.
- Integration: deliberate-failure test exercises `Entity.exists` rollback (D10).

**CP-7 — S1 full port (D8)**
- Delete `ObjectArrayEntityState`, land `S1FlatEntityState`.
- S1 decoders ported.
- `S1FieldReader` rewritten.
- `EntityStateParseBench S1` (new or extended): substantial improvement.
- Integration: a full S1 (old Dota 2) replay parses with bit-identical event stream vs CP-0.

**CP-8 — `MutationListener` contract (D12)**
- `MutationRecorder` trace parity test.
- `dtinspector` smoke test.

Each CP has an acceptance criterion in `tasks.md`; don't advance past the gate.

## Risks / Trade-offs

- **Risk**: Inline-string max-length audit reveals props without metadata bounds. → Mitigation: the audit is a blocking task; if it fails, revisit design (likely: keep inline for most, fall back to refs for unbounded props — hybrid).

- **Risk**: Inline-string read-time allocation surfaces as a regression for handlers that re-read strings per event. → Mitigation: cached-String companion as a follow-up optimization.

- **Risk**: Rollback path is rare and under-tested. → Mitigation: explicit failure-path unit tests, including `Entity.exists` rollback.

- **Risk**: S1 decoder port has edge cases (coord decoders, specific bit layouts). → Mitigation: parity unit test for each decoder: `decodeInto(bs, buf, 0)` produces the same bytes as `PrimitiveType.write(buf, 0, decode(bs))`.

- **Risk**: TREE_MAP regression annoys someone. → Mitigation: document in change notes. TREE_MAP is opt-in via `withS2EntityState(TREE_MAP)`; users selected it deliberately.

- **Risk**: Entity cache invalidation audit (D14) misses something. → Mitigation: explicit audit gate before CP-6; unit test asserts post-rollback property reads match pre-packet.

- **Trade-off**: `readFieldsFast` has a single `instanceof FlatEntityState` hoisted to the top. JIT handles this trivially. Cleaner than pushing behind a virtual method.

- **Trade-off**: `MutationTraceBench` becomes primarily a non-FLAT measurement after CP-6 (FLAT no longer exercises `applyMutation`). Accepted; `EntityStateParseBench` is the end-to-end FLAT signal.

- **Trade-off**: S1 full port drags a mechanical decoder-porting workstream into an already-large change. Chosen over a smaller-scope approach for consistency — after this change, every first-class state type is byte[]-based with owner-ptr COW.

## Future follow-ups (not in this change)

- **COW-capable TreeMapEntityState** — persistent/functional map or sharded design.
- **Field-path cursor in `readFieldsFast`** — incremental traversal for sorted fp sequences.
- **Cached-String companion** — lazy cache on inline-string leaves if read-time allocation shows up in profiles.
- **Fixed-capacity small-string optimization** — inline the entire string in a tagged leaf with no length prefix for short props.
- **Inline S1 ARRAY props** — 11 ARRAY props in full Dota S1 DT tree currently occupy one refs slot each in `S1FlatEntityState`. Inlining them requires: (a) extending `s1sendtables` dumper to print `SendProp.template.type` and auditing the inner types, (b) `ArrayDecoder.decodeInto` that reserves `numElements × innerSize + countPrefix` bytes and composes the inner `decodeInto` per element, (c) deciding on a policy when the inner type is STRING or nested ARRAY. If every inner type is primitive, the S1 refs slab can be dropped entirely and `S1FlatEntityState` reduces to `byte[] data + owner + layout`.

## Open Questions

- Should `EntityState.decodeInto(fp, decoder, bs)` be an interface method with a default implementation (delegates to `decode + write`), removing the `instanceof FlatEntityState` check in the reader? Defer decision to CP-6 measurement.
- For S1 `ARRAY` prop type, does the decoder produce an `Object[]` stored as one ref slot, or does the DT-compile flatten arrays to per-element indices? Implementation-time audit; determines whether S1 refs slab is empty or non-empty.
