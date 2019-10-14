package skadistats.clarity.model;

import skadistats.clarity.model.state.EntityState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
        Iterator<FieldPath> iter = state.fieldPathIterator();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        return result;
    }

}

