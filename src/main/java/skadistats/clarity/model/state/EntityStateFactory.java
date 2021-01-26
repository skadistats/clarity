package skadistats.clarity.model.state;

import skadistats.clarity.io.s1.ReceiveProp;
import skadistats.clarity.io.s2.field.SerializerField;

public class EntityStateFactory {

    public static EntityState forS1(ReceiveProp[] receiveProps) {
        return new ObjectArrayEntityState(receiveProps.length);
    }

    public static EntityState forS2(SerializerField field) {
        return new NestedArrayEntityState(field);
        //return new TreeMapEntityState();
    }

}
