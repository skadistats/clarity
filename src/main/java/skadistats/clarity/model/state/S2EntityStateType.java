package skadistats.clarity.model.state;

import skadistats.clarity.io.s2.field.SerializerField;

public enum S2EntityStateType {

    NESTED_ARRAY {
        @Override
        public EntityState createState(SerializerField field, int pointerCount, FieldLayoutBuilder layoutBuilder) {
            return new NestedArrayEntityState(field, pointerCount);
        }
    },

    TREE_MAP {
        @Override
        public EntityState createState(SerializerField field, int pointerCount, FieldLayoutBuilder layoutBuilder) {
            return new TreeMapEntityState(field, pointerCount);
        }
    },

    FLAT {
        @Override
        public EntityState createState(SerializerField field, int pointerCount, FieldLayoutBuilder layoutBuilder) {
            var built = layoutBuilder.buildSerializer(field.getSerializer());
            return new FlatEntityState(field, pointerCount, built.layout(), built.totalBytes());
        }
    };

    public abstract EntityState createState(SerializerField field, int pointerCount, FieldLayoutBuilder layoutBuilder);

}
