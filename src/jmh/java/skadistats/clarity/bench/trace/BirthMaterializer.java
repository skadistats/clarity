package skadistats.clarity.bench.trace;

import skadistats.clarity.model.s1.S1DTClass;
import skadistats.clarity.model.s2.S2DTClass;
import skadistats.clarity.state.EntityState;
import skadistats.clarity.state.FieldLayoutBuilder;
import skadistats.clarity.state.s1.S1EntityStateType;
import skadistats.clarity.state.s2.S2EntityStateType;

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
        return materialize(trace, S1EntityStateType.FLAT, impl);
    }

    public static EntityState[] materialize(CapturedTrace trace, S1EntityStateType s1Impl) {
        return materialize(trace, s1Impl, S2EntityStateType.FLAT);
    }

    public static EntityState[] materialize(CapturedTrace trace, S1EntityStateType s1Impl, S2EntityStateType s2Impl) {
        var births = trace.births();
        var states = new EntityState[births.size()];
        var layoutBuilder = new FieldLayoutBuilder();
        var pointerCount = trace.pointerCount();

        for (var b : births) {
            var state = switch (b.kind()) {
                case EMPTY -> {
                    var cls = b.cls();
                    if (cls instanceof S1DTClass s1c) {
                        yield s1Impl.createState(s1c);
                    } else if (cls instanceof S2DTClass s2c) {
                        yield s2Impl.createState(s2c.getField(), pointerCount, layoutBuilder);
                    } else {
                        throw new IllegalStateException("unknown DTClass type: " + cls.getClass());
                    }
                }
                case COPY_OF -> states[b.srcStateId()].copy();
            };
            states[b.stateId()] = state;
            for (var m : b.setupMutations()) {
                EntityState.applyMutation(state, m.fp(), m.mutation());
            }
        }
        return states;
    }
}
