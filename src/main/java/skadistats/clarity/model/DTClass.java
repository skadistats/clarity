package skadistats.clarity.model;

import skadistats.clarity.model.s1.ReceiveProp;
import skadistats.clarity.model.s1.SendTable;

public interface DTClass {

    String getDtName();

    int getClassId();
    void setClassId(int classId);

    int getFieldNum();


    @Deprecated
    void setSuperClass(DTClass dtClass);
    @Deprecated
    DTClass getSuperClass();

    @Deprecated
    void setReceiveProps(ReceiveProp[] receiveProps);
    @Deprecated
    ReceiveProp[] getReceiveProps();

    @Deprecated
    Integer getPropertyIndex(String property);

    @Deprecated
    SendTable getSendTable();

}

