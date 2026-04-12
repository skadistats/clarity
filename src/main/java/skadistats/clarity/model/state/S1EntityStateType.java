package skadistats.clarity.model.state;

import skadistats.clarity.io.s1.ReceiveProp;

public enum S1EntityStateType {

    OBJECT_ARRAY {
        @Override
        public EntityState createState(ReceiveProp[] receiveProps) {
            return new ObjectArrayEntityState(receiveProps.length);
        }
    };

    public abstract EntityState createState(ReceiveProp[] receiveProps);

}
