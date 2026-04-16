package skadistats.clarity.model.state;

import skadistats.clarity.io.s1.S1DTClass;

public enum S1EntityStateType {

    OBJECT_ARRAY {
        @Override
        public EntityState createState(S1DTClass dtClass) {
            return new ObjectArrayEntityState(dtClass.getReceiveProps().length);
        }
    },

    FLAT {
        @Override
        public EntityState createState(S1DTClass dtClass) {
            return new S1FlatEntityState(dtClass);
        }
    };

    public abstract EntityState createState(S1DTClass dtClass);

}
