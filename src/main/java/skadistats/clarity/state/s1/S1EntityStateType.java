package skadistats.clarity.state.s1;

import skadistats.clarity.model.s1.S1DTClass;
import skadistats.clarity.state.EntityState;

public enum S1EntityStateType {

    OBJECT_ARRAY {
        @Override
        public EntityState createState(S1DTClass dtClass) {
            return new S1ObjectArrayEntityState(dtClass.getReceiveProps().length);
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
