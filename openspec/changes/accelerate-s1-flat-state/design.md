## Context

S1 is the original Dota 2 / CS:GO (pre-Source-2) replay format. Its entity state is 1D-flat: every entity class has a fixed list of `ReceiveProp[]`, indexed by `S1FieldPath.idx`. No sub-serializers, no polymorphism, no vectors-of-sub-states. Arrays (`PropType.ARRAY`) are the only non-scalar shape, and they decode to a whole `Object[]` stored at one prop index.

The current `ObjectArrayEntityState` is the simplest possible implementation — `new Object[length]`, `state[idx] = value` — which was fine when the `StateMutation` abstraction was the interface contract, but now imposes two costs the S2 side has shed:

1. **Autoboxing**: every `int`/`float`/`long` primitive is wrapped on decode (`DecoderDispatch.decode` returns `Object`) and stored boxed forever.
2. **`WriteValue` records**: `S1FieldReader.readFields` wraps every decoded value in `new StateMutation.WriteValue(decoded)` and stores it in `FieldChanges.mutations[]`. That's one record per field per packet — pure overhead under the current interface, because `ObjectArrayEntityState.applyMutation` just unwraps it back to the raw value.

S1 STRING props are the worst offender: 458 props in the full Dota S1 DT, each decoded via `StringLenDecoder.decode` → `new String(...)` of up to 511 bytes + the enclosing `WriteValue`. That's ~110 KB of throwaway per packet in the worst case on a class with all STRINGs populated.

The infrastructure to fix this wholesale already exists:
- `EntityState.decodeInto(fp, decoder, bs)` + `EntityState.write(fp, decoded)` — hot-path contract, skips `StateMutation`.
- `DecoderDispatch.decodeInto` — generated switch covering all primitive + vector S1 decoders.
- `FieldLayout.InlineString` leaf shape + `StringLenDecoder.decodeIntoInline(bs, data, offset, maxLength)` — zero-alloc string write.
- Eager-copy model for state copies (post-`strip-entity-state-cow`).

S1's simpler shape (1D-flat, no nesting) means the port is mechanically small. No FieldLayout tree needed — a single `S1FlatLayout` with per-prop arrays suffices.

## Goals / Non-Goals

**Goals:**
- Every S1 hot-path field write goes through `decodeInto` (primitive + inline-string) or `write` (ARRAY Ref). Zero `StateMutation` allocation on the hot path.
- `S1FlatEntityState.copy()` is an eager deep copy (one `Arrays.copyOf(data)` + optional `Arrays.copyOf(refs)`). No COW.
- `ObjectArrayEntityState` is retained as a parallel, selectable variant (mirrors S2's pattern of keeping `NESTED_ARRAY` / `TREE_MAP` alongside `FLAT`). `S1EntityStateType` gains `FLAT` alongside existing `OBJECT_ARRAY`; runner default flips to `FLAT`.
- Full parity with the current reader: decode bit consumption identical; `getValueForFieldPath` returns values compatible with current consumers.
- No user-visible API changes.

**Non-Goals:**
- Deleting `ObjectArrayEntityState` — kept as a benchmark/fallback baseline.
- Inline S1 ARRAY props (deferred — audit `template.type` first; see Future follow-ups).
- Typed read API (`getInt`, `getFloat` etc.).
- Renaming any of the S2 state classes for engine-prefix symmetry — purely mechanical, split out as a separate follow-up change so this one stays focused on behaviour.
- Changes to `ReceiveProp.decode`, `S1DTClass` structure beyond layout caching, or `S1DecoderFactory`.
- Changes to debug (`readFields` with `debug=true`) or materialize (`materialize=true`) paths beyond mechanical re-slotting to the new state type.

## Decisions

### D1: Single-level `S1FlatLayout`

S1 has no nesting. A flat per-DTClass layout suffices:

```java
public final class S1FlatLayout {
    public enum LeafKind { PRIMITIVE, INLINE_STRING, REF }

    private final LeafKind[] kinds;         // indexed by receive-prop idx
    private final int[]       offsets;       // byte offset of (flag + value) in data[]
    private final PrimitiveType[] primTypes; // null unless kinds[i] == PRIMITIVE
    private final int[] maxLengths;          // 0 unless kinds[i] == INLINE_STRING
    private final int dataBytes;             // total byte[] size
    private final int refSlots;              // count of REF leaves (upper bound on refs[].length)
}
```

Offset layout per prop: `[flag byte][value bytes]`. Primitive value bytes come from `primType.size()`; inline strings reserve `2 + maxLength` bytes (little-endian length prefix + UTF-8); refs reserve 4 bytes (int slot index, via `INT_VH`).

Build rules (at `S1DTClass.getFlatLayout()` first call):

```java
for each receiveProp rp at index i:
    Decoder d = rp.getSendProp().getDecoder();
    if (d.getPrimitiveType() != null)           → PRIMITIVE(d.getPrimitiveType())
    else if (d instanceof StringLenDecoder)     → INLINE_STRING(maxLength = 512)
    else                                         → REF        // ARRAY only (audit 0.6)
```

Uniform `maxLength = 512` for inline strings mirrors the S2 decision in `accelerate-flat-entity-state` D7 — grounded in `StringLenDecoder`'s 9-bit wire cap (511 bytes) rounded for alignment. S1 STRING metadata (`numBits`/`flags`/`numElements`) carries no per-prop bound, so the uniform reservation is the best grounded choice.

Worst-case `dataBytes` for a Dota 2 S1 DTClass with all 458 STRING props: `458 × (1 + 2 + 512) ≈ 236 KB`. Typical DTClasses are much smaller. The layout is shared across all instances of a DTClass, so the footprint is amortized.

### D2: `S1FlatEntityState` — direct byte[] + optional small refs

```java
public final class S1FlatEntityState implements EntityState {
    private final S1FlatLayout layout;   // shared per-DTClass
    private byte[] data;
    private Object[] refs;               // null if layout.refSlots() == 0
    private int[] freeSlots;             // free-list stack; null if no refs
    private int freeSlotsTop;
}
```

- **Construct** (fresh): `data = new byte[layout.dataBytes()]`, refs allocated lazily (null) or at construction time if `layout.refSlots() > 0`. Freelist initialized empty — slots are append-first, free-list-reused.
- **Copy** (eager): `new byte[data.length]` via `Arrays.copyOf`; refs + freeSlots via `Arrays.copyOf` if non-null. No owner pointer. `copy()` returns a fully-independent state; the `EntityState.copy()` post-condition from `strip-entity-state-cow` is "fully independent" — S1 matches that post-condition by construction.
- **No `pointerSerializers[]`** (S1 has no polymorphism; this is an S2-only concept carried by `AbstractS2EntityState`). S1FlatEntityState does NOT extend any S2-family abstract.

### D3: `decodeInto(fp, decoder, bs)` routing

```java
public boolean decodeInto(FieldPath fpX, Decoder decoder, BitStream bs) {
    int i = fpX.s1().idx();
    int offset = layout.offsets()[i];
    switch (layout.kinds()[i]) {
        case PRIMITIVE    -> { data[offset] = 1;
                               DecoderDispatch.decodeInto(bs, decoder, data, offset + 1); }
        case INLINE_STRING -> { data[offset] = 1;
                                // StringLenDecoder is the only decoder S1 maps to INLINE_STRING
                                StringLenDecoder.decodeIntoInline(bs, data, offset + 1, layout.maxLengths()[i]); }
        case REF          -> throw new IllegalStateException("decodeInto called on REF leaf");
    }
    return false;   // S1 has no structural capacity changes
}
```

Flag byte convention (shared with S2): `data[offset] == 0` means "never written / absent", `== 1` means "has value". `getValueForFieldPath` uses the flag to decide whether to return the decoded value or `null` (S1 legacy semantic: unset slot → `null`).

### D4: `write(fp, decoded)` routing

```java
public boolean write(FieldPath fpX, Object decoded) {
    int i = fpX.s1().idx();
    int offset = layout.offsets()[i];
    switch (layout.kinds()[i]) {
        case PRIMITIVE     -> { data[offset] = 1;
                                layout.primTypes()[i].write(data, offset + 1, decoded); }
        case INLINE_STRING -> { data[offset] = 1;
                                writeInlineString(offset + 1, (String) decoded, layout.maxLengths()[i]); }
        case REF           -> {
            if (data[offset] == 0) {
                int slot = allocateRefSlot();
                INT_VH.set(data, offset + 1, slot);
                data[offset] = 1;
                refs[slot] = decoded;
            } else {
                int slot = (int) INT_VH.get(data, offset + 1);
                refs[slot] = decoded;
            }
        }
    }
    return false;
}
```

`write` covers three callers:
1. The hot path for REF leaves (from `S1FieldReader.readFields`).
2. Programmatic / setup writes (tests, baseline materialization via `applyMutation`).
3. `applyMutation(fp, WriteValue(v))` delegates to `write(fp, v)`.

`writeInlineString` UTF-8-encodes + writes length prefix; truncates at `maxLength` (same contract as `StringLenDecoder.decodeIntoInline`).

### D5: `applyMutation(fp, WriteValue)` kept for non-reader callers

S1 has no `ResizeVector` / `SwitchPointer` — `applyMutation` on S1 only ever receives `WriteValue`. Implementation:

```java
public boolean applyMutation(FieldPath fp, StateMutation mutation) {
    return write(fp, ((StateMutation.WriteValue) mutation).value());
}
```

Non-`WriteValue` inputs throw via the cast. This matches the existing `ObjectArrayEntityState.applyMutation` shape. Used by:
- `S1FieldReader.readFields` when `materialize=true` (traces, baseline materialization — calls `fieldChanges.applyTo(state)` which iterates `state.applyMutation`).
- `MutationRecorder` replay.
- Any test or internal programmatic write.

### D6: `getValueForFieldPath` — reverse translation

```java
public <T> T getValueForFieldPath(FieldPath fpX) {
    int i = fpX.s1().idx();
    int offset = layout.offsets()[i];
    if (data[offset] == 0) return null;      // matches ObjectArrayEntityState semantic
    return (T) switch (layout.kinds()[i]) {
        case PRIMITIVE     -> layout.primTypes()[i].read(data, offset + 1);
        case INLINE_STRING -> readInlineString(offset + 1);
        case REF           -> refs[(int) INT_VH.get(data, offset + 1)];
    };
}
```

`readInlineString`: read 2-byte LE length prefix, allocate `new String(data, offset + 3, len, UTF_8)`. Per-read allocation accepted (same design as S2; revisit only if profiles show it).

### D7: `fieldPathIterator` — every populated (flag=1) slot

Current `ObjectArrayEntityState.fieldPathIterator` returns `S1FieldPath(i)` for `i in 0..length-1`, regardless of whether the slot was ever written. That's a pre-existing quirk — downstream consumers (`@OnEntityUpdated` name lookups, `dtinspector` dump) iterate and filter `null` themselves.

Preserve the existing semantic: iterate `0..layout.kinds.length - 1`, unconditionally. Do NOT filter on flag byte. This keeps diffs in consumer behaviour to zero. A follow-up could tighten the contract, but that's a separate discussion.

### D8: `S1FieldReader.readFields` rewritten (state-agnostic fast path)

Both `OBJECT_ARRAY` and `FLAT` share the same reader path. The state decides whether `decodeInto` is zero-alloc (FLAT) or falls back to the boxing path (OBJECT_ARRAY).

```java
public FieldChanges readFields(BitStream bs, DTClass dtClassGeneric, EntityState state,
                               boolean debug, boolean materialize) {
    var dtClass = dtClassGeneric.s1();

    if (debug)         return readFieldsDebug(bs, dtClass, state);
    if (materialize)   return readFieldsMaterialize(bs, dtClass, state);

    // Fast path: decode-direct, no StateMutation. Uniform across OBJECT_ARRAY and FLAT.
    var layout       = dtClass.getFlatLayout();
    var receiveProps = dtClass.getReceiveProps();
    var n = readIndices(bs, dtClass);

    for (var ci = 0; ci < n; ci++) {
        var o       = fieldPaths[ci].s1().idx();
        var decoder = receiveProps[o].getSendProp().getDecoder();
        if (layout.kinds()[o] == S1FlatLayout.LeafKind.REF) {
            state.write(fieldPaths[ci], DecoderDispatch.decode(bs, decoder));
        } else {
            state.decodeInto(fieldPaths[ci], decoder, bs);
        }
    }
    return new FieldChanges(fieldPaths, n, /* capacityChanged */ false);
}
```

`readFieldsDebug` and `readFieldsMaterialize` populate `FieldChanges.mutations[]` via `new WriteValue(DecoderDispatch.decode(bs, decoder))` for every field — retaining the full materialized-record path for tooling and baseline construction. Both call the boxed `decode` variant; neither is on the hot path.

The fast-path constructor `new FieldChanges(FieldPath[] paths, int length, boolean capacityChanged)` already exists from `accelerate-flat-entity-state`. Reuse it.

`ObjectArrayEntityState` gains a `decodeInto(fp, decoder, bs)` implementation so the reader has a uniform call shape:

```java
@Override
public boolean decodeInto(FieldPath fp, Decoder decoder, BitStream bs) {
    state[fp.s1().idx()] = DecoderDispatch.decode(bs, decoder);
    return false;
}
```

This is strictly a convenience — `OBJECT_ARRAY` still pays the full boxing cost (the decoder's `decode` allocates `Integer`/`Float`/`String`/etc.). The performance story stays concentrated in `S1FlatEntityState`.

### D9: `S1EntityStateType` gains `FLAT` alongside `OBJECT_ARRAY`

- `OBJECT_ARRAY` is retained. `OBJECT_ARRAY.createState(...)` unchanged.
- New `FLAT` variant. `FLAT.createState(...)` allocates `S1FlatEntityState(S1DTClass)`. The factory signature is updated from `createState(ReceiveProp[])` to `createState(S1DTClass)` so the state constructor can read `dtClass.getFlatLayout()`; `OBJECT_ARRAY.createState` ignores everything except `dtClass.getReceiveProps().length`. This is an internal factory (no public callers pass `ReceiveProp[]` directly — confirm during implementation).
- `AbstractFileRunner.s1EntityStateType` default flips from `OBJECT_ARRAY` → `FLAT` after CP-2 gates pass. `OBJECT_ARRAY` stays opt-in via `withS1EntityState(OBJECT_ARRAY)`.
- `withS1EntityState(S1EntityStateType)` API signature unchanged. Consumer code that does not call `withS1EntityState` gets the faster path automatically.

### D10: Eager copy — no COW

S1 matches `strip-entity-state-cow`'s discipline:

```java
private S1FlatEntityState(S1FlatEntityState other) {
    this.layout = other.layout;
    this.data   = Arrays.copyOf(other.data, other.data.length);
    if (other.refs != null) {
        this.refs          = Arrays.copyOf(other.refs, other.refs.length);
        this.freeSlots     = Arrays.copyOf(other.freeSlots, other.freeSlots.length);
        this.freeSlotsTop  = other.freeSlotsTop;
    }
}
```

No owner, no writable flag, no `ensureXxxOwned`. Every write mutates in place. Per-copy allocation cost: one `byte[]` (up to ~236 KB for the worst-case STRING-heavy DTClass; typically <10 KB) + optional small refs + freeSlots. Dominated by `byte[]`. Accepted — matches the S2 FLAT post-strip cost model.

## Checkpoints and benches

**CP-0 — Baseline capture**
- Capture `EntityStateParseBench` on an old Dota 2 S1 replay against current master (`ObjectArrayEntityState` path). Wall-clock + `-prof gc` alloc. This is the pre-change reference.
- Precursor task: extend `EntityStateParseBench` (or add `S1EntityStateParseBench`) to accept a replay and run S1 end-to-end. Today's bench is S2-only.

**CP-1 — S1FlatLayout + S1FlatEntityState + tests**
- Land `S1FlatLayout` builder, `S1FlatEntityState` class, `S1EntityStateType.FLAT` variant (kept alongside `OBJECT_ARRAY` during the transition).
- Unit tests:
  - `S1FlatEntityStateTest.copyIsIndependent` — write to copy, verify source unchanged; write to source, verify copy unchanged.
  - `S1FlatEntityStateTest.primitiveRoundTrip` — for each `PrimitiveType` variant, write + read returns equal value.
  - `S1FlatEntityStateTest.inlineStringRoundTrip` — write via `decodeInto` on a synthetic `BitStream` with `StringLenDecoder` wire, read via `getValueForFieldPath`, compare to the same `StringLenDecoder.decode` on a parallel stream.
  - `S1FlatEntityStateTest.refSlotLifecycle` — ARRAY write allocates slot, overwrite reuses, `getValueForFieldPath` returns the current value.
  - `S1FlatEntityStateTest.fieldPathIteratorParity` — iterator yields 0..length-1 unconditionally, matching `ObjectArrayEntityState`.
- **Gate**: all unit tests green. `:repro:issue289Run`, `:repro:issue350Run` still green against `OBJECT_ARRAY` default (FLAT is opt-in at this CP).

**CP-2 — Port `S1FieldReader.readFields` to decode-direct**
- Fast-path rewrite per D8. Debug + materialize paths re-slotted to use the new state's `applyMutation(WriteValue)` / `write` as appropriate.
- Runner default flipped: `AbstractFileRunner.s1EntityStateType = S1EntityStateType.FLAT`.
- Parity gate: byte-for-byte event stream comparison on a medium S1 replay between `OBJECT_ARRAY` (precursor binary) and `FLAT` (new path). Use an event-writer harness that dumps `@OnEntityUpdated` firing order + property values to a file; diff against a captured pre-change baseline.
- **Gate**: `EntityStateParseBench` on the S1 replay — `≥ -20%` wall-clock; `≥ -50%` `-prof gc` alloc. No regression on `S2EntityStateParseBench` (orthogonal).

**CP-3 — Delete `ObjectArrayEntityState` + `S1EntityStateType.OBJECT_ARRAY`**
- After CP-2 gate passes, delete the old class and the enum variant. `S1EntityStateType.FLAT` is the sole option.
- Chase any stragglers (tests that named `ObjectArrayEntityState`, example code, dev tools).
- **Gate**: `./gradlew build` green; `:dev:dtinspectorRun` on an S1 replay compiles + parses cleanly.

**CP-4 — Downstream compatibility**
- `clarity-analyzer` — compile against modified clarity (composite build `includeBuild("../clarity")` on `next` branch). User-verified interactive-run gate if the user wishes to run it.
- `MutationTraceBench` — materialize + replay round-trip on an S1 replay: no exceptions, byte-for-byte parity.
- **Gate**: both pass. Full `./gradlew build` green.

**CP-5 — Final validation + RESULTS.md**
- Consolidate all bench numbers + acceptance checklist into `RESULTS.md` (matches `accelerate-flat-entity-state` convention).
- `openspec validate accelerate-s1-flat-state --strict` → passes.
- `./gradlew build` → green.

## Risks / Trade-offs

- **Risk**: S1 STRING worst-case layout footprint (~236 KB per DTClass) is large in absolute terms. → Mitigation: `dataBytes` is bounded and known at DTClass build time; log it during layout construction for any DTClass above a threshold (e.g. 64 KB) so regressions are visible. Typical DTClasses are under 10 KB.

- **Risk**: `new String(data, off, len, UTF_8)` at read time is allocated per `getValueForFieldPath` call. Heavy string-reading workloads (an event handler that reads every STRING prop every packet) could regress. → Mitigation: same as the S2 accepted trade-off. Follow-up: cached-String companion if profiles show it.

- **Risk**: Parity test between OBJECT_ARRAY and FLAT event streams could hide bugs if the harness silently tolerates differences. → Mitigation: dump to a deterministic text format, diff with `diff -q`, fail the gate on any byte. Harness shape lifted from S2 `SmokeTraceMain`.

- **Risk**: ARRAY decoder `getPrimitiveType()` returns null (correct — it produces `Object[]`), so REF is the only routing outcome. If a future ARRAY whose inner type is inlinable slips through, it would silently stay on the boxing path. → Mitigation: this change doesn't inline arrays at all; the follow-up (listed below) will audit `SendProp.template.type` before attempting inlining. No hidden regression.

- **Risk**: `S1DTClass.getFlatLayout()` is built lazily; concurrent first-access from multiple threads could double-build. → Mitigation: S1 DT classes are assembled once at replay startup, well before concurrent reads begin. Build eagerly from `S1DTClass` constructor / initialization hook, or use `computeIfAbsent` on an internal field. Pick during implementation.

- **Trade-off**: `S1FlatEntityState` does NOT extend any shared abstract (no equivalent of `AbstractS2EntityState`). S1 has no pointer-serializer tracking, no name-resolution that needs state lookup (name → fieldpath is on `S1DTClass.propsByName`). Keeping it flat-standalone avoids dragging S2 abstractions into S1. Accepted.

- **Trade-off**: `fieldPathIterator` yields every prop unconditionally (not filtered on flag byte). Matches current semantic; avoids behaviour diff in consumers. Could be tightened later.

- **Trade-off**: Keeping `S1EntityStateType` as a single-variant enum is arguably dead-weight. Kept for runner-API symmetry with `S2EntityStateType` and to avoid a user-visible API removal. Removing `withS1EntityState(...)` entirely is orthogonal cleanup — out of scope here to keep the diff focused.

## Future follow-ups (not in this change)

- **Inline S1 ARRAY props** — audit `SendProp.template.type` for the 11 ARRAY props via an extended `s1sendtables` dumper. If all inner types are primitive and the `numElements` metadata matches the wire, add `ArrayDecoder.decodeInto` reserving `numElements × innerSize + countPrefix` bytes and composing inner `decodeInto` per element. Drops the S1 refs slab; `S1FlatEntityState` reduces to `byte[] data + layout`. Previously tracked in `accelerate-flat-entity-state` follow-ups; restated here for visibility.

- **Cached-String companion** — lazy cache on inline-string leaves if read-time `new String(...)` shows up as a hotspot. Same shape as the S2 follow-up.

- **Delete `S1EntityStateType` / `withS1EntityState`** — single-variant enum + runner API are dead weight once `ObjectArrayEntityState` is gone. Can be batched with any future S1 state-type experiment or done as a one-line cleanup.

- **Tighten `fieldPathIterator` contract** — yield only populated slots (flag byte == 1) once downstream consumers are audited. Separate change; needs a brief consumer-compatibility review.

## Open Questions

- Should `S1FlatLayout` be built eagerly from the `S1DTClass` constructor or lazily on first access via `getFlatLayout()`? Either works — eager is simpler and DT class count is small. Defer decision to implementation.

- Should `S1EntityStateType.FLAT.createState` take `S1DTClass` or `ReceiveProp[]` + pre-computed layout? `S1DTClass` is cleaner (layout + props both reachable from one argument), but involves a small ctor signature change. Pick during implementation; no spec impact.
