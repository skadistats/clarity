package clarity.match;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import clarity.model.DTClass;
import clarity.model.SendTable;

public class DTClassCollection {

    private final Map<Integer, DTClass> byClassId = new TreeMap<Integer, DTClass>();
    private final Map<String, DTClass> byDtName = new TreeMap<String, DTClass>();

    public void add(DTClass dtClass) {
        byDtName.put(dtClass.getDtName(), dtClass);
    }

    public void setClassIdForDtName(String dtName, int classId) {
        DTClass dt = forDtName(dtName);
        dt.setClassId(classId);
        byClassId.put(classId, dt);
    }

    public DTClass forClassId(int id) {
        return byClassId.get(id);
    }

    public DTClass forDtName(String dtName) {
        return byDtName.get(dtName);
    }

    public SendTable sendTableForDtName(String dtName) {
        return byDtName.get(dtName).getSendTable();
    }

    public Iterator<DTClass> iterator() {
        return byClassId.values().iterator();
    }

    public int size() {
        return byClassId.size();
    }
    
}
