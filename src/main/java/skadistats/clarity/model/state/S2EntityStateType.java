package skadistats.clarity.model.state;

import skadistats.clarity.io.s2.field.SerializerField;

public enum S2EntityStateType {

    NESTED_ARRAY {
        @Override
        public EntityState createState(SerializerField field, int pointerCount) {
            return new NestedArrayEntityState(field, pointerCount);
        }
    },

    TREE_MAP {
        @Override
        public EntityState createState(SerializerField field, int pointerCount) {
            return new TreeMapEntityState(field, pointerCount);
        }
    };

    public abstract EntityState createState(SerializerField field, int pointerCount);

}
