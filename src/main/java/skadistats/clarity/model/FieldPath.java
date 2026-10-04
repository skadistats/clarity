package skadistats.clarity.model;

import skadistats.clarity.model.s1.S1FieldPath;
import skadistats.clarity.model.s2.S2FieldPath;

/**
 * Identifies one property within an entity's {@link skadistats.clarity.state.EntityState EntityState}.
 *
 * <p>Source 1 uses {@link S1FieldPath}, a single index into the class's
 * receive props. Source 2 uses {@link S2FieldPath}, a path of up to
 * {@value S2FieldPath#MAX_DEPTH} indices descending through nested
 * serializers, vectors and arrays. Both implementations are immutable value
 * objects: {@code equals} and {@code hashCode} compare the path content, so
 * they can be used as map keys. A path is only meaningful for entities of the
 * class it was resolved for; the entity's own {@code getFieldPathForName}
 * resolves names. Mixing S1 and S2 paths with the wrong state fails with a
 * {@link ClassCastException}.
 */
public sealed interface FieldPath permits S1FieldPath, S2FieldPath {
}
