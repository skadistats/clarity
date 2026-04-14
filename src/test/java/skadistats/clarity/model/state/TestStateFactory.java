package skadistats.clarity.model.state;

import skadistats.clarity.io.s2.field.SerializerField;

@FunctionalInterface
public interface TestStateFactory {

    EntityState create(SerializerField root, int pointerCount);

    String NESTED_ARRAY = "NESTED_ARRAY";
    String TREE_MAP = "TREE_MAP";
    String FLAT = "FLAT";

    static TestStateFactory of(String name) {
        return switch (name) {
            case NESTED_ARRAY -> NestedArrayEntityState::new;
            case TREE_MAP -> TreeMapEntityState::new;
            case FLAT -> (root, pc) -> {
                var built = new FieldLayoutBuilder().buildSerializer(root.getSerializer());
                return new FlatEntityState(root, pc, built.layout(), built.totalBytes());
            };
            default -> throw new IllegalArgumentException(name);
        };
    }
}
