package skadistats.clarity.model.state;

import skadistats.clarity.io.s1.ReceiveProp;
import skadistats.clarity.io.s2.field.SerializerField;

public class EntityStateFactory {

    private final S1EntityStateType s1Type;
    private final S2EntityStateType s2Type;

    public EntityStateFactory(S1EntityStateType s1Type, S2EntityStateType s2Type) {
        this.s1Type = s1Type;
        this.s2Type = s2Type;
    }

    public EntityState forS1(ReceiveProp[] receiveProps) {
        return s1Type.createState(receiveProps);
    }

    public EntityState forS2(SerializerField field) {
        return s2Type.createState(field);
    }

}
