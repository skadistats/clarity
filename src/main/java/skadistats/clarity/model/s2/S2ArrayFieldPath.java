package skadistats.clarity.model.s2;

public class S2ArrayFieldPath extends S2FieldPath implements Comparable<S2ArrayFieldPath> {

    private final int[] path;
    private int last;

    public S2ArrayFieldPath() {
        path = new int[6];
        path[0] = -1;
        last = 0;
    }

    S2ArrayFieldPath(S2ArrayFieldPath other) {
        path = new int[6];
        last = other.last;
        System.arraycopy(other.path, 0, path, 0, last + 1);
    }

    @Override
    S2ArrayFieldPath copy() {
        return new S2ArrayFieldPath(this);
    }

    @Override
    public void set(int i, int v) {
        path[i] = v;
    }

    @Override
    public int get(int i) {
        return path[i];
    }

    @Override
    public void down() {
        last++;
    }

    @Override
    public void up(int n) {
        for (int i = 0; i < n; i++) {
            path[last--] = 0;
        }
    }

    @Override
    public int last() {
        return last;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        S2ArrayFieldPath fieldPath = (S2ArrayFieldPath) o;
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
    public int compareTo(S2ArrayFieldPath o) {
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