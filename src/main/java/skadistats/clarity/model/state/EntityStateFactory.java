package skadistats.clarity.model.state;

import skadistats.clarity.decoder.s1.ReceiveProp;
import skadistats.clarity.decoder.s2.Serializer;
import skadistats.clarity.model.s1.S1FieldPath;
import skadistats.clarity.model.s2.S2FieldPath;

public class EntityStateFactory {

    public static EntityState<S1FieldPath> forS1(ReceiveProp[] receiveProps) {
        return new ObjectArrayEntityState(receiveProps.length);
    }

    public static EntityState<S2FieldPath> forS2(Serializer serializer) {
        return new NestedArrayEntityState(serializer);
        //return new TreeMapEntityState();
    }

}
