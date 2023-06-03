package skadistats.clarity.model;

import skadistats.clarity.io.s1.S1DTClass;
import skadistats.clarity.io.s2.S2DTClass;
import skadistats.clarity.model.state.EntityState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public interface DTClass {

    String getDtName();

    int getClassId();
    void setClassId(int classId);

    EntityState getEmptyState();

    String getNameForFieldPath(FieldPath fp);
    FieldPath getFieldPathForName(String property);

    @Deprecated
    default List<FieldPath> collectFieldPaths(EntityState state) {
        List<FieldPath> result = new ArrayList<>();
        var iter = state.fieldPathIterator();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        return result;
    }

    default <V> V evaluate(Function<S1DTClass, V> s1, Function<S2DTClass, V> s2) {
        if (this instanceof S2DTClass) {
            return s2.apply((S2DTClass) this);
        } else {
            return s1.apply((S1DTClass) this);
        }
    }

}

