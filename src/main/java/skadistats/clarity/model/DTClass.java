package skadistats.clarity.model;

import java.util.List;

public interface DTClass {

    String getDtName();

    int getClassId();
    void setClassId(int classId);

    Object[] getEmptyStateArray();

    String getNameForFieldPath(FieldPath fp);
    FieldPath getFieldPathForName(String property);

    <T> T getValueForFieldPath(FieldPath fp, Object[] state);

    List<FieldPath> collectFieldPaths(Object[] state);
    String dumpState(String title, Object[] state);

}

