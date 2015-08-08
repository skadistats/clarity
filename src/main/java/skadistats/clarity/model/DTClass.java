package skadistats.clarity.model;

import skadistats.clarity.model.s1.ReceiveProp;
import skadistats.clarity.model.s1.SendTable;

import java.util.HashMap;
import java.util.Map;

public class DTClass {

    private final String dtName;
    private final SendTable sendTable;
    private int classId = -1;
    private ReceiveProp[] receiveProps;
    private Map<String, Integer> propsByName;
    private DTClass superClass;

    public DTClass(String dtName, SendTable sendTable) {
        this.dtName = dtName;
        this.sendTable = sendTable;
    }

    public int getClassId() {
        return classId;
    }

    public void setClassId(int classId) {
        this.classId = classId;
    }
    
    public DTClass getSuperClass() {
        return superClass;
    }

    public void setSuperClass(DTClass superClass) {
        this.superClass = superClass;
    }

    public String getDtName() {
        return dtName;
    }
    
    public boolean instanceOf(String dtName) {
        DTClass s = this;
        while (s != null) {
            if (s.getDtName().equals(dtName)) {
                return true;
            }
            s = s.superClass;
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
