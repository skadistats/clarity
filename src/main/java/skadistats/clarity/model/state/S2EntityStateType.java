package skadistats.clarity.model.state;

import skadistats.clarity.io.s2.field.SerializerField;

public enum S2EntityStateType {

    NESTED_ARRAY {
        @Override
        public EntityState createState(SerializerField field) {
            return new NestedArrayEntityState(field);
        }
    },

    TREE_MAP {
        @Override
        public EntityState createState(SerializerField field) {
            return new TreeMapEntityState(field);
        }
    };

    public abstract EntityState createState(SerializerField field);

}
