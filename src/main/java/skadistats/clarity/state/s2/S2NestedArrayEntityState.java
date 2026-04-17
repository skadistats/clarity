package skadistats.clarity.state.s2;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s2.Field;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.model.s2.Serializer;
import skadistats.clarity.model.s2.field.ArrayField;
import skadistats.clarity.model.s2.field.PointerField;
import skadistats.clarity.model.s2.field.SerializerField;
import skadistats.clarity.model.s2.field.VectorField;
import skadistats.clarity.state.EntityState;
import skadistats.clarity.state.StateMutation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

public final class S2NestedArrayEntityState extends S2EntityState {

    private List<Entry> entries;
    private Deque<Integer> freeEntries;
    private boolean capacityChanged;

    public S2NestedArrayEntityState(SerializerField field, int pointerCount) {
        super(field, pointerCount);
        entries = new ArrayList<>(20);
        entries.add(new Entry());
        // freeEntries is lazy-allocated when the first slot is freed.
    }

    private S2NestedArrayEntityState(S2NestedArrayEntityState other) {
        super(other);
        var size = other.entries.size();
        entries = new ArrayList<>(size);
        for (var e : other.entries) {
            entries.add(e == null ? null : new Entry(e.state.length == 0 ? EMPTY_STATE : e.state.clone()));
        }
        freeEntries = other.freeEntries == null || other.freeEntries.isEmpty()
            ? null
            : new ArrayDeque<>(other.freeEntries);
    }

    private Entry rootEntry() {
        return entries.get(0);
    }

    @Override
    public Iterator<FieldPath> fieldPathIterator() {
        return new S2NestedArrayEntityStateIterator(rootEntry());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getValueForFieldPath(S2FieldPath fp) {
        Field field = rootField;
        Entry node = rootEntry();
        var last = fp.last();

        var i = 0;
        while (true) {
            var idx = fp.get(i);
            if (i == last) {
                return (T) node.get(idx);
            }
            field = field.getChild(this, idx);
            if (!node.isSub(idx)) {
                return null;
            }
            node = node.subEntry(idx);
            i++;
        }
    }

    @Override
    public EntityState copy() {
        return new S2NestedArrayEntityState(this);
    }

    @Override
    public boolean write(S2FieldPath fp, Object decoded) {
        Field field = rootField;
        Entry node = rootEntry();
        var last = fp.last();
        capacityChanged = false;

        var i = 0;
        while (true) {
            var idx = fp.get(i);
            if (node.length() <= idx) {
                ensureNodeCapacity(field, node, idx);
            }
            var child = field.getChild(this, idx);
            if (i == last) {
                return switch (child) {
                    case PointerField pf -> handlePointerSwitch(node, idx, pf, (Serializer) decoded);
                    case VectorField vf  -> handleResizeVector(node, idx, (Integer) decoded);
                    default -> {
                        node.set(idx, decoded);
                        yield capacityChanged;
                    }
                };
            }
            field = child;
            node = subEntryForWrite(node, idx);
            i++;
        }
    }

    @Override
    public boolean decodeInto(S2FieldPath fp, Decoder decoder, BitStream bs) {
        throw new UnsupportedOperationException("decodeInto is implemented only on S2FlatEntityState (S2) and S1FlatEntityState (S1)");
    }

    @Override
    public boolean applyMutation(S2FieldPath fp, StateMutation mutation) {
        Field field = rootField;
        Entry node = rootEntry();
        var last = fp.last();
        capacityChanged = false;

        var i = 0;
        while (true) {
            var idx = fp.get(i);
            if (node.length() <= idx) {
                ensureNodeCapacity(field, node, idx);
            }
            var child = field.getChild(this, idx);
            if (i == last) {
                return switch (mutation) {
                    case StateMutation.WriteValue wv -> {
                        node.set(idx, wv.value());
                        yield capacityChanged;
                    }
                    case StateMutation.ResizeVector rv -> handleResizeVector(node, idx, rv.count());
                    case StateMutation.SwitchPointer sp ->
                            child instanceof PointerField pf && handlePointerSwitch(node, idx, pf, sp.newSerializer());
                };
            }
            field = child;
            node = subEntryForWrite(node, idx);
            i++;
        }
    }

    private Entry subEntryForWrite(Entry parent, int idx) {
        if (!parent.isSub(idx)) {
            var fresh = new Entry();
            var ref = createEntryRef(fresh);
            parent.set(idx, ref);
            return fresh;
        }
        var entryRef = (EntryRef) parent.get(idx);
        return entries.get(entryRef.idx);
    }

    private void ensureNodeCapacity(Field parentField, Entry node, int idx) {
        switch (parentField) {
            case SerializerField sf -> node.capacity(sf.getSerializer().getFieldCount(), false);
            case ArrayField af      -> node.capacity(af.getLength(), false);
            default                 -> node.capacity(idx + 1, false);
        }
    }

    private boolean handlePointerSwitch(Entry node, int idx, PointerField pf, Serializer newSerializer) {
        var currentSerializer = pointerSerializers[pf.getPointerId()];
        if (currentSerializer == newSerializer) return false;
        var removedOccupied = false;
        if (node.has(idx)) {
            removedOccupied = hasAnyOccupiedPath(subEntryForWrite(node, idx));
            pointerSerializers[pf.getPointerId()] = null;
            node.clear(idx);
        }
        if (newSerializer != null) {
            pointerSerializers[pf.getPointerId()] = newSerializer;
            subEntryForWrite(node, idx);
        }
        return removedOccupied;
    }

    private boolean handleResizeVector(Entry node, int idx, int newCount) {
        var oldCount = node.isSub(idx) ? node.subEntry(idx).length() : 0;
        if (oldCount == newCount) return false;
        var droppedOccupied = false;
        if (newCount < oldCount) {
            var sub = node.subEntry(idx);
            for (var i = newCount; i < oldCount && !droppedOccupied; i++) {
                if (hasOccupiedSlot(sub, i)) droppedOccupied = true;
            }
        }
        subEntryForWrite(node, idx).capacity(newCount, true);
        return droppedOccupied;
    }

    private boolean hasAnyOccupiedPath(Entry entry) {
        for (var i = 0; i < entry.length(); i++) {
            if (hasOccupiedSlot(entry, i)) return true;
        }
        return false;
    }

    private boolean hasOccupiedSlot(Entry entry, int i) {
        if (!entry.has(i)) return false;
        var v = entry.get(i);
        if (v instanceof EntryRef ref) {
            var sub = entries.get(ref.idx);
            return sub != null && hasAnyOccupiedPath(sub);
        }
        return true;
    }

    private EntryRef createEntryRef(Entry entry) {
        int i;
        if (freeEntries == null || freeEntries.isEmpty()) {
            i = entries.size();
            entries.add(entry);
        } else {
            i = freeEntries.removeFirst();
            entries.set(i, entry);
        }
        return new EntryRef(i);
    }

    private void clearEntryRef(EntryRef entryRef) {
        entries.set(entryRef.idx, null);
        ensureFreeEntries().add(entryRef.idx);
    }

    private Deque<Integer> ensureFreeEntries() {
        if (freeEntries == null) freeEntries = new ArrayDeque<>();
        return freeEntries;
    }

    private void releaseEntryRef(EntryRef entryRef) {
        var e = entries.get(entryRef.idx);
        if (e != null) {
            for (var slot : e.state) {
                if (slot instanceof EntryRef child) {
                    releaseEntryRef(child);
                }
            }
        }
        clearEntryRef(entryRef);
    }

    public int slabSize() {
        return entries.size();
    }

    public int freeSlotCount() {
        return freeEntries == null ? 0 : freeEntries.size();
    }


    private static class EntryRef {

        private final int idx;

        private EntryRef(int idx) {
            this.idx = idx;
        }

        @Override
        public String toString() {
            return "EntryRef[" + idx + "]";
        }
    }

    private static final Object[] EMPTY_STATE = {};

    public class Entry implements S2NestedEntityState {

        private Object[] state;

        private Entry() {
            this(EMPTY_STATE);
        }

        private Entry(Object[] state) {
            this.state = state;
        }

        @Override
        public int length() {
            return state.length;
        }

        @Override
        public boolean has(int idx) {
            return state.length > idx && state[idx] != null;
        }

        @Override
        public Object get(int idx) {
            return state.length > idx ? state[idx] : null;
        }

        @Override
        public void set(int idx, Object value) {
            if (state[idx] instanceof EntryRef ref) {
                releaseEntryRef(ref);
            }
            if ((state[idx] == null) ^ (value == null)) {
                capacityChanged = true;
            }
            state[idx] = value;
        }

        @Override
        public void clear(int idx) {
            set(idx, null);
        }

        @Override
        public boolean isSub(int idx) {
            return has(idx) && get(idx) instanceof EntryRef;
        }

        @Override
        public S2NestedEntityState sub(int idx) {
            return subEntry(idx);
        }

        Entry subEntry(int idx) {
            if (!isSub(idx)) {
                set(idx, createEntryRef(new Entry()));
            }
            var entryRef = (EntryRef) get(idx);
            return entries.get(entryRef.idx);
        }

        @Override
        public S2NestedEntityState capacity(int wantedSize, boolean shrinkIfNeeded) {
            var curSize = state.length;
            if (wantedSize == curSize) {
                return this;
            }

            Object[] newState = null;
            if (wantedSize > curSize) {
                newState = new Object[wantedSize];
            } else if (shrinkIfNeeded) {
                for (var i = wantedSize; i < curSize; i++) {
                    if (state[i] instanceof EntryRef ref) {
                        releaseEntryRef(ref);
                    }
                }
                newState = wantedSize == 0 ? EMPTY_STATE : new Object[wantedSize];
            }

            if (newState != null) {
                System.arraycopy(state, 0, newState, 0, Math.min(curSize, wantedSize));
                state = newState;
                capacityChanged = true;
            }
            return this;
        }

        @Override
        public String toString() {
            return "Entry[size=" + state.length + "]";
        }

    }

}
