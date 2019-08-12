package skadistats.clarity.model.state;

import skadistats.clarity.model.FieldPath;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class NestedArrayEntityState implements EntityState, ArrayEntityState {

    private final NameGetter nameGetter;
    private final ValueGetter valueGetter;
    private final ValueSetter valueSetter;
    private final FieldPathCollector fieldPathCollector;

    private final List<Entry> entries = new ArrayList<>();

    public NestedArrayEntityState(int length, NameGetter nameGetter, ValueGetter valueGetter, ValueSetter valueSetter, FieldPathCollector fieldPathCollector) {
        this.nameGetter = nameGetter;
        this.valueGetter = valueGetter;
        this.valueSetter = valueSetter;
        this.fieldPathCollector = fieldPathCollector;
        Entry rootEntry = new Entry();
        rootEntry.capacity(length);
        entries.add(rootEntry);
    }

    private NestedArrayEntityState(NestedArrayEntityState other) {
        nameGetter = other.nameGetter;
        valueGetter = other.valueGetter;
        valueSetter = other.valueSetter;
        fieldPathCollector = other.fieldPathCollector;
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
    public String getNameForFieldPath(FieldPath fp) {
        return nameGetter.get(this, fp);
    }

    @Override
    public void setValueForFieldPath(FieldPath fp, Object value) {
        valueSetter.set(this, fp, value);
    }

    @Override
    public <T> T getValueForFieldPath(FieldPath fp) {
        return (T) valueGetter.get(this, fp);
    }

    @Override
    public List<FieldPath> collectFieldPaths() {
        return fieldPathCollector.collect(this);
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

    public interface FieldPathCollector {
        List<FieldPath> collect(ArrayEntityState state);
    }

    public interface NameGetter {
        String get(ArrayEntityState state, FieldPath fp);
    }

    public interface ValueGetter {
        Object get(ArrayEntityState state, FieldPath fp);
    }

    public interface ValueSetter {
        void set(ArrayEntityState state, FieldPath fp, Object data);
    }

}
