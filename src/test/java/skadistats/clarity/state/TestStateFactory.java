package skadistats.clarity.state;

import skadistats.clarity.model.s2.field.SerializerField;
import skadistats.clarity.state.s2.S2FlatEntityState;
import skadistats.clarity.state.s2.S2NestedArrayEntityState;
import skadistats.clarity.state.s2.S2TreeMapEntityState;

@FunctionalInterface
public interface TestStateFactory {

    EntityState create(SerializerField root, int pointerCount);

    String NESTED_ARRAY = "NESTED_ARRAY";
    String TREE_MAP = "TREE_MAP";
    String FLAT = "FLAT";

    static TestStateFactory of(String name) {
        return switch (name) {
            case NESTED_ARRAY -> S2NestedArrayEntityState::new;
            case TREE_MAP -> S2TreeMapEntityState::new;
            case FLAT -> (root, pc) -> {
                var built = new FieldLayoutBuilder().buildSerializer(root.getSerializer());
                return new S2FlatEntityState(root, pc, built.layout(), built.totalBytes());
            };
            default -> throw new IllegalArgumentException(name);
        };
    }
}
