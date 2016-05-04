package skadistats.clarity.model;

public class FieldPath implements Comparable<FieldPath> {

    public final int[] path;
    public int last;

    public FieldPath() {
        path = new int[6];
        path[0] = -1;
        last = 0;
    }

    public FieldPath(int... elements) {
        path = new int[6];
        last = Math.min(6, elements.length) - 1;
        System.arraycopy(elements, 0, path, 0, last + 1);
    }


    public FieldPath(FieldPath other) {
        path = new int[6];
        last = other.last;
        System.arraycopy(other.path, 0, path, 0, last + 1);
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
        for (int i = 0; i <= last; i++) {
            if (path[i] != fieldPath.path[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (int i = 0; i <= last; i++) {
            result = 31 * result + path[i];
        }
        return result;
    }

    @Override
    public int compareTo(FieldPath o) {
        if (this == o) return 0;
        int n = Math.min(last, o.last);
        for (int i = 0; i <= n; i++) {
            int r = Integer.compare(path[i], o.path[i]);
            if (r != 0) {
                return r;
            }
        }
        return Integer.compare(last, o.last);
    }
}
