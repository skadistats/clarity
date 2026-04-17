package skadistats.clarity.state.s2;

public interface S2NestedEntityState {

    int length();

    boolean has(int idx);

    Object get(int idx);
    void set(int idx, Object value);
    void clear(int idx);

    boolean isSub(int idx);
    S2NestedEntityState sub(int idx);

    S2NestedEntityState capacity(int wantedSize, boolean shrinkIfNeeded);

}
