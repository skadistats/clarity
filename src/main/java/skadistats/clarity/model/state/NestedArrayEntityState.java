package skadistats.clarity.model.state;

import skadistats.clarity.io.s2.Field;
import skadistats.clarity.io.s2.field.ArrayField;
import skadistats.clarity.io.s2.field.PointerField;
import skadistats.clarity.io.s2.field.SerializerField;
import skadistats.clarity.model.FieldPath;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

public class NestedArrayEntityState extends AbstractS2EntityState {

    private final List<Entry> entries;
    private Deque<Integer> freeEntries;
    private boolean capacityChanged;

    public NestedArrayEntityState(SerializerField field, int pointerCount) {
        super(field, pointerCount);
        entries = new ArrayList<>(20);
        entries.add(new Entry());
    }

    private NestedArrayEntityState(NestedArrayEntityState other) {
        super(other);
        var otherSize = other.entries.size();
        entries = new ArrayList<>(otherSize + 4);
        for (var i = 0; i < otherSize; i++) {
            var e = other.entries.get(i);
            if (e == null) {
                entries.add(null);
                markFree(i);
            } else {
                var modifiable = e.state.length == 0;
                e.modifiable = modifiable;
                entries.add(new Entry(e.state, modifiable));
            }
        }
    }

    private Entry rootEntry() {
        return entries.get(0);
    }

    @Override
    public EntityState copy() {
        return new NestedArrayEntityState(this);
    }

    @Override
    public boolean applyMutation(FieldPath fpX, StateMutation mutation) {
        var fp = fpX.s2();
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
                if (mutation instanceof StateMutation.WriteValue wv) {
                    node.set(idx, wv.value());
                    return capacityChanged;
                } else if (mutation instanceof StateMutation.ResizeVector rv) {
                    return handleResizeVector(node, idx, rv.count());
                } else if (mutation instanceof StateMutation.SwitchPointer sp) {
                    return handlePointerSwitch(node, idx, child, sp);
                }
                throw new IllegalStateException();
            }
            field = child;
            node = node.subEntry(idx);
            i++;
        }
    }

    private void ensureNodeCapacity(Field parentField, Entry node, int idx) {
        if (parentField instanceof SerializerField sf) {
            node.capacity(sf.getSerializer().getFieldCount(), false);
        } else if (parentField instanceof ArrayField af) {
            node.capacity(af.getLength(), false);
        } else {
            node.capacity(idx + 1, false);
        }
    }

    private boolean handlePointerSwitch(Entry node, int idx, Field field, StateMutation.SwitchPointer sp) {
        var newSerializer = sp.newSerializer();
        if (!(field instanceof PointerField pf)) return false;
        var currentSerializer = pointerSerializers[pf.getPointerId()];
        if (currentSerializer == newSerializer) return false;
        var removedOccupied = false;
        if (node.has(idx)) {
            removedOccupied = hasAnyOccupiedPath(node.subEntry(idx));
            pointerSerializers[pf.getPointerId()] = null;
            node.clear(idx);
        }
        if (newSerializer != null) {
            pointerSerializers[pf.getPointerId()] = newSerializer;
            node.subEntry(idx);
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
        node.subEntry(idx).capacity(newCount, true);
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

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getValueForFieldPath(FieldPath fpX) {
        var fp = fpX.s2();
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
    public Iterator<FieldPath> fieldPathIterator() {
        return new NestedArrayEntityStateIterator(rootEntry());
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
        markFree(entryRef.idx);
    }

    private void markFree(int i) {
        if (freeEntries == null) {
            freeEntries = new ArrayDeque<>();
        }
        freeEntries.add(i);
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

    public class Entry implements NestedEntityState {

        private Object[] state;
        private boolean modifiable;

        private Entry() {
            this(EMPTY_STATE, true);
        }

        private Entry(Object[] state, boolean modifiable) {
            this.state = state;
            this.modifiable = modifiable;
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
            if (!modifiable) {
                var newState = new Object[state.length];
                System.arraycopy(state, 0, newState, 0, state.length);
                state = newState;
                modifiable = true;
            }
            if (state[idx] instanceof EntryRef) {
                clearEntryRef((EntryRef) state[idx]);
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
        public NestedEntityState sub(int idx) {
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
        public NestedEntityState capacity(int wantedSize, boolean shrinkIfNeeded) {
            var curSize = state.length;
            if (wantedSize == curSize) {
                return this;
            }

            Object[] newState = null;
            if (wantedSize > curSize) {
                newState = new Object[wantedSize];
            } else if (shrinkIfNeeded) {
                newState = wantedSize == 0 ? EMPTY_STATE : new Object[wantedSize];
            }

            if (newState != null) {
                System.arraycopy(state, 0, newState, 0, Math.min(curSize, wantedSize));
                state = newState;
                modifiable = true;
                capacityChanged = true;
            }
            return this;
        }

        @Override
        public String toString() {
            return "Entry[modifiable=" + modifiable + ", size=" + state.length + "]";
        }

    }

}
