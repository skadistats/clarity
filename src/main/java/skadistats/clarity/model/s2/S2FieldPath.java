package skadistats.clarity.model.s2;

import skadistats.clarity.model.FieldPath;

public abstract class S2FieldPath<F extends S2FieldPath> implements FieldPath<F> {

    public static S2FieldPath createEmpty() {
        return new S2LongFieldPath();
    }

    public static S2FieldPath createCopy(S2FieldPath other) {
        return other.copy();
    }

    abstract S2FieldPath copy();

    public abstract void set(int i, int v);

    public abstract int get(int i);

    public abstract void down();

    public abstract void up(int n);

    public abstract int last();

    public void inc(int i, int n) {
        set(i, get(i) + n);
    }

    public void inc(int n) {
        inc(last(), n);
    }

    public void cur(int v) {
        set(last(), v);
    }

    public int cur() {
        return get(last());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= last(); i++) {
            if (i != 0) {
                sb.append('/');
            }
            sb.append(get(i));
        }
        return sb.toString();
    }

}
