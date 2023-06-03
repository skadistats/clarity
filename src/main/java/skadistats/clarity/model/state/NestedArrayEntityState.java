package skadistats.clarity.model.state;

import skadistats.clarity.io.s2.Field;
import skadistats.clarity.io.s2.field.SerializerField;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s2.S2FieldPath;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

public class NestedArrayEntityState implements EntityState, ArrayEntityState {

    private final SerializerField rootField;
    private final List<Entry> entries;
    private Deque<Integer> freeEntries;

    boolean capacityChanged;

    public NestedArrayEntityState(SerializerField field) {
        rootField = field;
        entries = new ArrayList<>(20);
        entries.add(new Entry());
    }

    private NestedArrayEntityState(NestedArrayEntityState other) {
        rootField = other.rootField;
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
    public int length() {
        return rootEntry().length();
    }

    @Override
    public boolean has(int idx) {
        return rootEntry().has(idx);
    }

    @Override
    public Object get(int idx) {
        return rootEntry().get(idx);
    }

    @Override
    public void set(int idx, Object value) {
        rootEntry().set(idx, value);
    }

    @Override
    public void clear(int idx) {
        rootEntry().clear(idx);
    }

    @Override
    public boolean isSub(int idx) {
        return rootEntry().isSub(idx);
    }

    @Override
    public ArrayEntityState sub(int idx) {
        return rootEntry().sub(idx);
    }

    @Override
    public ArrayEntityState capacity(int wantedSize, boolean shrinkIfNeeded) {
        return rootEntry().capacity(wantedSize, shrinkIfNeeded);
    }

    @Override
    public EntityState copy() {
        return new NestedArrayEntityState(this);
    }

    @Override
    public boolean setValueForFieldPath(FieldPath fpX, Object value) {
        var fp = fpX.s2();

        Field field = rootField;
        var entry = rootEntry();
        var last = fp.last();

        capacityChanged = false;
        var i = 0;
        while (true) {
            var idx = fp.get(i);
            if (entry.length() <= idx) {
                field.ensureArrayEntityStateCapacity(entry, idx + 1);
            }
            field = field.getChild(idx);
            if (i == last) {
                field.setArrayEntityState(entry, idx, value);
                return capacityChanged;
            }
            entry = entry.subEntry(idx);
            i++;
        }
    }

    @Override
    public <T> T getValueForFieldPath(FieldPath fpX) {
        var fp = fpX.s2();

        Field field = rootField;
        var entry = rootEntry();
        var last = fp.last();

        var i = 0;
        while (true) {
            var idx = fp.get(i);
            field = field.getChild(idx);
            if (i == last) {
                return (T) field.getArrayEntityState(entry, idx);
            }
            if (!entry.isSub(idx)) {
                return null;
            }
            entry = entry.subEntry(idx);
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

    public class Entry implements ArrayEntityState {

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
        public ArrayEntityState sub(int idx) {
            return subEntry(idx);
        }

        private Entry subEntry(int idx) {
            if (!isSub(idx)) {
                set(idx, createEntryRef(new Entry()));
            }
            var entryRef = (EntryRef) get(idx);
            return entries.get(entryRef.idx);
        }

        @Override
        public ArrayEntityState capacity(int wantedSize, boolean shrinkIfNeeded) {
            var curSize = state.length;
            if (wantedSize == curSize) {
                return this;
            }
            if (wantedSize < 0) {
                // TODO: sometimes negative - figure out what this means
                return this;
            }
            if (wantedSize == 0xFFFFFE0) {
                // TODO: 7.28 hotfix - figure out what's going on
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
