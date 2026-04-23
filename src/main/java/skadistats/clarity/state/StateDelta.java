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

    FieldPath[] fields();

    int getInt(FieldPath fp);

    long getLong(FieldPath fp);

    float getFloat(FieldPath fp);

    Object getObject(FieldPath fp);
}
