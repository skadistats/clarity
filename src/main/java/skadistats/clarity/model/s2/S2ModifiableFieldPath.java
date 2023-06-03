package skadistats.clarity.model.s2;

public interface S2ModifiableFieldPath extends S2FieldPath {

    static S2ModifiableFieldPath newInstance() {
        return new S2LongModifiableFieldPath();
    }

    void set(int i, int v);

    int get(int i);

    void down();

    void up(int n);

    int last();

    S2FieldPath unmodifiable();

    default void inc(int i, int n) {
        set(i, get(i) + n);
    }

    default void inc(int n) {
        inc(last(), n);
    }

    default void cur(int v) {
        set(last(), v);
    }

    default int cur() {
        return get(last());
    }

}
