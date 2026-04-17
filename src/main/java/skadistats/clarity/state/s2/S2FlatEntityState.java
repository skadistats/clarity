package skadistats.clarity.state.s2;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.decoder.DecoderDispatch;
import skadistats.clarity.io.decoder.StringLenDecoder;
import skadistats.clarity.io.decoder.StringZeroTerminatedDecoder;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.model.s2.S2LongFieldPathFormat;
import skadistats.clarity.model.s2.S2ModifiableFieldPath;
import skadistats.clarity.model.s2.Serializer;
import skadistats.clarity.model.s2.field.SerializerField;
import skadistats.clarity.state.EntityState;
import skadistats.clarity.state.FieldLayout;
import skadistats.clarity.state.StateMutation;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static skadistats.clarity.state.PrimitiveType.INT_VH;

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
            case StateMutation.WriteValue wv    -> writeValue(current, layout, base, wv.value());
            case StateMutation.ResizeVector rv  -> resizeVector(current, layout, base, rv.count());
            case StateMutation.SwitchPointer sp -> switchPointer(current, layout, base, sp.newSerializer());
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
                case FieldLayout.SubStateKind.Pointer p -> switchPointer(current, s, base, (Serializer) decoded);
                case FieldLayout.SubStateKind.Vector v  -> resizeVector(current, s, base, (Integer) decoded);
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

    private boolean switchPointer(Entry current, FieldLayout layout, int base, Serializer newSerializer) {
        if (!(layout instanceof FieldLayout.SubState s) || !(s.kind() instanceof FieldLayout.SubStateKind.Pointer p)) {
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

    private static int lookupLayoutIndex(FieldLayout.SubStateKind.Pointer p, Serializer newSerializer) {
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
            default -> null;
        };
    }

    @Override
    public Iterator<FieldPath> fieldPathIterator() {
        var out = new ArrayList<FieldPath>();
        var indices = new int[S2LongFieldPathFormat.MAX_FIELDPATH_LENGTH];
        walk(rootEntry, rootEntry.rootLayout, 0, indices, 0, out);
        return out.iterator();
    }

    private void walk(Entry entry, FieldLayout layout, int base, int[] indices, int depth, List<FieldPath> out) {
        switch (layout) {
            case FieldLayout.Composite c -> {
                var children = c.children();
                for (var i = 0; i < children.length; i++) {
                    indices[depth] = i;
                    walk(entry, children[i], base, indices, depth + 1, out);
                }
            }
            case FieldLayout.Array a -> {
                for (var i = 0; i < a.length(); i++) {
                    indices[depth] = i;
                    walk(entry, a.element(), base + a.baseOffset() + i * a.stride(), indices, depth + 1, out);
                }
            }
            case FieldLayout.Primitive p -> {
                if (entry.data[base + p.offset()] != 0) out.add(buildFieldPath(indices, depth));
            }
            case FieldLayout.InlineString is -> {
                if (entry.data[base + is.offset()] != 0) out.add(buildFieldPath(indices, depth));
            }
            case FieldLayout.Ref r -> {
                if (entry.data[base + r.offset()] != 0) out.add(buildFieldPath(indices, depth));
            }
            case FieldLayout.SubState s -> {
                if (entry.data[base + s.offset()] != 0) {
                    var slot = (int) INT_VH.get(entry.data, base + s.offset() + 1);
                    var sub = (Entry) refs[slot];
                    walk(sub, sub.rootLayout, 0, indices, depth, out);
                }
            }
        }
    }

    private static S2FieldPath buildFieldPath(int[] indices, int depth) {
        var fp = S2ModifiableFieldPath.newInstance();
        for (var i = 0; i < depth; i++) {
            if (i > 0) fp.down();
            fp.set(i, indices[i]);
        }
        return fp.unmodifiable();
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
            case FieldLayout.SubStateKind.Pointer p -> {
                if (p.serializers().length != 1) {
                    throw new IllegalStateException(
                        "cannot lazy-create sub-Entry for Pointer with " + p.serializers().length
                        + " serializers (expected explicit SwitchPointer first), pointerId=" + p.pointerId());
                }
                pointerSerializers[p.pointerId()] = p.serializers()[0];
                yield new Entry(p.layouts()[0], new byte[p.layoutBytes()[0]]);
            }
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

    static final class Entry {

        FieldLayout rootLayout;
        byte[] data;

        Entry(FieldLayout rootLayout, byte[] data) {
            this.rootLayout = rootLayout;
            this.data = data;
        }
    }
}
