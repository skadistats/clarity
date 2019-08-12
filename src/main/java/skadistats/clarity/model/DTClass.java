package skadistats.clarity.model;

import skadistats.clarity.model.state.EntityState;

public interface DTClass {

    String getDtName();

    int getClassId();
    void setClassId(int classId);

    EntityState getEmptyState();

    String getNameForFieldPath(FieldPath fp);
    FieldPath getFieldPathForName(String property);

}

