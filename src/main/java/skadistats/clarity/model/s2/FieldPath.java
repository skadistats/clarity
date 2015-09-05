package skadistats.clarity.model.s2;

import java.util.Arrays;

public class FieldPath {

    public final int[] path;
    public int last;

    public FieldPath() {
        path = new int[6];
        path[0] = -1;
        last = 0;
    }

    public FieldPath(FieldPath other) {
        path = Arrays.copyOf(other.path, 6);
        last = other.last;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= last; i++) {
            if (i != 0) {
                sb.append('/');
            }
            sb.append(path[i]);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldPath fieldPath = (FieldPath) o;
        if (last != fieldPath.last) return false;
        for (int i = 0; i < last; i++) {
            if (path[i] != fieldPath.path[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (int i = 0; i < last; i++) {
            result = 31 * result + path[i];
        }
        return result;
    }

}
