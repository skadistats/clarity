package skadistats.clarity.model;

public interface DTClass {

    String getDtName();

    int getClassId();
    void setClassId(int classId);

    Object[] getEmptyStateArray();

    @Deprecated
    Integer getPropertyIndex(String property);

}

