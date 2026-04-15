package skadistats.clarity.bench.trace;

import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.state.StateMutation;

public record Mutation(int stateId, FieldPath fp, StateMutation mutation) {
}
