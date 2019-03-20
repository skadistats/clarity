package skadistats.clarity.model.state;

import java.util.function.Consumer;

public interface EntityState {

    int length();

    boolean has(int idx);

    Object get(int idx);
    void set(int idx, Object value);
    void clear(int idx);

    EntityState clone();

    EntityState sub(int idx);

    EntityState capacity(int wantedSize, boolean shrinkIfNeeded, Consumer<EntityState> initializer);

    default EntityState capacity(int wantedSize) {
        return capacity(wantedSize, false, null);
    }

    default EntityState capacity(int wantedSize, boolean shrinkIfNeeded) {
        return capacity(wantedSize, shrinkIfNeeded, null);
    }

    default EntityState capacity(int wantedSize, Consumer<EntityState> initializer) {
        return capacity(wantedSize, false, initializer);
    }

}
