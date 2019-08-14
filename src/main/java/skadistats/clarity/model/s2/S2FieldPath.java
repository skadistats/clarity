package skadistats.clarity.model.s2;

import skadistats.clarity.model.FieldPath;

public interface S2FieldPath<F extends S2FieldPath> extends FieldPath<F> {

    int get(int i);
    int last();

    default String asString() {
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
