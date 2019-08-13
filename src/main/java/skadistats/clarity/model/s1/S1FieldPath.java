package skadistats.clarity.model.s1;

import skadistats.clarity.model.FieldPath;

public class S1FieldPath implements FieldPath<S1FieldPath> {

    private final int idx;

    public S1FieldPath(int idx) {
        this.idx = idx;
    }

    @Override
    public int cur() {
        return idx;
    }

    @Override
    public String toString() {
        return String.valueOf(idx);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        S1FieldPath that = (S1FieldPath) o;
        return idx == that.idx;
    }

    @Override
    public int hashCode() {
        return idx;
    }

    @Override
    public int compareTo(S1FieldPath o) {
        return idx - o.idx;
    }

}
