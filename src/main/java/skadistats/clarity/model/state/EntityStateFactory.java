package skadistats.clarity.model.state;

import skadistats.clarity.decoder.s1.ReceiveProp;
import skadistats.clarity.decoder.s2.Serializer;
import skadistats.clarity.model.FieldPath;

import java.util.ArrayList;
import java.util.List;

public class EntityStateFactory {

    public static EntityState forS1(ReceiveProp[] receiveProps) {
        return new NestedArrayEntityState(
                receiveProps.length,
                (s, fp) -> receiveProps[fp.path[0]].getVarName(),
                (s, fp) -> s.get(fp.path[0]),
                (s, fp, d) -> s.set(fp.path[0], d),
                (s) -> {
                    ArrayList<FieldPath> result = new ArrayList<>(s.length());
                    for (int i = 0; i < s.length(); i++) {
                        result.add(new FieldPath(i));
                    }
                    return result;
                });
    }

    public static EntityState forS2(Serializer serializer) {
        NestedArrayEntityState state = new NestedArrayEntityState(
                serializer.getFieldCount(),
                (s, fp) -> {
                    List<String> parts = new ArrayList<>();
                    serializer.accumulateName(fp, 0, parts);
                    return String.join(".", parts);
                },
                (s, fp) -> serializer.getValueForFieldPath(fp, 0, s),
                (s, fp, d) -> serializer.setValueForFieldPath(fp, 0, s, d),
                (s) -> {
                    List<FieldPath> result = new ArrayList<>(s.length());
                    serializer.collectFieldPaths(new FieldPath(), result, s);
                    return result;
                });
        serializer.initInitialState(state);
        return state;
    }

}
