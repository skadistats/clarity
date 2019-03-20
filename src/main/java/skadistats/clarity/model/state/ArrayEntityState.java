package skadistats.clarity.model.state;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ArrayEntityState implements CloneableEntityState {

    private final List<State> states = new ArrayList<>();

    public ArrayEntityState(int length) {
        State rootState = new State();
        rootState.capacity(length);
        states.add(rootState);
    }

    private ArrayEntityState(ArrayEntityState other) {
        for (State state : other.states) {
            states.add(state != null ? new State(state.state, state.state.length == 0) : null);
        }
    }

    private State rootState() {
        return states.get(0);
    }

    @Override
    public int length() {
        return rootState().length();
    }

    @Override
    public boolean has(int idx) {
        return rootState().has(idx);
    }

    @Override
    public Object get(int idx) {
        return rootState().get(idx);
    }

    @Override
    public void set(int idx, Object value) {
        rootState().set(idx, value);
    }

    @Override
    public void clear(int idx) {
        rootState().clear(idx);
    }

    @Override
    public EntityState sub(int idx) {
        return rootState().sub(idx);
    }

    @Override
    public EntityState capacity(int wantedSize, boolean shrinkIfNeeded, Consumer<EntityState> initializer) {
        return rootState().capacity(wantedSize, shrinkIfNeeded, initializer);
    }

    @Override
    public CloneableEntityState clone() {
        return new ArrayEntityState(this);
    }

    private StateRef createStateRef(State state) {
        // TODO: this is slower than not doing it
//        for (int i = 0; i < states.size(); i++) {
//            if (states.get(i) == null) {
//                states.set(i, state);
//                return new StateRef(i);
//            }
//        }
        int i = states.size();
        states.add(state);
        return new StateRef(i);
    }

    private void clearStateRef(StateRef stateRef) {
        states.set(stateRef.idx, null);
    }

    private class StateRef {

        private final int idx;

        private StateRef(int idx) {
            this.idx = idx;
        }

        private State getState() {
            return states.get(idx);
        }
    }

    private static final Object[] EMPTY_STATE = {};

    public class State implements EntityState {

        private Object[] state;
        private boolean modifiable;

        private State() {
            this(EMPTY_STATE, true);
        }

        private State(Object[] state, boolean modifiable) {
            this.state = state;
            this.modifiable = modifiable;
        }

        @Override
        public int length() {
            return state.length;
        }

        @Override
        public boolean has(int idx) {
            return state[idx] != null;
        }

        @Override
        public Object get(int idx) {
            return state[idx];
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
        public EntityState sub(int idx) {
            if (!has(idx)) {
                set(idx, createStateRef(new State()));
            }
            return ((StateRef) get(idx)).getState();
        }

        @Override
        public EntityState capacity(int wantedSize, boolean shrinkIfNeeded, Consumer<EntityState> initializer) {
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
    }

}
