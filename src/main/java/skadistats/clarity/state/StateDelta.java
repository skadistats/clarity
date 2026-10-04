package skadistats.clarity.state;

import skadistats.clarity.model.FieldPath;

/**
 * Sparse, immutable snapshot of a subset of an {@link EntityState}'s fields.
 * Produced by {@code EntityState.captureChanged(state, fps, num)}; consumed
 * via primitive-typed getters that allocate nothing at the accessor boundary.
 *
 * Queries for a {@link FieldPath} that is not part of {@link #fields()}
 * return the zero/null default — never throw. Queries that request the
 * wrong primitive type (e.g. {@code getInt} on a float-typed field) also
 * return the zero default. Callers are expected to know the type of the
 * field path they captured.
 */
public interface StateDelta {

    /**
     * @return the captured field paths
     */
    FieldPath[] fields();

    /** The captured {@code int} value for {@code fp}, or 0 if not captured or not int-typed. */
    int getInt(FieldPath fp);

    /** The captured {@code long} value for {@code fp}, or 0 if not captured or not long-typed. */
    long getLong(FieldPath fp);

    /** The captured {@code float} value for {@code fp}, or 0 if not captured or not float-typed. */
    float getFloat(FieldPath fp);

    /** The captured object value for {@code fp}, or {@code null} if not captured. */
    Object getObject(FieldPath fp);
}
