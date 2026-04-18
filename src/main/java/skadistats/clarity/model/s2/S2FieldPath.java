package skadistats.clarity.model.s2;

import skadistats.clarity.model.FieldPath;

public sealed interface S2FieldPath extends FieldPath, Comparable<S2FieldPath> permits S2LongFieldPath {

    int MAX_DEPTH = 7;

    int get(int i);
    int last();

    /**
     * Returns a path that extends this one by one level, with {@code index}
     * as the value at the new depth.
     */
    S2FieldPath childAt(int index);

    /**
     * Returns a path that sorts strictly greater than every descendant of
     * this path at the given {@code depth}. Suitable as an exclusive upper
     * bound for {@code subMap}-style range queries: everything keyed under
     * the subtree rooted at {@code this} (considering depths {@code 0..depth})
     * is strictly less than the returned path.
     */
    S2FieldPath upperBoundForSubtreeAt(int depth);

    static S2FieldPathBuilder newBuilder() {
        return new S2LongFieldPathBuilder();
    }

    /**
     * Builds a path from the given component indices. {@code of()} with no
     * arguments produces a zero-depth path with component 0 uninitialised.
     */
    static S2FieldPath of(int... indices) {
        var b = newBuilder();
        for (var i = 0; i < indices.length; i++) {
            if (i > 0) b.down();
            b.set(i, indices[i]);
        }
        return b.snapshot();
    }

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
