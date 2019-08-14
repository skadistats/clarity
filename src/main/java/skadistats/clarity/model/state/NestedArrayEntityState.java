package skadistats.clarity.model.state;

import skadistats.clarity.decoder.s2.Serializer;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s2.S2ModifiableFieldPath;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class NestedArrayEntityState implements EntityState, ArrayEntityState {

    private final Serializer serializer;
    private final List<Entry> entries = new ArrayList<>();

    public NestedArrayEntityState(Serializer serializer) {
        this.serializer = serializer;
        Entry rootEntry = new Entry();
        rootEntry.capacity(serializer.getFieldCount());
        entries.add(rootEntry);
        serializer.initInitialState(this);
    }

    private NestedArrayEntityState(NestedArrayEntityState other) {
        serializer = other.serializer;
        for (Entry e : other.entries) {
            entries.add(e != null ? new Entry(e.state, e.state.length == 0) : null);
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
    public ArrayEntityState capacity(int wantedSize, boolean shrinkIfNeeded, Consumer<ArrayEntityState> initializer) {
        return rootEntry().capacity(wantedSize, shrinkIfNeeded, initializer);
    }

    @Override
    public EntityState clone() {
        return new NestedArrayEntityState(this);
    }

    @Override
    public void setValueForFieldPath(FieldPath fp, Object value) {
        serializer.setValueForFieldPath(fp.s2(), 0, this, value);
    }

    @Override
    public <T> T getValueForFieldPath(FieldPath fp) {
        return (T) serializer.getValueForFieldPath(fp.s2(), 0, this);
    }

    @Override
    public Iterator<FieldPath> fieldPathIterator() {
        List<FieldPath> result = new ArrayList<>();
        serializer.collectFieldPaths(S2ModifiableFieldPath.newInstance(), result, this);
        return result.iterator();
    }

    private StateRef createStateRef(Entry state) {
        // TODO: this is slower than not doing it
//        for (int i = 0; i < states.size(); i++) {
//            if (states.get(i) == null) {
//                states.set(i, state);
//                return new StateRef(i);
//            }
//        }
        int i = entries.size();
        entries.add(state);
        return new StateRef(i);
    }

    private void clearStateRef(StateRef stateRef) {
        entries.set(stateRef.idx, null);
    }

    private static class StateRef {

        private final int idx;

        private StateRef(int idx) {
            this.idx = idx;
        }

        @Override
        public String toString() {
            return "StateRef[" + idx + "]";
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
                Object[] newState = new Object[state.length];
                System.arraycopy(state, 0, newState, 0, state.length);
                state = newState;
                modifiable = true;
            }
            if (state[idx] instanceof StateRef) {
                clearStateRef((StateRef) state[idx]);
            }
            state[idx] = value;
        }

        @Override
        public void clear(int idx) {
            set(idx, null);
        }

        @Override
        public boolean isSub(int idx) {
            return has(idx) && get(idx) instanceof StateRef;
        }

        @Override
        public ArrayEntityState sub(int idx) {
            if (!has(idx)) {
                set(idx, createStateRef(new Entry()));
            }
            StateRef stateRef = (StateRef) get(idx);
            return entries.get(stateRef.idx);
        }

        @Override
        public ArrayEntityState capacity(int wantedSize, boolean shrinkIfNeeded, Consumer<ArrayEntityState> initializer) {
            int curSize = state.length;
            if (wantedSize == curSize) {
                return this;
            }
            if (wantedSize < 0) {
                // TODO: sometimes negative - figure out what this means
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
                if (initializer != null) {
                    for (int i = curSize; i < wantedSize; i++) {
                        initializer.accept(sub(i));
                    }
                }
            }
            return this;
        }

        @Override
        public String toString() {
            return "State[modifiable=" + modifiable + "]";
        }

    }

}
