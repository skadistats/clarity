package clarity.model;

import java.util.List;

public class DTClass {

    private final String dtName;
    private final SendTable sendTable;
    private int classId = -1;
    private List<ReceiveProp> receiveProps;

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
    }

}
