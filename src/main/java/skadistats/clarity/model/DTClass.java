package skadistats.clarity.model;

public interface DTClass {

    String getDtName();

    int getClassId();
    void setClassId(int classId);

    int getFieldNum();

    @Deprecated
    Integer getPropertyIndex(String property);

}

