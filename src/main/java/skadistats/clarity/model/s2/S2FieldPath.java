package skadistats.clarity.model.s2;

import skadistats.clarity.model.FieldPath;

public interface S2FieldPath extends FieldPath {

    int get(int i);
    int last();

    default String asString() {
        final var sb = new StringBuilder();
        for (var i = 0; i <= last(); i++) {
            if (i != 0) {
                sb.append('/');
            }
            sb.append(get(i));
        }
        return sb.toString();
    }

}
