## MODIFIED Requirements

### Requirement: FlatEntityState extends AbstractS2EntityState with global refs and lean Entry

The system SHALL provide a `FlatEntityState` class extending `AbstractS2EntityState`. It SHALL own a global refs container (shared across all nesting levels) and a tree of `Entry` instances that each hold only a `byte[]`. This mirrors `NestedArrayEntityState`'s pattern of a global `List<Entry> entries` with a `Deque<Integer> freeEntries` free-list (NestedArrayEntityState.java:17-18, 142-164).

`FlatEntityState` inherits from `AbstractS2EntityState`:
- `rootField` (SerializerField) — for field path name resolution and field navigation
- `pointerSerializers[]` — global, flat pointer serializer tracking (shared across all nesting levels)
- `getFieldForFieldPath()`, `getNameForFieldPath()`, `getFieldPathForName()`, `getTypeForFieldPath()` — operate on the Field hierarchy

`FlatEntityState` additionally holds:
- `Object[] refs` + `int refsSize` — global container for **sub-Entry instances only** (Strings are stored inline in the composite byte[] via the inline-string leaf shape; the mixed Strings-and-sub-Entries design is removed). `refsSize` is the logical length; grow via `Arrays.copyOf` when `refsSize == refs.length`
- `int[] freeSlots` + `int freeSlotsTop` — global free-list stack for refs recycling
- `Entry rootEntry` — the root Entry containing the flat primitive storage (including inline-string bytes)

The inner `Entry` class is a pure byte[] container. It holds:
- `FieldLayout rootLayout` — the layout tree for this Entry's scope
- `byte[] data` — primitive value bytes and slot-indices for refs/sub-states

Sub-states for VectorField and PointerField are `Entry` instances living in `FlatEntityState.refs` at dynamically-allocated slot indices. They have NO refs of their own — all String values and nested sub-Entries across all nesting levels live in the single `FlatEntityState.refs` container. They do NOT carry their own pointerSerializers — pointer tracking is global on the outer `FlatEntityState`.

There SHALL be no owner-pointer or modifiable-flag machinery on `FlatEntityState`, `Entry`, `refs`, `freeSlots`, or `pointerSerializers`. Every mutation path writes directly; `copy()` allocates an independent state graph up front (see `FlatEntityState.copy() is an eager deep copy`).

#### Scenario: Sub-states are Entry instances in FlatEntityState.refs

- **WHEN** a VectorField or PointerField sub-state is created
- **THEN** an `Entry` instance is created with its own `byte[] data` and `rootLayout`
- **AND** a slot is allocated in `FlatEntityState.refs` via `allocateRefSlot()`
- **AND** the Entry is stored at that slot via `refs` write
- **AND** the slot-index is stored in the parent Entry's `byte[]` at the SubState's offset+1
- **AND** the SubState's flag byte (at offset) is set to 1

#### Scenario: Dynamic ref slot allocation

- **WHEN** a WriteValue on a Ref is applied and the flag byte was 0
- **THEN** a slot is allocated from `FlatEntityState.refs` via `allocateRefSlot()` (reusing a free slot or appending)
- **AND** the slot-index is written to `data[offset+1]` via `INT_VH.set`
- **AND** the flag byte is set to 1
- **AND** the refs slab stores the value at that slot
- **WHEN** a WriteValue on a Ref is applied and the flag byte was 1
- **THEN** the existing slot-index is read from `data[offset+1]`
- **AND** the refs slab updates the value in place (no new allocation)

#### Scenario: Ref slot freed on null write

- **WHEN** a WriteValue with `value == null` is applied to a Ref with flag=1
- **THEN** the slot-index is read, `freeRefSlot(slot)` marks the slot reusable, and the flag is cleared

### Requirement: FlatEntityState dispatches on StateMutation via applyMutation

`applyMutation(FieldPath, StateMutation)` SHALL traverse the FieldLayout tree using a `base` accumulator, a `layout` cursor, and a `current` Entry reference. When a SubState is encountered mid-traversal, the loop SHALL read the slot-index from `current.data[base + s.offset + 1]`, look up the sub-Entry in `FlatEntityState.refs`, swap `current` to the sub-Entry, and `continue` to reprocess the current FieldPath index. Descent through a SubState consumes no fp index.

The SubState branch MUST NOT include its own `if (i == last) break` check. SubState-as-leaf operations (`SwitchPointer`, `ResizeVector`, or any other op targeting the SubState position itself) reach the dispatch via the Composite/Array branch advancing `layout = SubState` on the last idx and the loop's terminal break. Adding a break inside the SubState branch would prevent the final descent for transitional `WriteValue` operations whose leaf lives inside the sub-Entry.

**Lazy sub-Entry creation:** When the flag byte at `base + s.offset` is 0 mid-traversal, `applyMutation` SHALL lazy-create the sub-Entry to mirror `NestedArrayEntityState`'s implicit creation:
- **Pointer with `serializers.length == 1`**: create sub-Entry with `layouts[0]` / `layoutBytes[0]`, allocate slot, set flag, set `pointerSerializers[pointerId] = serializers[0]`.
- **Pointer with `serializers.length > 1`**: throw — the protocol must emit `SwitchPointer` before any inner write.
- **Vector**: create sub-Entry sized to fit `nextIdx + 1` elements (`Array(0, elementBytes, nextIdx+1, elementLayout)`).

After any descent through `SubState(Vector)`, if the sub-Entry's `Array.length < nextIdx + 1`, `applyMutation` SHALL grow the sub-Entry via byte[] reallocation and update its `rootLayout` with the new length. This mirrors `NestedArrayEntityState.ensureNodeCapacity`'s `idx + 1` fallback.

At the leaf, the method SHALL dispatch on the `StateMutation` type:

```java
public boolean applyMutation(FieldPath fpX, StateMutation op) {
    var fp = fpX.s2();
    Entry current = this.rootEntry;
    FieldLayout layout = current.rootLayout;
    int base = 0;
    var last = fp.last();

    int i = 0;
    while (true) {
        var idx = fp.get(i);
        switch (layout) {
            case Composite c -> layout = c.children[idx];
            case Array a     -> { base += a.baseOffset + idx * a.stride; layout = a.element; }
            case SubState s  -> {
                if (i == last) break;  // SubState-as-leaf → structural op below
                int slot = (int) INT_VH.get(current.data, base + s.offset + 1);
                Entry sub = (Entry) refs[slot];
                current = sub;
                layout = sub.rootLayout;
                base = 0;
                continue;
            }
            default -> throw new IllegalStateException();
        }
        if (i == last) break;
        i++;
    }

    return switch (op) {
        case StateMutation.WriteValue wv     -> writeValue(current, layout, base, wv.value());
        case StateMutation.ResizeVector rv   -> resizeVector(current, layout, base, rv.count());
        case StateMutation.SwitchPointer sp  -> switchPointer(current, layout, base, sp);
    };
}

private boolean writeValue(Entry target, FieldLayout layout, int base, Object value) {
    switch (layout) {
        case Primitive p -> {
            byte[] data = target.data;
            int flagPos = base + p.offset;
            byte oldFlag = data[flagPos];
            boolean willSet = value != null;
            data[flagPos] = willSet ? (byte) 1 : (byte) 0;
            if (willSet) p.type().write(data, flagPos + 1, value);
            return (oldFlag != 0) ^ willSet;
        }
        case Ref r -> {
            byte[] data = target.data;
            int flagPos = base + r.offset;
            byte oldFlag = data[flagPos];
            if (value == null) {
                if (oldFlag == 0) return false;
                int slot = (int) INT_VH.get(data, flagPos + 1);
                freeRefSlot(slot);
                data[flagPos] = 0;
                return true;
            }
            int slot;
            if (oldFlag != 0) {
                slot = (int) INT_VH.get(data, flagPos + 1);
            } else {
                slot = allocateRefSlot();
                INT_VH.set(data, flagPos + 1, slot);
                data[flagPos] = 1;
            }
            refs[slot] = value;
            return oldFlag == 0;
        }
        default -> throw new IllegalStateException();
    }
}
```

#### Scenario: WriteValue on a Primitive field — capacity change on flag transition

- **WHEN** `applyMutation` receives a `WriteValue(non-null value)` and the leaf layout is a Primitive
- **THEN** the old flag byte is captured, the flag is set to 1, `PrimitiveType.write` writes the value at `offset+1`
- **AND** returns `true` iff the flag transitioned from 0 to 1 (new occupied path)
- **WHEN** `applyMutation` receives a `WriteValue(null)` and the leaf layout is a Primitive
- **THEN** the flag is set to 0; value bytes are not touched
- **AND** returns `true` iff the flag transitioned from 1 to 0

#### Scenario: WriteValue on a Ref field — dynamic slot allocation

- **WHEN** `applyMutation` receives a `WriteValue(non-null value)` and the leaf is a Ref with flag=0
- **THEN** `allocateRefSlot()` returns a new slot (from free-list or by appending to refs)
- **AND** `INT_VH.set(data, offset+1, slot)` writes the slot-index
- **AND** flag is set to 1
- **AND** `refs[slot] = value` stores the value
- **AND** returns `true` (capacity changed)
- **WHEN** `applyMutation` receives a `WriteValue(non-null value)` and the leaf is a Ref with flag=1
- **THEN** the existing slot is read from `data[offset+1]` and `refs[slot] = value` updates in place
- **AND** returns `false`
- **WHEN** `applyMutation` receives a `WriteValue(null)` and the leaf is a Ref with flag=1
- **THEN** `freeRefSlot(slot)` releases the slot and flag is set to 0
- **AND** returns `true`

#### Scenario: ResizeVector on a SubState(Vector)

- **WHEN** `applyMutation` receives a `ResizeVector(count)` at a SubState(Vector) leaf with no existing sub-Entry (flag=0)
- **AND** count > 0
- **THEN** a sub-Entry with `byte[count * elementBytes]` is created, a slot is allocated in `refs`, the slot-index and flag are stored in the parent's byte[]
- **AND** returns `false` (no occupied paths existed in the dropped tail; the fresh sub-Entry has no occupied paths)
- **WHEN** a sub-Entry exists and `newCount != oldCount`
- **THEN** a new `byte[newCount * elementBytes]` is allocated, existing data is copied up to `min(oldCount, newCount) * elementBytes`, and `sub.data` is replaced with the new array
- **AND** `sub.rootLayout` is replaced with a new `Array` record carrying the new length
- **AND** on shrink, dropped tail slot-indices are orphaned in refs (no recursive cleanup — matches `NestedArrayEntityState.clearEntryRef`)
- **AND** returns `true` iff any occupied path existed in the dropped tail `[newCount, oldCount)` (grow → false, shrink with only-empty tail → false)
- **WHEN** `newCount == oldCount`
- **THEN** no mutation occurs and returns `false`

#### Scenario: SwitchPointer on a SubState(Pointer)

- **WHEN** `applyMutation` receives a `SwitchPointer(newSerializer)` at a SubState(Pointer) leaf
- **THEN** it reads `pointerId` from `SubStateKind.Pointer.pointerId()`
- **AND** if flag was 1 AND (`newSerializer == null` OR `pointerSerializers[pointerId] != newSerializer`): the direct slot is freed via `freeRefSlot` (nested slots are orphaned), flag cleared, `pointerSerializers[pointerId] = null`
- **AND** if `newSerializer != null` AND flag is now 0: a new sub-Entry is created with the pre-computed layout and bytes for `newSerializer`, a slot is allocated, slot-index and flag stored in parent's byte[], `pointerSerializers[pointerId] = newSerializer`
- **AND** returns `true` iff the cleared old sub-Entry contained any occupied paths (fresh creation or switch-to-same-serializer → false)

#### Scenario: Traversal through nested Composites (sub-serializer access)

- **WHEN** `applyMutation` is called with FieldPath `[2, 1]` where children[2] is a Composite (sub-serializer)
- **THEN** i=0 advances layout to `Composite.children[2]`, base stays 0
- **AND** i=1 advances layout to the inner `Composite.children[1]` (a Primitive)
- **AND** the WriteValue is applied at `base(0) + offset` using the Primitive's PrimitiveType

#### Scenario: Traversal through Array (fixed-length array access)

- **WHEN** `applyMutation` is called with FieldPath `[3, 7]` where children[3] is an Array(baseOffset=33, stride=5)
- **THEN** i=0 advances layout to the Array
- **AND** i=1 computes `base = 0 + 33 + 7*5 = 68`, layout = element Primitive(offset=0)
- **AND** the WriteValue is applied at `base(68) + offset(0) = 68`

#### Scenario: Traversal through SubState with context switch

- **WHEN** `applyMutation` is called with FieldPath `[4, 2]` where children[4] is SubState(offset=O)
- **THEN** i=0 advances layout to SubState via Composite
- **AND** i=1 encounters SubState, reads `slot = INT_VH.get(current.data, base+O+1)`, `sub = (Entry) refs[slot]`, swaps `current=sub, layout=sub.rootLayout, base=0`, and `continue`s
- **AND** i=1 is reprocessed with the sub-Entry's Array layout: `base = 0 + 2*stride`, layout = element
- **AND** the WriteValue targets the sub-Entry's byte[]

#### Scenario: SubState as leaf (structural operation)

- **WHEN** `applyMutation` is called with a `ResizeVector` and FieldPath `[4]` where children[4] is SubState
- **THEN** the loop advances layout to SubState and breaks because `i == last`
- **AND** the ResizeVector op is dispatched with the SubState layout

### Requirement: Refs slot release is transitive

When `FlatEntityState` releases a `refs` slot that holds a sub-Entry, it SHALL also release every `FieldLayout.Ref` and `FieldLayout.SubState` slot transitively reachable through that Entry's `data` via its `rootLayout`, returning all of them to `freeSlots`. No sub-Entry or value-Ref SHALL remain in `refs` after its sole navigation path from the root has been removed.

The transitive release SHALL be triggered from both mutation primitives that remove navigation edges:

- `switchPointer` when an existing sub-Entry is replaced or cleared (the slot formerly occupied by the old sub-Entry)
- `resizeVector` shrink when the truncated tail of `sub.data` contains `FieldLayout.Ref` or `FieldLayout.SubState` slot indices

The walk SHALL read `data` byte arrays and mutate `this.refs` and `this.freeSlots` directly; no sharing check or clone step is required because `data`, `refs`, and `freeSlots` are owned outright by `this` after `copy()` performs eager deep cloning.

Plain `FieldLayout.Ref` leaves hold arbitrary `Object` values (not sub-Entries); their slots SHALL be freed non-recursively via `freeRefSlot`. `FieldLayout.SubState` slots hold sub-Entries and SHALL be released recursively.

#### Scenario: SwitchPointer releases the whole sub-Entry subtree

- **GIVEN** a pointer SubState whose current sub-Entry `E` has a `FieldLayout.Ref` at offset `r` storing slot `s1`, and a nested `FieldLayout.SubState` at offset `s` storing slot `s2` whose own sub-Entry contains further Ref/SubState slots
- **WHEN** `SwitchPointer` replaces or clears that pointer
- **THEN** the top-level slot of `E` is returned to `freeSlots`
- **AND** slot `s1` is returned to `freeSlots`
- **AND** slot `s2` and every further Ref/SubState slot reachable from its sub-Entry are returned to `freeSlots`

#### Scenario: ResizeVector shrink releases slots in the discarded tail

- **GIVEN** a vector sub-Entry whose element layout contains `FieldLayout.Ref` or `FieldLayout.SubState` positions, and element indices `[M..N-1]` are about to be dropped by a shrink from length `N` to `M`
- **WHEN** `resizeVector` applies the shrink
- **THEN** for each element index `i` in `[M..N-1]`, every occupied Ref or SubState slot reachable through that element is returned to `freeSlots` before `sub.data` is reallocated

### Requirement: FlatEntityState provides decodeInto for primitive decode-direct path

`FlatEntityState` SHALL provide a method `decodeInto(FieldPath fp, Decoder decoder, BitStream bs)` that traverses the FieldLayout tree to the leaf layout for `fp` and dispatches to the decoder's static `decodeInto` method, writing decoded bytes directly into the Entry's `byte[]` without producing an intermediate boxed `Object` or allocating a `StateMutation.WriteValue` record.

The traversal SHALL be identical in shape to `applyMutation`: Composite/Array/SubState cases walk the layout and accumulate `base`; SubState descent is a direct pointer-chase (`sub = (Entry) refs[slot]`) with no ownership check. Lazy sub-Entry creation (Pointer with `serializers.length == 1`, Vector sized to `nextIdx + 1`) and vector growth on traversal are identical to `applyMutation`'s behavior.

At the leaf:

- **If the leaf layout is `Primitive`**: `decodeInto` SHALL read the flag byte at `base + p.offset()`, set it to `(byte) 1`, and invoke `DecoderDispatch.decodeInto(bs, decoder, data, base + p.offset() + 1)`. It SHALL return `oldFlag == 0` (null → value capacity-change signal).
- **If the leaf layout is `Ref`**: `decodeInto` SHALL fall back to the boxing path — invoke `DecoderDispatch.decode(bs, decoder)` to produce an `Object`, then perform the equivalent of `WriteValue` on the Ref slot (flag byte, slot allocation if needed, store in refs slab). It SHALL return the capacity-change signal per the same rules as `applyMutation`/`WriteValue` on Ref.
- **If the leaf layout is `SubState`**: `decodeInto` SHALL throw `IllegalStateException` — structural mutations (`ResizeVector`, `SwitchPointer`) do not go through `decodeInto`; callers must use `applyMutation` with the appropriate `StateMutation` variant.

`decodeInto` SHALL NOT allocate any intermediate `StateMutation` record on the Primitive path. The autobox and record allocation that occur on `applyMutation(fp, WriteValue(decoded))` SHALL NOT occur on `decodeInto(fp, decoder, bs)` when the leaf is `Primitive`.

**Capacity-change semantics:** values decoded from the bitstream are never null. On the Primitive path, `decodeInto` returns true only for the null→value transition (`oldFlag == 0` before the write). It SHALL NOT produce value→null transitions; those arise exclusively via `WriteValue(null)` through `applyMutation`, or via structural mutations.

#### Scenario: Primitive decodeInto writes via VarHandle with no autobox

- **WHEN** `decodeInto(fp, intDecoder, bs)` is called and the leaf layout is `Primitive(offset, Scalar.INT)`
- **THEN** the flag byte at `base + offset` is set to 1
- **AND** `DecoderDispatch.decodeInto(bs, intDecoder, data, base + offset + 1)` dispatches to the decoder's static `decodeInto`, which reads the int from the bitstream and writes it via `INT_VH.set(data, base + offset + 1, value)`
- **AND** no `Integer` object is allocated
- **AND** no `StateMutation.WriteValue` record is allocated
- **AND** the method returns true if the old flag was 0, false otherwise

#### Scenario: Ref-typed decodeInto falls back to boxing path

- **WHEN** `decodeInto(fp, stringDecoder, bs)` is called and the leaf layout is `Ref(offset)`
- **THEN** the value is decoded via `DecoderDispatch.decode(bs, decoder)` producing a `String`
- **AND** the Ref write proceeds identically to `applyMutation(fp, WriteValue(string))`

#### Scenario: Structural leaf throws

- **WHEN** `decodeInto(fp, decoder, bs)` is called and the leaf layout is `SubState`
- **THEN** `IllegalStateException` is thrown
- **AND** callers are expected to invoke `applyMutation(fp, ResizeVector(...))` or `applyMutation(fp, SwitchPointer(...))` for structural leaves

## ADDED Requirements

### Requirement: FlatEntityState.copy() is an eager deep copy

`FlatEntityState.copy()` SHALL return a state that is fully independent of the original at the moment of return. No byte[] array, `refs` slot, `Entry` instance, `freeSlots` array, or `pointerSerializers` array SHALL be shared with the original after `copy()` returns. Subsequent mutations on either state SHALL NOT be observable from the other, without any additional per-write bookkeeping.

`copy()` SHALL:
1. Clone `pointerSerializers` via `Arrays.copyOf`.
2. Clone `refs` via `Arrays.copyOf(refs, refs.length)` (preserving slot indices at their original positions).
3. Clone `freeSlots` via `Arrays.copyOf(freeSlots, freeSlots.length)` and copy `freeSlotsTop`.
4. Clone `rootEntry` as a new `Entry` instance with `rootLayout` shared by reference (layout is immutable) and `data` cloned via `Arrays.copyOf`.
5. For each slot `i` in `0..refsSize-1`, if `refs[i]` is an `Entry` instance, replace the cloned `refs[i]` with a freshly cloned `Entry` (recursively cloning `data` and any descendant sub-Entries). Non-`Entry` slot values (refs holding plain values other than sub-Entries — none after inline-string migration, but defensive) are left as shared references.

Slot stability SHALL be preserved — every sub-Entry in the clone occupies the same slot index it occupied in the original.

`copy()` SHALL NOT walk the FieldLayout tree. The sub-Entry traversal walks `refs` directly; FieldLayout shape is not needed to enumerate reachable Entries because the `refs` slab is the single back-reference container for all sub-Entries.

#### Scenario: copy() returns fully independent state

- **WHEN** `copy()` is invoked on a FlatEntityState
- **THEN** the returned state's `rootEntry`, `rootEntry.data`, `refs`, `freeSlots`, `pointerSerializers`, and every sub-`Entry` reachable through `refs` are newly allocated
- **AND** no byte[] or array in the returned state is `==` to any byte[] or array in the original
- **AND** every mutation on the copy (primitive write, ref write, sub-state resize, pointer switch) leaves the original's observable state unchanged
- **AND** vice versa

#### Scenario: Slot indices are preserved across copy

- **WHEN** the original has a sub-Entry at `refs[k]` reachable via slot-index `k` in some parent Entry's byte[]
- **THEN** the copy has the sub-Entry clone at `refs[k]` reachable via the same slot-index `k` in the copy's cloned parent byte[]
- **AND** the byte[] slot-indices stored in data bytes do not need rewriting

#### Scenario: Subsequent writes do not cross-affect

- **WHEN** `copy()` is invoked and the copy is then mutated via `applyMutation`, `write`, `decodeInto`, or any structural mutation
- **THEN** no ownership check or clone-on-write operation occurs during the mutation — the write proceeds directly on the copy's independently-allocated data
- **AND** the original's state is bit-for-bit unchanged

## REMOVED Requirements

### Requirement: FlatEntityState supports two-axis copy-on-write

**Reason**: CP-6 of `accelerate-flat-entity-state` eliminated the packet-atomicity requirement that motivated owner-pointer COW. Analysis of actual `copy()` call sites (baseline CREATE/RECREATE, `ClientFrame.Capsule`, clarity-analyzer per-event snapshots) shows that every production snapshot is followed by live-side mutation, so COW relocates the clone into the parser's hot path rather than avoiding it. Eager `copy()` performs identical total clone work with no per-write owner-check tax, simpler code, and fewer invariants to preserve.

**Migration**: `FlatEntityState.copy()` becomes an eager deep copy (see `FlatEntityState.copy() is an eager deep copy`). The `Entry.owner`, `refsOwner`, `pointerSerializersOwner` fields are deleted. Callers of `makeWritable`, `rootEntryWritable`, `ensureRefsOwned`, and `ensureRefsModifiable` reduce to straight pointer-chase or direct slab mutation. No external API consumer is affected; `EntityState.copy()` continues to return an independent state (the independence was post-first-write under COW, immediate under eager — the stricter guarantee subsumes the former).
