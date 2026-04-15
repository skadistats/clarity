package skadistats.clarity.bench.trace;

import skadistats.clarity.io.s2.field.SerializerField;

public record BirthRecipe(
        int stateId,
        BirthKind kind,
        SerializerField field,    // non-null for EMPTY, null for COPY_OF
        int srcStateId,           // -1 for EMPTY, valid stateId for COPY_OF
        Mutation[] setupMutations
) {
    public static final Mutation[] NO_SETUP = new Mutation[0];
}
