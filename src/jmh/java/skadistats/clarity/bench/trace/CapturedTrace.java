package skadistats.clarity.bench.trace;

import java.util.List;

public record CapturedTrace(
        List<BirthRecipe> births,
        Mutation[] updateMutations,
        int pointerCount
) {
}
