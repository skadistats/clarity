package skadistats.clarity.bench.trace;

import skadistats.clarity.model.state.EntityState;
import skadistats.clarity.model.state.FieldLayoutBuilder;
import skadistats.clarity.model.state.S2EntityStateType;

/**
 * Builds the {@code EntityState[]} that the trace bench will dispatch into. For each
 * {@link BirthRecipe} (in birth order), constructs a fresh state of the requested impl
 * (EMPTY) or copies an already-materialized one (COPY_OF), then replays the birth's setup
 * mutations to bring the state to its post-create shape.
 */
public final class BirthMaterializer {

    private BirthMaterializer() {
    }

    public static EntityState[] materialize(CapturedTrace trace, S2EntityStateType impl) {
        var births = trace.births();
        var states = new EntityState[births.size()];
        var layoutBuilder = new FieldLayoutBuilder();
        var pointerCount = trace.pointerCount();

        for (var b : births) {
            var state = switch (b.kind()) {
                case EMPTY -> impl.createState(b.field(), pointerCount, layoutBuilder);
                case COPY_OF -> states[b.srcStateId()].copy();
            };
            states[b.stateId()] = state;
            for (var m : b.setupMutations()) {
                state.applyMutation(m.fp(), m.mutation());
            }
        }
        return states;
    }
}
