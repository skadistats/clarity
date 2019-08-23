package skadistats.clarity.model.s1;

import skadistats.clarity.model.FieldPath;

public class S1FieldPath implements FieldPath<S1FieldPath> {

    private final int idx;

    public S1FieldPath(int idx) {
        this.idx = idx;
    }

    public int idx() {
        return idx;
    }

    @Override
    public String toString() {
        return String.valueOf(idx);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof S1FieldPath) {
            return idx == ((S1FieldPath) o).idx;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(idx);
    }

    @Override
    public int compareTo(S1FieldPath o) {
        return Integer.compare(idx, o.idx);
    }

}
