package skadistats.clarity.processor.sendtables;

import skadistats.clarity.event.Provides;
import skadistats.clarity.model.DTClass;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

@Provides(UsesDTClasses.class)
public class DTClasses {

    final Map<Integer, DTClass> byClassId = new TreeMap<>();
    private final Map<String, DTClass> byDtName = new TreeMap<>();
    int classBits;

    @OnDTClass
    public void onDTClass(DTClass dtClass) {
        byDtName.put(dtClass.getDtName(), dtClass);
    }

    public DTClass forClassId(int id) {
        return byClassId.get(id);
    }

    public DTClass forDtName(String dtName) {
        return byDtName.get(dtName);
    }

    public Iterator<DTClass> iterator() {
        return byClassId.values().iterator();
    }

    public int getClassCount() {
        return byClassId.size();
    }

    public int getClassBits() {
        return classBits;
    }

}
