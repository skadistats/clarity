package skadistats.clarity.model.s1;

import skadistats.clarity.model.DTClass;

import java.util.HashMap;
import java.util.Map;

public class S1DTClass implements DTClass {

    private final String dtName;
    private final SendTable sendTable;
    private int classId = -1;
    private ReceiveProp[] receiveProps;
    private Map<String, Integer> propsByName;
    private S1DTClass superClass;

    public S1DTClass(String dtName, SendTable sendTable) {
        this.dtName = dtName;
        this.sendTable = sendTable;
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
    public Object[] getEmptyStateArray() {
        return new Object[receiveProps.length];
    }

    public S1DTClass getSuperClass() {
        return superClass;
    }

    public void setSuperClass(S1DTClass superClass) {
        this.superClass = superClass;
    }

    public String getDtName() {
        return dtName;
    }
    
    public boolean instanceOf(String dtName) {
        S1DTClass s = this;
        while (s != null) {
            if (s.getDtName().equals(dtName)) {
                return true;
            }
            s = s.getSuperClass();
        }
        return false;
    }
    
    public SendTable getSendTable() {
        return sendTable;
    }

    public ReceiveProp[] getReceiveProps() {
        return receiveProps;
    }

    public void setReceiveProps(ReceiveProp[] receiveProps) {
        this.receiveProps = receiveProps;
        this.propsByName = new HashMap<String, Integer>();
        for(int i = 0; i < receiveProps.length; ++i) {
            this.propsByName.put(receiveProps[i].getVarName(), i);
        }
    }
    
    public Integer getPropertyIndex(String name){
        return this.propsByName.get(name);
    }

}
