package skadistats.clarity.model;

public interface DTClass {

    String getDtName();

    int getClassId();
    void setClassId(int classId);

    Object[] getEmptyStateArray();

    FieldPath getFieldPathForName(String property);
    <T> T getValueForFieldPath(FieldPath fp, Object[] state);

    String dumpState(String title, Object[] state);

}

