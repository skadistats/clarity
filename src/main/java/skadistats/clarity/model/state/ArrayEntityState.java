package skadistats.clarity.model.state;

import skadistats.clarity.decoder.Util;

import java.util.function.Consumer;

public class ArrayEntityState implements CloneableEntityState {

    private static final Object[] EMPTY_STATE = {};

    private Object[] state = EMPTY_STATE;

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
        state[idx] = value;
    }

    @Override
    public void clear(int idx) {
        state[idx] = null;
    }

    @Override
    public CloneableEntityState clone() {
        return Util.clone(this);
    }

    @Override
    public EntityState sub(int idx) {
        if (state[idx] == null) {
            state[idx] = new ArrayEntityState();
        }
        return (EntityState) state[idx];
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
            if (initializer != null) {
                for (int i = curSize; i < wantedSize; i++) {
                    initializer.accept(sub(i));
                }
            }
        }
        return this;
    }

}
