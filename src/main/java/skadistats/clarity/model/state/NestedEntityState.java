package skadistats.clarity.model.state;

public interface NestedEntityState {

    int length();

    boolean has(int idx);

    Object get(int idx);
    void set(int idx, Object value);
    void clear(int idx);

    boolean isSub(int idx);
    NestedEntityState sub(int idx);

    NestedEntityState capacity(int wantedSize, boolean shrinkIfNeeded);

}
