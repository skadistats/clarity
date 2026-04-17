package skadistats.clarity.bench.trace;

import skadistats.clarity.model.FieldPath;
import skadistats.clarity.state.StateMutation;

public record Mutation(int stateId, FieldPath fp, StateMutation mutation) {
}
