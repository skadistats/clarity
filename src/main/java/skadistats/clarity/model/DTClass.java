package skadistats.clarity.model;

import skadistats.clarity.model.state.CloneableEntityState;
import skadistats.clarity.model.state.EntityState;

import java.util.List;

public interface DTClass {

    String getDtName();

    int getClassId();
    void setClassId(int classId);

    CloneableEntityState getEmptyStateArray();

    String getNameForFieldPath(FieldPath fp);
    FieldPath getFieldPathForName(String property);

    <T> T getValueForFieldPath(FieldPath fp, EntityState state);

    List<FieldPath> collectFieldPaths(EntityState state);
    String dumpState(String title, EntityState state);

}

