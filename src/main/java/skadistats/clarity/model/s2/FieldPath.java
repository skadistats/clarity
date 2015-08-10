package skadistats.clarity.model.s2;

import java.util.Arrays;

public class FieldPath {

    final int[] path;
    int last;

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
}
