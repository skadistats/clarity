package skadistats.clarity.model;

import skadistats.clarity.model.state.EntityState;

public interface DTClass<F extends FieldPath> {

    String getDtName();

    int getClassId();
    void setClassId(int classId);

    EntityState getEmptyState();

    String getNameForFieldPath(F fp);
    F getFieldPathForName(String property);

}

