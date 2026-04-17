package skadistats.clarity.model.s2;

import skadistats.clarity.model.FieldPath;

public sealed interface S2FieldPath extends FieldPath permits S2LongFieldPath, S2ModifiableFieldPath {

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
