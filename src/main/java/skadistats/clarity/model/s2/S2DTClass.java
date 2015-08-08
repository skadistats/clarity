package skadistats.clarity.model.s2;

import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.s1.ReceiveProp;
import skadistats.clarity.model.s1.SendTable;

public class S2DTClass implements DTClass {


    private final Serializer serializer;
    private int classId = -1;

    public S2DTClass(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public String getDtName() {
        return serializer.getId().getName();
    }

    @Override
    public void setSuperClass(DTClass dtClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DTClass getSuperClass() {
        return null;
    }

    @Override
    public int getClassId() {
        return classId;
    }

    @Override
    public void setClassId(int classId) {
        this.classId = classId;
    }

    @Override
    public void setReceiveProps(ReceiveProp[] receiveProps) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ReceiveProp[] getReceiveProps() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integer getPropertyIndex(String property) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SendTable getSendTable() {
        throw new UnsupportedOperationException();
    }

}
