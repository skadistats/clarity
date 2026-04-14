package skadistats.clarity.model.state;

import skadistats.clarity.io.s2.field.SerializerField;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.model.s2.S2LongFieldPathFormat;
import skadistats.clarity.model.s2.S2ModifiableFieldPath;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import static skadistats.clarity.model.state.PrimitiveType.INT_VH;

public class FlatEntityState extends AbstractS2EntityState {

    private List<Object> refs;
    private Deque<Integer> freeSlots;
    private boolean refsModifiable;
    private final Entry rootEntry;

    public FlatEntityState(SerializerField rootField, int pointerCount,
                           FieldLayout rootLayout, int totalBytes) {
        super(rootField, pointerCount);
        this.refs = new ArrayList<>();
        this.freeSlots = new ArrayDeque<>();
        this.refsModifiable = true;
        this.rootEntry = new Entry(rootLayout, new byte[totalBytes], true);
    }

    private FlatEntityState(FlatEntityState other) {
        super(other);
        this.refs = other.refs;
        this.freeSlots = other.freeSlots;
        this.refsModifiable = false;
        other.refsModifiable = false;
        this.rootEntry = other.rootEntry.copy();
    }

    @Override
    public EntityState copy() {
        return new FlatEntityState(this);
    }

    @Override
    public boolean applyMutation(FieldPath fpX, StateMutation mutation) {
        var fp = fpX.s2();
        Entry current = this.rootEntry;
        FieldLayout layout = current.rootLayout;
        var base = 0;
        var last = fp.last();

        var i = 0;
        while (true) {
            var idx = fp.get(i);
            if (layout instanceof FieldLayout.Composite c) {
                layout = c.children()[idx];
            } else if (layout instanceof FieldLayout.Array a) {
                base += a.baseOffset() + idx * a.stride();
                layout = a.element();
            } else if (layout instanceof FieldLayout.SubState s) {
                // Descent through SubState consumes no fp index. SubState-as-leaf
                // ops (SwitchPointer, ResizeVector) reach the dispatch via the
                // Composite/Array branch advancing `layout = SubState` on the last
                // idx and breaking — they never enter THIS branch.
                var nextIdx = fp.get(i);
                if (current.data[base + s.offset()] == 0) {
                    lazyCreateSubEntry(current, base, s, nextIdx);
                }
                var slot = (int) INT_VH.get(current.data, base + s.offset() + 1);
                var sub = (Entry) refs.get(slot);
                if (!sub.modifiable) {
                    ensureRefsModifiable();
                    sub = sub.copy();
                    refs.set(slot, sub);
                }
                if (s.kind() instanceof FieldLayout.SubStateKind.Vector v) {
                    growVectorIfNeeded(sub, v, nextIdx + 1);
                }
                current = sub;
                layout = sub.rootLayout;
                base = 0;
                continue;
            } else {
                throw new IllegalStateException("non-branch layout at non-leaf position: " + layout);
            }
            if (i == last) break;
            i++;
        }

        if (mutation instanceof StateMutation.WriteValue wv) {
            return writeValue(current, layout, base, wv.value());
        } else if (mutation instanceof StateMutation.ResizeVector rv) {
            return resizeVector(current, layout, base, rv.count());
        } else if (mutation instanceof StateMutation.SwitchPointer sp) {
            return switchPointer(current, layout, base, sp);
        }
        throw new IllegalStateException("unknown mutation: " + mutation);
    }

    private boolean writeValue(Entry target, FieldLayout layout, int base, Object value) {
        if (layout instanceof FieldLayout.Primitive p) {
            target.ensureModifiable();
            var data = target.data;
            var flagPos = base + p.offset();
            var oldFlag = data[flagPos];
            var willSet = value != null;
            data[flagPos] = willSet ? (byte) 1 : (byte) 0;
            if (willSet) p.type().write(data, flagPos + 1, value);
            return (oldFlag != 0) ^ willSet;
        }
        if (layout instanceof FieldLayout.Ref r) {
            target.ensureModifiable();
            var data = target.data;
            var flagPos = base + r.offset();
            var oldFlag = data[flagPos];
            if (value == null) {
                if (oldFlag == 0) return false;
                ensureRefsModifiable();
                var slot = (int) INT_VH.get(data, flagPos + 1);
                freeRefSlot(slot);
                data[flagPos] = 0;
                return true;
            }
            ensureRefsModifiable();
            int slot;
            if (oldFlag != 0) {
                slot = (int) INT_VH.get(data, flagPos + 1);
            } else {
                slot = allocateRefSlot();
                INT_VH.set(data, flagPos + 1, slot);
                data[flagPos] = 1;
            }
            refs.set(slot, value);
            return oldFlag == 0;
        }
        throw new IllegalStateException("WriteValue on non-leaf layout: " + layout);
    }

    private boolean resizeVector(Entry current, FieldLayout layout, int base, int newCount) {
        if (!(layout instanceof FieldLayout.SubState s) || !(s.kind() instanceof FieldLayout.SubStateKind.Vector v)) {
            throw new IllegalStateException("ResizeVector on non-vector substate: " + layout);
        }
        var data = current.data;
        var flagPos = base + s.offset();
        if (data[flagPos] == 0) {
            if (newCount == 0) return false;
            current.ensureModifiable();
            ensureRefsModifiable();
            var array = new FieldLayout.Array(0, v.elementBytes(), newCount, v.elementLayout());
            var sub = new Entry(array, new byte[newCount * v.elementBytes()], true);
            var slot = allocateRefSlot();
            refs.set(slot, sub);
            INT_VH.set(current.data, flagPos + 1, slot);
            current.data[flagPos] = 1;
            return false;
        }
        var slot = (int) INT_VH.get(data, flagPos + 1);
        var sub = (Entry) refs.get(slot);
        var oldArray = (FieldLayout.Array) sub.rootLayout;
        var oldCount = oldArray.length();
        if (oldCount == newCount) return false;
        var droppedOccupied = false;
        if (newCount < oldCount) {
            for (var i = newCount; i < oldCount && !droppedOccupied; i++) {
                droppedOccupied = hasAnyOccupiedPath(sub, v.elementLayout(), i * v.elementBytes());
            }
            ensureRefsModifiable();
            for (var i = newCount; i < oldCount; i++) {
                releaseRefsInEntry(sub, v.elementLayout(), i * v.elementBytes());
            }
        }
        if (!sub.modifiable) {
            ensureRefsModifiable();
            sub = sub.copy();
            refs.set(slot, sub);
        }
        var newData = new byte[newCount * v.elementBytes()];
        System.arraycopy(sub.data, 0, newData, 0, Math.min(sub.data.length, newData.length));
        sub.data = newData;
        sub.rootLayout = new FieldLayout.Array(0, v.elementBytes(), newCount, v.elementLayout());
        sub.modifiable = true;
        return droppedOccupied;
    }

    private boolean switchPointer(Entry current, FieldLayout layout, int base, StateMutation.SwitchPointer sp) {
        if (!(layout instanceof FieldLayout.SubState s) || !(s.kind() instanceof FieldLayout.SubStateKind.Pointer p)) {
            throw new IllegalStateException("SwitchPointer on non-pointer substate: " + layout);
        }
        var newSerializer = sp.newSerializer();
        var currentSerializer = pointerSerializers[p.pointerId()];
        if (currentSerializer == newSerializer) return false;
        var flagPos = base + s.offset();
        var hadSub = current.data[flagPos] != 0;
        var removedOccupied = false;

        if (hadSub) {
            current.ensureModifiable();
            var oldSlot = (int) INT_VH.get(current.data, flagPos + 1);
            var oldSub = (Entry) refs.get(oldSlot);
            removedOccupied = hasAnyOccupiedPath(oldSub, oldSub.rootLayout, 0);
            ensureRefsModifiable();
            releaseRefSlot(oldSlot);
            current.data[flagPos] = 0;
            pointerSerializers[p.pointerId()] = null;
            hadSub = false;
        }
        if (newSerializer != null) {
            current.ensureModifiable();
            var layoutIdx = lookupLayoutIndex(p, newSerializer);
            var sub = new Entry(p.layouts()[layoutIdx], new byte[p.layoutBytes()[layoutIdx]], true);
            ensureRefsModifiable();
            var slot = allocateRefSlot();
            refs.set(slot, sub);
            INT_VH.set(current.data, flagPos + 1, slot);
            current.data[flagPos] = 1;
            pointerSerializers[p.pointerId()] = newSerializer;
        }
        return removedOccupied;
    }

    private boolean hasAnyOccupiedPath(Entry entry, FieldLayout layout, int base) {
        if (layout instanceof FieldLayout.Composite c) {
            for (var child : c.children()) {
                if (hasAnyOccupiedPath(entry, child, base)) return true;
            }
            return false;
        }
        if (layout instanceof FieldLayout.Array a) {
            for (var i = 0; i < a.length(); i++) {
                if (hasAnyOccupiedPath(entry, a.element(), base + a.baseOffset() + i * a.stride())) return true;
            }
            return false;
        }
        if (layout instanceof FieldLayout.Primitive p) {
            return entry.data[base + p.offset()] != 0;
        }
        if (layout instanceof FieldLayout.Ref r) {
            return entry.data[base + r.offset()] != 0;
        }
        if (layout instanceof FieldLayout.SubState s) {
            if (entry.data[base + s.offset()] == 0) return false;
            var slot = (int) INT_VH.get(entry.data, base + s.offset() + 1);
            var sub = (Entry) refs.get(slot);
            return hasAnyOccupiedPath(sub, sub.rootLayout, 0);
        }
        return false;
    }

    private static int lookupLayoutIndex(FieldLayout.SubStateKind.Pointer p, skadistats.clarity.io.s2.Serializer newSerializer) {
        var serializers = p.serializers();
        for (var i = 0; i < serializers.length; i++) {
            if (serializers[i] == newSerializer) return i;
        }
        throw new IllegalStateException("Serializer " + newSerializer + " not found in pointer serializers");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getValueForFieldPath(FieldPath fpX) {
        var fp = fpX.s2();
        Entry current = this.rootEntry;
        FieldLayout layout = current.rootLayout;
        var base = 0;
        var last = fp.last();

        var i = 0;
        while (true) {
            var idx = fp.get(i);
            if (layout instanceof FieldLayout.Composite c) {
                layout = c.children()[idx];
            } else if (layout instanceof FieldLayout.Array a) {
                if (idx >= a.length()) return null;
                base += a.baseOffset() + idx * a.stride();
                layout = a.element();
            } else if (layout instanceof FieldLayout.SubState s) {
                if (current.data[base + s.offset()] == 0) return null;
                var slot = (int) INT_VH.get(current.data, base + s.offset() + 1);
                var sub = (Entry) refs.get(slot);
                current = sub;
                layout = sub.rootLayout;
                base = 0;
                continue;
            } else {
                throw new IllegalStateException("non-branch layout at non-leaf position: " + layout);
            }
            if (i == last) break;
            i++;
        }

        if (layout instanceof FieldLayout.Primitive p) {
            if (current.data[base + p.offset()] == 0) return null;
            return (T) p.type().read(current.data, base + p.offset() + 1);
        }
        if (layout instanceof FieldLayout.Ref r) {
            if (current.data[base + r.offset()] == 0) return null;
            var slot = (int) INT_VH.get(current.data, base + r.offset() + 1);
            return (T) refs.get(slot);
        }
        return null;
    }

    @Override
    public Iterator<FieldPath> fieldPathIterator() {
        var out = new ArrayList<FieldPath>();
        var indices = new int[S2LongFieldPathFormat.MAX_FIELDPATH_LENGTH];
        walk(rootEntry, rootEntry.rootLayout, 0, indices, 0, out);
        return out.iterator();
    }

    private void walk(Entry entry, FieldLayout layout, int base, int[] indices, int depth, List<FieldPath> out) {
        if (layout instanceof FieldLayout.Composite c) {
            var children = c.children();
            for (var i = 0; i < children.length; i++) {
                indices[depth] = i;
                walk(entry, children[i], base, indices, depth + 1, out);
            }
        } else if (layout instanceof FieldLayout.Array a) {
            for (var i = 0; i < a.length(); i++) {
                indices[depth] = i;
                walk(entry, a.element(), base + a.baseOffset() + i * a.stride(), indices, depth + 1, out);
            }
        } else if (layout instanceof FieldLayout.Primitive p) {
            if (entry.data[base + p.offset()] != 0) {
                out.add(buildFieldPath(indices, depth));
            }
        } else if (layout instanceof FieldLayout.Ref r) {
            if (entry.data[base + r.offset()] != 0) {
                out.add(buildFieldPath(indices, depth));
            }
        } else if (layout instanceof FieldLayout.SubState s) {
            if (entry.data[base + s.offset()] != 0) {
                var slot = (int) INT_VH.get(entry.data, base + s.offset() + 1);
                var sub = (Entry) refs.get(slot);
                walk(sub, sub.rootLayout, 0, indices, depth, out);
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
     * NestedArrayEntityState does this implicitly via untyped Object[] storage.
     * For FLAT we need a concrete layout, so this only works for cases where the
     * layout is unambiguous: Pointer with a single (default) serializer.
     * For ambiguous Pointers and Vectors, the protocol is expected to emit
     * SwitchPointer / ResizeVector before any inner write.
     */
    private void lazyCreateSubEntry(Entry parent, int base, FieldLayout.SubState s, int hintIdx) {
        parent.ensureModifiable();
        ensureRefsModifiable();
        Entry sub;
        if (s.kind() instanceof FieldLayout.SubStateKind.Pointer p) {
            if (p.serializers().length != 1) {
                throw new IllegalStateException(
                    "cannot lazy-create sub-Entry for Pointer with " + p.serializers().length
                    + " serializers (expected explicit SwitchPointer first), pointerId=" + p.pointerId());
            }
            sub = new Entry(p.layouts()[0], new byte[p.layoutBytes()[0]], true);
            pointerSerializers[p.pointerId()] = p.serializers()[0];
        } else if (s.kind() instanceof FieldLayout.SubStateKind.Vector v) {
            // Lazy-create vector sized to fit the upcoming element index.
            // Mirrors NestedArrayEntityState's auto-growing capacity on writes.
            var length = hintIdx + 1;
            var array = new FieldLayout.Array(0, v.elementBytes(), length, v.elementLayout());
            sub = new Entry(array, new byte[length * v.elementBytes()], true);
        } else {
            throw new IllegalStateException("unknown SubState kind: " + s.kind());
        }
        var slot = allocateRefSlot();
        refs.set(slot, sub);
        INT_VH.set(parent.data, base + s.offset() + 1, slot);
        parent.data[base + s.offset()] = 1;
    }

    /**
     * Grow a vector sub-Entry to fit at least `requiredLength` elements.
     * Caller is responsible for ensuring `sub` is modifiable.
     * Mirrors NestedArrayEntityState's capacity-extension behavior on writes.
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
        if (!freeSlots.isEmpty()) return freeSlots.removeFirst();
        refs.add(null);
        return refs.size() - 1;
    }

    private void freeRefSlot(int slot) {
        refs.set(slot, null);
        freeSlots.addLast(slot);
    }

    private void releaseRefSlot(int slot) {
        if (refs.get(slot) instanceof Entry e) {
            releaseRefsInEntry(e, e.rootLayout, 0);
        }
        freeRefSlot(slot);
    }

    private void releaseRefsInEntry(Entry e, FieldLayout layout, int base) {
        if (layout instanceof FieldLayout.Composite c) {
            for (var child : c.children()) {
                releaseRefsInEntry(e, child, base);
            }
        } else if (layout instanceof FieldLayout.Array a) {
            for (var i = 0; i < a.length(); i++) {
                releaseRefsInEntry(e, a.element(), base + a.baseOffset() + i * a.stride());
            }
        } else if (layout instanceof FieldLayout.Ref r) {
            if (e.data[base + r.offset()] != 0) {
                var innerSlot = (int) INT_VH.get(e.data, base + r.offset() + 1);
                freeRefSlot(innerSlot);
            }
        } else if (layout instanceof FieldLayout.SubState s) {
            if (e.data[base + s.offset()] != 0) {
                var innerSlot = (int) INT_VH.get(e.data, base + s.offset() + 1);
                releaseRefSlot(innerSlot);
            }
        }
    }

    int slabSize() {
        return refs.size();
    }

    int freeSlotCount() {
        return freeSlots.size();
    }

    private void ensureRefsModifiable() {
        if (!refsModifiable) {
            refs = new ArrayList<>(refs);
            freeSlots = new ArrayDeque<>(freeSlots);
            refsModifiable = true;
        }
    }

    final class Entry {

        FieldLayout rootLayout;
        byte[] data;
        boolean modifiable;

        Entry(FieldLayout rootLayout, byte[] data, boolean modifiable) {
            this.rootLayout = rootLayout;
            this.data = data;
            this.modifiable = modifiable;
        }

        Entry copy() {
            markSubEntriesNonModifiable(rootLayout, 0);
            modifiable = false;
            return new Entry(rootLayout, data, false);
        }

        void ensureModifiable() {
            if (!modifiable) {
                data = data.clone();
                modifiable = true;
            }
        }

        private void markSubEntriesNonModifiable(FieldLayout layout, int base) {
            if (layout instanceof FieldLayout.Composite c) {
                for (var child : c.children()) {
                    markSubEntriesNonModifiable(child, base);
                }
            } else if (layout instanceof FieldLayout.Array a) {
                for (var i = 0; i < a.length(); i++) {
                    markSubEntriesNonModifiable(a.element(), base + a.baseOffset() + i * a.stride());
                }
            } else if (layout instanceof FieldLayout.SubState s) {
                if (data[base + s.offset()] != 0) {
                    var slot = (int) INT_VH.get(data, base + s.offset() + 1);
                    var sub = (Entry) refs.get(slot);
                    if (sub.modifiable) {
                        sub.markSubEntriesNonModifiable(sub.rootLayout, 0);
                        sub.modifiable = false;
                    }
                }
            }
        }
    }
}
