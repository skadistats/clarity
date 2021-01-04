package skadistats.clarity.model.state;

import skadistats.clarity.io.s1.ReceiveProp;
import skadistats.clarity.io.s2.field.RecordField;

public class EntityStateFactory {

    public static EntityState forS1(ReceiveProp[] receiveProps) {
        return new ObjectArrayEntityState(receiveProps.length);
    }

    public static EntityState forS2(RecordField serializer) {
        return new NestedArrayEntityState(serializer);
        //return new TreeMapEntityState();
    }

}
