package skadistats.clarity.model.state;

import java.util.function.Consumer;

public interface ArrayEntityState {

    int length();

    boolean has(int idx);

    Object get(int idx);
    void set(int idx, Object value);
    void clear(int idx);

    boolean isSub(int idx);
    ArrayEntityState sub(int idx);

    ArrayEntityState capacity(int wantedSize, boolean shrinkIfNeeded, Consumer<ArrayEntityState> initializer);

    default ArrayEntityState capacity(int wantedSize) {
        return capacity(wantedSize, false, null);
    }

    default ArrayEntityState capacity(int wantedSize, boolean shrinkIfNeeded) {
        return capacity(wantedSize, shrinkIfNeeded, null);
    }

    default ArrayEntityState capacity(int wantedSize, Consumer<ArrayEntityState> initializer) {
        return capacity(wantedSize, false, initializer);
    }

}
