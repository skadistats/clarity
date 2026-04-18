package skadistats.clarity.model.s2;

public interface S2FieldPathBuilder {

    void set(int i, int v);
    int get(int i);
    void down();
    void up(int n);
    int last();

    void inc(int i, int n);
    void inc(int n);
    void cur(int v);
    int cur();

    S2FieldPath snapshot();

}
