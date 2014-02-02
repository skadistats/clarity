package clarity.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DTClass {

    private final String dtName;
    private final SendTable sendTable;
    private int classId = -1;
    private List<ReceiveProp> receiveProps;
    private Map<String, Integer> propsByName;
    
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

    public String getDtName() {
        return dtName;
    }

    public SendTable getSendTable() {
        return sendTable;
    }

    public List<ReceiveProp> getReceiveProps() {
        return receiveProps;
    }

    public void setReceiveProps(List<ReceiveProp> receiveProps) {
        this.receiveProps = receiveProps;
        this.propsByName = new HashMap<String, Integer>();
        for(int i = 0; i < this.receiveProps.size(); ++i)
            this.propsByName.put(this.receiveProps.get(i).getVarName(), i);
        
    }
    
    public Integer getPropertyIndex(String name){
        return this.propsByName.get(name);
    }
}
