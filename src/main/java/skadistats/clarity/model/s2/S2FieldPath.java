package skadistats.clarity.model.s2;

import skadistats.clarity.model.FieldPath;

public abstract class S2FieldPath implements FieldPath {

    public static S2FieldPath createEmpty() {
        return new S2ArrayFieldPath();
    }

    public static S2FieldPath createCopy(S2FieldPath other) {
        return other.copy();
    }

    abstract S2FieldPath copy();

    public abstract void inc(int i, int n);

    public abstract void inc(int n);

    public abstract void down();

    public abstract void up(int n);

    public abstract int last();

    public abstract void cur(int v);

    public abstract void set(int i, int v);

    public abstract int get(int i);

}
