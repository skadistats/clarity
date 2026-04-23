package skadistats.clarity.state.s2;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.decoder.DecoderDispatch;
import skadistats.clarity.io.decoder.StringLenDecoder;
import skadistats.clarity.io.decoder.StringZeroTerminatedDecoder;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.model.s2.S2FieldPathBuilder;
import skadistats.clarity.model.s2.Serializer;
import skadistats.clarity.model.s2.field.SerializerField;
import skadistats.clarity.state.EntityState;
import skadistats.clarity.state.FieldLayout;
import skadistats.clarity.state.PrimitiveType;
import skadistats.clarity.state.SparseStateDelta;
import skadistats.clarity.state.StateDelta;
import skadistats.clarity.state.StateMutation;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static skadistats.clarity.state.PrimitiveType.FLOAT_VH;
import static skadistats.clarity.state.PrimitiveType.INT_VH;
import static skadistats.clarity.state.PrimitiveType.LONG_VH;

public final class S2FlatEntityState extends S2EntityState {

    private static final Object[] EMPTY_REFS = {};
    private static final int[] EMPTY_FREE_SLOTS = {};

    private Object[] refs;
    private int refsSize;
    private int[] freeSlots;
    private int freeSlotsTop;
    private Entry rootEntry;

    public S2FlatEntityState(SerializerField rootField, int pointerCount,
                           FieldLayout rootLayout, int totalBytes) {
        super(rootField, pointerCount);
        this.refs = EMPTY_REFS;
        this.refsSize = 0;
        this.freeSlots = EMPTY_FREE_SLOTS;
        this.freeSlotsTop = 0;
        this.rootEntry = new Entry(rootLayout, new byte[totalBytes]);
    }

    private S2FlatEntityState(S2FlatEntityState other) {
        super(other);
        this.refs = other.refs.length == 0 ? EMPTY_REFS : other.refs.clone();
        this.refsSize = other.refsSize;
        this.freeSlots = other.freeSlots.length == 0 ? EMPTY_FREE_SLOTS : other.freeSlots.clone();
        this.freeSlotsTop = other.freeSlotsTop;
        for (var i = 0; i < refsSize; i++) {
            if (refs[i] instanceof Entry e) {
                refs[i] = new Entry(e.rootLayout, e.data.clone());
            }
        }
        this.rootEntry = new Entry(other.rootEntry.rootLayout, other.rootEntry.data.clone());
    }

    @Override
    public EntityState copy() {
        return new S2FlatEntityState(this);
    }

    @Override
    public boolean applyMutation(S2FieldPath fp, StateMutation mutation) {
        Entry current = rootEntry;
        FieldLayout layout = current.rootLayout;
        var base = 0;
        var last = fp.last();

        var i = 0;
        while (true) {
            var idx = fp.get(i);
            switch (layout) {
                case FieldLayout.Composite c -> layout = c.children()[idx];
                case FieldLayout.Array a -> {
                    base += a.baseOffset() + idx * a.stride();
                    layout = a.element();
                }
                case FieldLayout.SubState s -> {
                    // Descent through SubState consumes no fp index. SubState-as-leaf
                    // ops (SwitchPointer, ResizeVector) reach the dispatch via the
                    // Composite/Array branch advancing `layout = SubState` on the last
                    // idx and breaking — they never enter THIS branch.
                    if (current.data[base + s.offset()] == 0) {
                        lazyCreateSubEntry(current, base, s, idx);
                    }
                    var slot = (int) INT_VH.get(current.data, base + s.offset() + 1);
                    var sub = (Entry) refs[slot];
                    if (s.kind() instanceof FieldLayout.SubStateKind.Vector v) {
                        growVectorIfNeeded(sub, v, idx + 1);
                    }
                    current = sub;
                    layout = sub.rootLayout;
                    base = 0;
                    continue;
                }
                default -> throw new IllegalStateException("non-branch layout at non-leaf position: " + layout);
            }
            if (i == last) break;
            i++;
        }

        return switch (mutation) {
            case StateMutation.WriteValue wv          -> writeValue(current, layout, base, wv.value());
            case StateMutation.ResizeVector rv         -> resizeVector(current, layout, base, rv.count());
            case StateMutation.SwitchPolymorphicPointer sp        -> switchPolymorphicPointer(current, layout, base, sp.newSerializer());
            case StateMutation.SwitchFixedPointer sfp  -> {
                if (!(layout instanceof FieldLayout.SubState s)) {
                    throw new IllegalStateException("SwitchFixedPointer on non-substate layout: " + layout);
                }
                yield switchFixedPointer(current, s, base, sfp.serializer());
            }
        };
    }

    @Override
    public boolean decodeInto(S2FieldPath fp, Decoder decoder, BitStream bs) {
        Entry current = rootEntry;
        FieldLayout layout = current.rootLayout;
        var base = 0;
        var last = fp.last();

        var i = 0;
        while (true) {
            var idx = fp.get(i);
            switch (layout) {
                case FieldLayout.Composite c -> layout = c.children()[idx];
                case FieldLayout.Array a -> {
                    base += a.baseOffset() + idx * a.stride();
                    layout = a.element();
                }
                case FieldLayout.SubState s -> {
                    if (current.data[base + s.offset()] == 0) {
                        lazyCreateSubEntry(current, base, s, idx);
                    }
                    var slot = (int) INT_VH.get(current.data, base + s.offset() + 1);
                    var sub = (Entry) refs[slot];
                    if (s.kind() instanceof FieldLayout.SubStateKind.Vector v) {
                        growVectorIfNeeded(sub, v, idx + 1);
                    }
                    current = sub;
                    layout = sub.rootLayout;
                    base = 0;
                    continue;
                }
                default -> throw new IllegalStateException("non-branch layout at non-leaf position: " + layout);
            }
            if (i == last) break;
            i++;
        }

        return switch (layout) {
            case FieldLayout.Primitive p -> {
                var data = current.data;
                var flagPos = base + p.offset();
                var oldFlag = data[flagPos];
                data[flagPos] = 1;
                DecoderDispatch.decodeInto(bs, decoder, data, flagPos + 1);
                yield oldFlag == 0;
            }
            case FieldLayout.InlineString is -> {
                var data = current.data;
                var flagPos = base + is.offset();
                var oldFlag = data[flagPos];
                data[flagPos] = 1;
                if (decoder instanceof StringZeroTerminatedDecoder) {
                    StringZeroTerminatedDecoder.decodeIntoInline(bs, data, flagPos + 1, is.maxLength());
                } else {
                    StringLenDecoder.decodeIntoInline(bs, data, flagPos + 1, is.maxLength());
                }
                yield oldFlag == 0;
            }
            case FieldLayout.Ref r -> writeValue(current, r, base, DecoderDispatch.decode(bs, decoder));
            case FieldLayout.SubState s -> throw new IllegalStateException("decodeInto called on SubState leaf: " + s);
            default -> throw new IllegalStateException("decodeInto on unknown leaf layout: " + layout);
        };
    }

    @Override
    public boolean write(S2FieldPath fp, Object decoded) {
        Entry current = rootEntry;
        FieldLayout layout = current.rootLayout;
        var base = 0;
        var last = fp.last();

        var i = 0;
        while (true) {
            var idx = fp.get(i);
            switch (layout) {
                case FieldLayout.Composite c -> layout = c.children()[idx];
                case FieldLayout.Array a -> {
                    base += a.baseOffset() + idx * a.stride();
                    layout = a.element();
                }
                case FieldLayout.SubState s -> {
                    if (current.data[base + s.offset()] == 0) {
                        lazyCreateSubEntry(current, base, s, idx);
                    }
                    var slot = (int) INT_VH.get(current.data, base + s.offset() + 1);
                    var sub = (Entry) refs[slot];
                    if (s.kind() instanceof FieldLayout.SubStateKind.Vector v) {
                        growVectorIfNeeded(sub, v, idx + 1);
                    }
                    current = sub;
                    layout = sub.rootLayout;
                    base = 0;
                    continue;
                }
                default -> throw new IllegalStateException("non-branch layout at non-leaf position: " + layout);
            }
            if (i == last) break;
            i++;
        }

        return switch (layout) {
            case FieldLayout.Primitive p    -> writeValue(current, p, base, decoded);
            case FieldLayout.InlineString is -> writeValue(current, is, base, decoded);
            case FieldLayout.Ref r          -> writeValue(current, r, base, decoded);
            case FieldLayout.SubState s -> switch (s.kind()) {
                case FieldLayout.SubStateKind.PolymorphicPointer p      -> switchPolymorphicPointer(current, s, base, (Serializer) decoded);
                case FieldLayout.SubStateKind.FixedPointer fixedPtr -> switchFixedPointer(current, s, base, (Serializer) decoded);
                case FieldLayout.SubStateKind.Vector v        -> resizeVector(current, s, base, (Integer) decoded);
            };
            default -> throw new IllegalStateException("write on unknown leaf layout: " + layout);
        };
    }

    private boolean writeValue(Entry target, FieldLayout layout, int base, Object value) {
        return switch (layout) {
            case FieldLayout.Primitive p -> {
                var data = target.data;
                var flagPos = base + p.offset();
                var oldFlag = data[flagPos];
                var willSet = value != null;
                data[flagPos] = willSet ? (byte) 1 : (byte) 0;
                if (willSet) p.type().write(data, flagPos + 1, value);
                yield (oldFlag != 0) ^ willSet;
            }
            case FieldLayout.InlineString is -> {
                var data = target.data;
                var flagPos = base + is.offset();
                var oldFlag = data[flagPos];
                if (value == null) {
                    data[flagPos] = 0;
                    yield oldFlag != 0;
                }
                var bytes = ((String) value).getBytes(StandardCharsets.UTF_8);
                if (bytes.length > is.maxLength()) {
                    throw new IllegalStateException(
                        "String length " + bytes.length + " exceeds leaf maxLength " + is.maxLength());
                }
                data[flagPos] = 1;
                data[flagPos + 1] = (byte) (bytes.length & 0xFF);
                data[flagPos + 2] = (byte) ((bytes.length >>> 8) & 0xFF);
                System.arraycopy(bytes, 0, data, flagPos + 3, bytes.length);
                yield oldFlag == 0;
            }
            case FieldLayout.Ref r -> {
                var data = target.data;
                var flagPos = base + r.offset();
                var oldFlag = data[flagPos];
                if (value == null) {
                    if (oldFlag == 0) yield false;
                    var slot = (int) INT_VH.get(data, flagPos + 1);
                    freeRefSlot(slot);
                    data[flagPos] = 0;
                    yield true;
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
                yield oldFlag == 0;
            }
            default -> throw new IllegalStateException("WriteValue on non-leaf layout: " + layout);
        };
    }

    private boolean resizeVector(Entry current, FieldLayout layout, int base, int newCount) {
        if (!(layout instanceof FieldLayout.SubState s) || !(s.kind() instanceof FieldLayout.SubStateKind.Vector v)) {
            throw new IllegalStateException("ResizeVector on non-vector substate: " + layout);
        }
        var data = current.data;
        var flagPos = base + s.offset();
        if (data[flagPos] == 0) {
            if (newCount == 0) return false;
            var array = new FieldLayout.Array(0, v.elementBytes(), newCount, v.elementLayout());
            var sub = new Entry(array, new byte[newCount * v.elementBytes()]);
            var slot = allocateRefSlot();
            refs[slot] = sub;
            INT_VH.set(current.data, flagPos + 1, slot);
            current.data[flagPos] = 1;
            return false;
        }
        var slot = (int) INT_VH.get(data, flagPos + 1);
        var sub = (Entry) refs[slot];
        var oldArray = (FieldLayout.Array) sub.rootLayout;
        var oldCount = oldArray.length();
        if (oldCount == newCount) return false;
        var droppedOccupied = false;
        if (newCount < oldCount) {
            for (var i = newCount; i < oldCount && !droppedOccupied; i++) {
                droppedOccupied = hasAnyOccupiedPath(sub, v.elementLayout(), i * v.elementBytes());
            }
            for (var i = newCount; i < oldCount; i++) {
                releaseRefsInEntry(sub, v.elementLayout(), i * v.elementBytes());
            }
        }
        var newData = new byte[newCount * v.elementBytes()];
        System.arraycopy(sub.data, 0, newData, 0, Math.min(sub.data.length, newData.length));
        sub.data = newData;
        sub.rootLayout = new FieldLayout.Array(0, v.elementBytes(), newCount, v.elementLayout());
        return droppedOccupied;
    }

    private boolean switchPolymorphicPointer(Entry current, FieldLayout layout, int base, Serializer newSerializer) {
        if (!(layout instanceof FieldLayout.SubState s) || !(s.kind() instanceof FieldLayout.SubStateKind.PolymorphicPointer p)) {
            throw new IllegalStateException("SwitchPointer on non-pointer substate: " + layout);
        }
        var currentSerializer = pointerSerializers[p.pointerId()];
        if (currentSerializer == newSerializer) return false;
        var flagPos = base + s.offset();
        var hadSub = current.data[flagPos] != 0;
        var removedOccupied = false;

        if (hadSub) {
            var oldSlot = (int) INT_VH.get(current.data, flagPos + 1);
            var oldSub = (Entry) refs[oldSlot];
            removedOccupied = hasAnyOccupiedPath(oldSub, oldSub.rootLayout, 0);
            releaseRefSlot(oldSlot);
            current.data[flagPos] = 0;
            pointerSerializers[p.pointerId()] = null;
        }
        if (newSerializer != null) {
            var layoutIdx = lookupLayoutIndex(p, newSerializer);
            var sub = new Entry(p.layouts()[layoutIdx], new byte[p.layoutBytes()[layoutIdx]]);
            var slot = allocateRefSlot();
            refs[slot] = sub;
            INT_VH.set(current.data, flagPos + 1, slot);
            current.data[flagPos] = 1;
            pointerSerializers[p.pointerId()] = newSerializer;
        }
        return removedOccupied;
    }

    private boolean switchFixedPointer(Entry current, FieldLayout layout, int base, Serializer newSerializer) {
        if (!(layout instanceof FieldLayout.SubState s) || !(s.kind() instanceof FieldLayout.SubStateKind.FixedPointer fp)) {
            throw new IllegalStateException("SwitchFixedPointer on non-fixed-pointer substate: " + layout);
        }
        var flagPos = base + s.offset();
        var hadSub = current.data[flagPos] != 0;

        if (newSerializer != null) {
            if (hadSub) return false;
            var sub = new Entry(fp.layout(), new byte[fp.layoutBytes()]);
            var slot = allocateRefSlot();
            refs[slot] = sub;
            INT_VH.set(current.data, flagPos + 1, slot);
            current.data[flagPos] = 1;
            return false;
        } else {
            if (!hadSub) return false;
            var oldSlot = (int) INT_VH.get(current.data, flagPos + 1);
            var oldSub = (Entry) refs[oldSlot];
            var removedOccupied = hasAnyOccupiedPath(oldSub, oldSub.rootLayout, 0);
            releaseRefSlot(oldSlot);
            current.data[flagPos] = 0;
            return removedOccupied;
        }
    }

    private boolean hasAnyOccupiedPath(Entry entry, FieldLayout layout, int base) {
        return switch (layout) {
            case FieldLayout.Composite c -> {
                for (var child : c.children()) {
                    if (hasAnyOccupiedPath(entry, child, base)) yield true;
                }
                yield false;
            }
            case FieldLayout.Array a -> {
                for (var i = 0; i < a.length(); i++) {
                    if (hasAnyOccupiedPath(entry, a.element(), base + a.baseOffset() + i * a.stride())) yield true;
                }
                yield false;
            }
            case FieldLayout.Primitive p    -> entry.data[base + p.offset()] != 0;
            case FieldLayout.InlineString is -> entry.data[base + is.offset()] != 0;
            case FieldLayout.Ref r          -> entry.data[base + r.offset()] != 0;
            case FieldLayout.SubState s -> {
                if (entry.data[base + s.offset()] == 0) yield false;
                var slot = (int) INT_VH.get(entry.data, base + s.offset() + 1);
                var sub = (Entry) refs[slot];
                yield hasAnyOccupiedPath(sub, sub.rootLayout, 0);
            }
        };
    }

    private static int lookupLayoutIndex(FieldLayout.SubStateKind.PolymorphicPointer p, Serializer newSerializer) {
        var serializers = p.serializers();
        for (var i = 0; i < serializers.length; i++) {
            if (serializers[i] == newSerializer) return i;
        }
        throw new IllegalStateException("Serializer " + newSerializer + " not found in pointer serializers");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getValueForFieldPath(S2FieldPath fp) {
        Entry current = this.rootEntry;
        FieldLayout layout = current.rootLayout;
        var base = 0;
        var last = fp.last();

        var i = 0;
        while (true) {
            var idx = fp.get(i);
            switch (layout) {
                case FieldLayout.Composite c -> layout = c.children()[idx];
                case FieldLayout.Array a -> {
                    if (idx >= a.length()) return null;
                    base += a.baseOffset() + idx * a.stride();
                    layout = a.element();
                }
                case FieldLayout.SubState s -> {
                    if (current.data[base + s.offset()] == 0) return null;
                    var slot = (int) INT_VH.get(current.data, base + s.offset() + 1);
                    var sub = (Entry) refs[slot];
                    current = sub;
                    layout = sub.rootLayout;
                    base = 0;
                    continue;
                }
                default -> throw new IllegalStateException("non-branch layout at non-leaf position: " + layout);
            }
            if (i == last) break;
            i++;
        }

        return switch (layout) {
            case FieldLayout.Primitive p -> {
                if (current.data[base + p.offset()] == 0) yield null;
                yield (T) p.type().read(current.data, base + p.offset() + 1);
            }
            case FieldLayout.InlineString is -> {
                var data = current.data;
                var flagPos = base + is.offset();
                if (data[flagPos] == 0) yield null;
                var len = (data[flagPos + 1] & 0xFF) | ((data[flagPos + 2] & 0xFF) << 8);
                yield (T) new String(data, flagPos + 3, len, StandardCharsets.UTF_8);
            }
            case FieldLayout.Ref r -> {
                if (current.data[base + r.offset()] == 0) yield null;
                var slot = (int) INT_VH.get(current.data, base + r.offset() + 1);
                yield (T) refs[slot];
            }
            case FieldLayout.SubState s -> {
                // Hidden composite terminals: return what `write` accepts, so
                // write(fp, get(fp)) is a semantic no-op. captureChanged /
                // applyFrom rely on this round-trip.
                var flagPos = base + s.offset();
                var isSet = current.data[flagPos] != 0;
                yield switch (s.kind()) {
                    case FieldLayout.SubStateKind.Vector v -> {
                        if (!isSet) yield (T) (Integer) 0;
                        var slot = (int) INT_VH.get(current.data, flagPos + 1);
                        var sub = (Entry) refs[slot];
                        yield (T) (Integer) ((FieldLayout.Array) sub.rootLayout).length();
                    }
                    case FieldLayout.SubStateKind.FixedPointer fixed -> (T) (isSet ? fixed.serializer() : null);
                    case FieldLayout.SubStateKind.PolymorphicPointer poly -> (T) (isSet ? pointerSerializers[poly.pointerId()] : null);
                };
            }
            default -> null;
        };
    }

    @Override
    public Iterator<FieldPath> fieldPathIterator() {
        var out = new ArrayList<FieldPath>();
        walk(rootEntry, rootEntry.rootLayout, 0, S2FieldPath.newBuilder(), 0, out);
        return out.iterator();
    }

    private void walk(Entry entry, FieldLayout layout, int base, S2FieldPathBuilder fp, int depth, List<FieldPath> out) {
        switch (layout) {
            case FieldLayout.Composite c -> {
                var children = c.children();
                if (depth > 0) fp.down();
                for (var i = 0; i < children.length; i++) {
                    fp.set(depth, i);
                    walk(entry, children[i], base, fp, depth + 1, out);
                }
                if (depth > 0) fp.up(1);
            }
            case FieldLayout.Array a -> {
                if (depth > 0) fp.down();
                for (var i = 0; i < a.length(); i++) {
                    fp.set(depth, i);
                    walk(entry, a.element(), base + a.baseOffset() + i * a.stride(), fp, depth + 1, out);
                }
                if (depth > 0) fp.up(1);
            }
            case FieldLayout.Primitive p -> {
                if (entry.data[base + p.offset()] != 0) out.add(fp.snapshot());
            }
            case FieldLayout.InlineString is -> {
                if (entry.data[base + is.offset()] != 0) out.add(fp.snapshot());
            }
            case FieldLayout.Ref r -> {
                if (entry.data[base + r.offset()] != 0) out.add(fp.snapshot());
            }
            case FieldLayout.SubState s -> {
                if (entry.data[base + s.offset()] != 0) {
                    var slot = (int) INT_VH.get(entry.data, base + s.offset() + 1);
                    var sub = (Entry) refs[slot];
                    walk(sub, sub.rootLayout, 0, fp, depth, out);
                }
            }
        }
    }

    /**
     * Lazily create a sub-Entry when descending through an uninitialized SubState.
     * S2NestedArrayEntityState does this implicitly via untyped Object[] storage.
     * For FLAT we need a concrete layout, so this only works for cases where the
     * layout is unambiguous: Pointer with a single (default) serializer.
     * For ambiguous Pointers and Vectors, the protocol is expected to emit
     * SwitchPointer / ResizeVector before any inner write.
     */
    private void lazyCreateSubEntry(Entry parent, int base, FieldLayout.SubState s, int hintIdx) {
        Entry sub = switch (s.kind()) {
            case FieldLayout.SubStateKind.PolymorphicPointer p -> {
                if (p.serializers().length != 1) {
                    throw new IllegalStateException(
                        "cannot lazy-create sub-Entry for Pointer with " + p.serializers().length
                        + " serializers (expected explicit SwitchPointer first), pointerId=" + p.pointerId());
                }
                pointerSerializers[p.pointerId()] = p.serializers()[0];
                yield new Entry(p.layouts()[0], new byte[p.layoutBytes()[0]]);
            }
            case FieldLayout.SubStateKind.FixedPointer fp ->
                new Entry(fp.layout(), new byte[fp.layoutBytes()]);
            case FieldLayout.SubStateKind.Vector v -> {
                // Lazy-create vector sized to fit the upcoming element index.
                // Mirrors S2NestedArrayEntityState's auto-growing capacity on writes.
                var length = hintIdx + 1;
                var array = new FieldLayout.Array(0, v.elementBytes(), length, v.elementLayout());
                yield new Entry(array, new byte[length * v.elementBytes()]);
            }
        };
        var slot = allocateRefSlot();
        refs[slot] = sub;
        INT_VH.set(parent.data, base + s.offset() + 1, slot);
        parent.data[base + s.offset()] = 1;
    }

    /**
     * Grow a vector sub-Entry to fit at least `requiredLength` elements.
     * Mirrors S2NestedArrayEntityState's capacity-extension behavior on writes.
     */
    private static void growVectorIfNeeded(Entry sub, FieldLayout.SubStateKind.Vector v, int requiredLength) {
        var array = (FieldLayout.Array) sub.rootLayout;
        if (array.length() >= requiredLength) return;
        var newData = new byte[requiredLength * v.elementBytes()];
        System.arraycopy(sub.data, 0, newData, 0, sub.data.length);
        sub.data = newData;
        sub.rootLayout = new FieldLayout.Array(0, v.elementBytes(), requiredLength, v.elementLayout());
    }

    private int allocateRefSlot() {
        if (freeSlotsTop > 0) {
            return freeSlots[--freeSlotsTop];
        }
        if (refsSize == refs.length) {
            var newCap = refs.length == 0 ? 8 : refs.length * 2;
            refs = Arrays.copyOf(refs, newCap);
        }
        return refsSize++;
    }

    private void freeRefSlot(int slot) {
        refs[slot] = null;
        if (freeSlotsTop == freeSlots.length) {
            var newCap = freeSlots.length == 0 ? 8 : freeSlots.length * 2;
            freeSlots = Arrays.copyOf(freeSlots, newCap);
        }
        freeSlots[freeSlotsTop++] = slot;
    }

    private void releaseRefSlot(int slot) {
        if (refs[slot] instanceof Entry e) {
            releaseRefsInEntry(e, e.rootLayout, 0);
        }
        freeRefSlot(slot);
    }

    private void releaseRefsInEntry(Entry e, FieldLayout layout, int base) {
        switch (layout) {
            case FieldLayout.Composite c -> {
                for (var child : c.children()) {
                    releaseRefsInEntry(e, child, base);
                }
            }
            case FieldLayout.Array a -> {
                for (var i = 0; i < a.length(); i++) {
                    releaseRefsInEntry(e, a.element(), base + a.baseOffset() + i * a.stride());
                }
            }
            case FieldLayout.Ref r -> {
                if (e.data[base + r.offset()] != 0) {
                    var innerSlot = (int) INT_VH.get(e.data, base + r.offset() + 1);
                    freeRefSlot(innerSlot);
                }
            }
            case FieldLayout.SubState s -> {
                if (e.data[base + s.offset()] != 0) {
                    var innerSlot = (int) INT_VH.get(e.data, base + s.offset() + 1);
                    releaseRefSlot(innerSlot);
                }
            }
            case FieldLayout.Primitive p -> { /* primitives live inline — no refs to release */ }
            case FieldLayout.InlineString is -> { /* inline-strings live inline — no refs to release */ }
        }
    }

    public int slabSize() {
        return refsSize;
    }

    public int freeSlotCount() {
        return freeSlotsTop;
    }

    public byte[] rootDataForTest() {
        return rootEntry.data;
    }

    public Object[] refsArrayForTest() {
        return refs;
    }

    public Serializer[] pointerSerializersForTest() {
        return pointerSerializers;
    }

    byte[] subEntryDataForTest(int slot) {
        return ((Entry) refs[slot]).data;
    }

    // -------------------------------------------------------------------
    // Primitive read accessors
    // -------------------------------------------------------------------

    @Override
    public int getInt(S2FieldPath fp) {
        var loc = navigate(fp);
        if (loc == null) return 0;
        if (loc.layout instanceof FieldLayout.Primitive p && p.type() == PrimitiveType.Scalar.INT) {
            if (loc.entry.data[loc.base + p.offset()] == 0) return 0;
            return (int) INT_VH.get(loc.entry.data, loc.base + p.offset() + 1);
        }
        return 0;
    }

    @Override
    public long getLong(S2FieldPath fp) {
        var loc = navigate(fp);
        if (loc == null) return 0L;
        if (loc.layout instanceof FieldLayout.Primitive p && p.type() == PrimitiveType.Scalar.LONG) {
            if (loc.entry.data[loc.base + p.offset()] == 0) return 0L;
            return (long) LONG_VH.get(loc.entry.data, loc.base + p.offset() + 1);
        }
        return 0L;
    }

    @Override
    public float getFloat(S2FieldPath fp) {
        var loc = navigate(fp);
        if (loc == null) return 0.0f;
        if (loc.layout instanceof FieldLayout.Primitive p && p.type() == PrimitiveType.Scalar.FLOAT) {
            if (loc.entry.data[loc.base + p.offset()] == 0) return 0.0f;
            return (float) FLOAT_VH.get(loc.entry.data, loc.base + p.offset() + 1);
        }
        return 0.0f;
    }

    @Override
    public Object getObject(S2FieldPath fp) {
        return getValueForFieldPath(fp);
    }

    // -------------------------------------------------------------------
    // Sparse capture / apply
    // -------------------------------------------------------------------

    @Override
    public StateDelta captureChanged(S2FieldPath[] fps, int num) {
        var delta = new SparseStateDelta(num, true);
        for (var i = 0; i < num; i++) {
            var fp = fps[i];
            captureOne(delta, i, fp);
        }
        return delta;
    }

    private void captureOne(SparseStateDelta delta, int i, S2FieldPath fp) {
        var loc = navigate(fp);
        if (loc == null) {
            delta.putEmpty(i, fp);
            return;
        }
        switch (loc.layout) {
            case FieldLayout.Primitive p -> {
                if (loc.entry.data[loc.base + p.offset()] == 0) {
                    delta.putEmpty(i, fp);
                    return;
                }
                var dataOff = loc.base + p.offset() + 1;
                if (p.type() == PrimitiveType.Scalar.INT) {
                    delta.putInt(i, fp, (int) INT_VH.get(loc.entry.data, dataOff));
                } else if (p.type() == PrimitiveType.Scalar.LONG) {
                    delta.putLong(i, fp, (long) LONG_VH.get(loc.entry.data, dataOff));
                } else if (p.type() == PrimitiveType.Scalar.FLOAT) {
                    delta.putFloat(i, fp, (float) FLOAT_VH.get(loc.entry.data, dataOff));
                } else {
                    delta.putObject(i, fp, p.type().read(loc.entry.data, dataOff));
                }
            }
            case FieldLayout.InlineString is -> {
                if (loc.entry.data[loc.base + is.offset()] == 0) {
                    delta.putEmpty(i, fp);
                    return;
                }
                delta.putObject(i, fp, readInlineString(loc.entry.data, loc.base + is.offset()));
            }
            case FieldLayout.Ref r -> {
                var flag = loc.entry.data[loc.base + r.offset()];
                if (flag == 0) {
                    delta.putEmpty(i, fp);
                    return;
                }
                var slot = (int) INT_VH.get(loc.entry.data, loc.base + r.offset() + 1);
                delta.putObject(i, fp, refs[slot]);
            }
            case FieldLayout.SubState s -> {
                // Hidden composite terminal: capture the value the symmetric
                // `write`/structural-mutation accepts. Vector → current length;
                // FixedPointer → field.serializer()|null; PolymorphicPointer →
                // currently-resolved serializer|null.
                var flagPos = loc.base + s.offset();
                var isSet = loc.entry.data[flagPos] != 0;
                switch (s.kind()) {
                    case FieldLayout.SubStateKind.Vector v -> {
                        if (!isSet) {
                            delta.putInt(i, fp, 0);
                        } else {
                            var slot = (int) INT_VH.get(loc.entry.data, flagPos + 1);
                            var sub = (Entry) refs[slot];
                            delta.putInt(i, fp, ((FieldLayout.Array) sub.rootLayout).length());
                        }
                    }
                    case FieldLayout.SubStateKind.FixedPointer fixed -> {
                        delta.putObject(i, fp, isSet ? fixed.serializer() : null);
                    }
                    case FieldLayout.SubStateKind.PolymorphicPointer poly -> {
                        delta.putObject(i, fp, isSet ? pointerSerializers[poly.pointerId()] : null);
                    }
                }
            }
            default -> delta.putEmpty(i, fp);
        }
    }

    private static String readInlineString(byte[] data, int flagPos) {
        var len = (data[flagPos + 1] & 0xFF) | ((data[flagPos + 2] & 0xFF) << 8);
        return new String(data, flagPos + 3, len, StandardCharsets.UTF_8);
    }

    @Override
    public void applyFrom(StateDelta delta, S2FieldPath fp) {
        if (!(delta instanceof SparseStateDelta sd)) {
            throw new IllegalArgumentException("applyFrom requires a SparseStateDelta");
        }
        var i = sd.indexOf(fp);
        if (i < 0) return;
        applySlot(sd, i, fp);
    }

    @Override
    public void applyAll(StateDelta delta) {
        if (!(delta instanceof SparseStateDelta sd)) {
            throw new IllegalArgumentException("applyAll requires a SparseStateDelta");
        }
        var fields = sd.fields();
        for (var i = 0; i < fields.length; i++) {
            var fp = fields[i];
            if (fp == null) continue;
            applySlot(sd, i, (S2FieldPath) fp);
        }
    }

    private void applySlot(SparseStateDelta sd, int i, S2FieldPath fp) {
        var tag = sd.tagAt(i);
        var loc = navigate(fp);
        if (loc == null) return;
        if (tag == SparseStateDelta.TAG_EMPTY) {
            applyClear(loc);
            return;
        }
        switch (loc.layout) {
            case FieldLayout.Primitive p -> {
                var dataOff = loc.base + p.offset() + 1;
                switch (tag) {
                    case SparseStateDelta.TAG_INT -> {
                        if (p.type() == PrimitiveType.Scalar.INT) {
                            INT_VH.set(loc.entry.data, dataOff, (int) sd.primAt(i));
                            loc.entry.data[loc.base + p.offset()] = 1;
                        }
                    }
                    case SparseStateDelta.TAG_LONG -> {
                        if (p.type() == PrimitiveType.Scalar.LONG) {
                            LONG_VH.set(loc.entry.data, dataOff, sd.primAt(i));
                            loc.entry.data[loc.base + p.offset()] = 1;
                        }
                    }
                    case SparseStateDelta.TAG_FLOAT -> {
                        if (p.type() == PrimitiveType.Scalar.FLOAT) {
                            FLOAT_VH.set(loc.entry.data, dataOff, Float.intBitsToFloat((int) sd.primAt(i)));
                            loc.entry.data[loc.base + p.offset()] = 1;
                        }
                    }
                    case SparseStateDelta.TAG_OBJECT -> {
                        var value = sd.objAt(i);
                        if (value != null) {
                            p.type().write(loc.entry.data, dataOff, value);
                            loc.entry.data[loc.base + p.offset()] = 1;
                        }
                    }
                    default -> { /* unreachable */ }
                }
            }
            case FieldLayout.InlineString is -> {
                if (tag != SparseStateDelta.TAG_OBJECT) return;
                writeValue(loc.entry, is, loc.base, sd.objAt(i));
            }
            case FieldLayout.Ref r -> {
                if (tag != SparseStateDelta.TAG_OBJECT) return;
                writeValue(loc.entry, r, loc.base, sd.objAt(i));
            }
            case FieldLayout.SubState s -> {
                switch (s.kind()) {
                    case FieldLayout.SubStateKind.Vector v -> {
                        if (tag == SparseStateDelta.TAG_INT) {
                            resizeVector(loc.entry, s, loc.base, (int) sd.primAt(i));
                        }
                    }
                    case FieldLayout.SubStateKind.FixedPointer fixed -> {
                        if (tag == SparseStateDelta.TAG_OBJECT) {
                            switchFixedPointer(loc.entry, s, loc.base, (Serializer) sd.objAt(i));
                        }
                    }
                    case FieldLayout.SubStateKind.PolymorphicPointer poly -> {
                        if (tag == SparseStateDelta.TAG_OBJECT) {
                            switchPolymorphicPointer(loc.entry, s, loc.base, (Serializer) sd.objAt(i));
                        }
                    }
                }
            }
            default -> { /* non-leaf target — skip */ }
        }
    }

    private void applyClear(Location loc) {
        switch (loc.layout) {
            case FieldLayout.Primitive p -> loc.entry.data[loc.base + p.offset()] = 0;
            case FieldLayout.InlineString is -> writeValue(loc.entry, is, loc.base, null);
            case FieldLayout.Ref r -> writeValue(loc.entry, r, loc.base, null);
            case FieldLayout.SubState s -> {
                switch (s.kind()) {
                    case FieldLayout.SubStateKind.Vector v -> resizeVector(loc.entry, s, loc.base, 0);
                    case FieldLayout.SubStateKind.FixedPointer fixed -> switchFixedPointer(loc.entry, s, loc.base, null);
                    case FieldLayout.SubStateKind.PolymorphicPointer poly -> switchPolymorphicPointer(loc.entry, s, loc.base, null);
                }
            }
            default -> { /* skip */ }
        }
    }

    /**
     * Shared navigation: walks the fp and returns the entry/layout/base at the
     * leaf, or {@code null} if the path terminates early (missing sub-entry
     * or out-of-bounds array index). Mirrors the read-side navigation used
     * by {@code getValueForFieldPath} — does not materialize sub-entries.
     */
    private Location navigate(S2FieldPath fp) {
        Entry current = rootEntry;
        FieldLayout layout = current.rootLayout;
        var base = 0;
        var last = fp.last();

        var i = 0;
        while (true) {
            var idx = fp.get(i);
            switch (layout) {
                case FieldLayout.Composite c -> layout = c.children()[idx];
                case FieldLayout.Array a -> {
                    if (idx >= a.length()) return null;
                    base += a.baseOffset() + idx * a.stride();
                    layout = a.element();
                }
                case FieldLayout.SubState s -> {
                    if (current.data[base + s.offset()] == 0) return null;
                    var slot = (int) INT_VH.get(current.data, base + s.offset() + 1);
                    current = (Entry) refs[slot];
                    layout = current.rootLayout;
                    base = 0;
                    continue;
                }
                default -> throw new IllegalStateException("non-branch layout at non-leaf position: " + layout);
            }
            if (i == last) break;
            i++;
        }
        return new Location(current, layout, base);
    }

    private record Location(Entry entry, FieldLayout layout, int base) {}

    static final class Entry {

        FieldLayout rootLayout;
        byte[] data;

        Entry(FieldLayout rootLayout, byte[] data) {
            this.rootLayout = rootLayout;
            this.data = data;
        }
    }
}
