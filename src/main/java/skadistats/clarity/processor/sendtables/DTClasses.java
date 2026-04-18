package skadistats.clarity.processor.sendtables;

import skadistats.clarity.event.Insert;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.state.EntityStateFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

@Provides(UsesDTClasses.class)
public class DTClasses {

    @Insert
    EntityStateFactory entityStateFactory;

    final Map<Integer, DTClass> byClassId = new TreeMap<>();
    private final Map<String, DTClass> byDtName = new TreeMap<>();
    int classBits;
    int pointerCount;

    @OnDTClass
    public void onDTClass(DTClass dtClass) {
        dtClass.setEntityStateFactory(entityStateFactory);
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

    public int getPointerCount() {
        return pointerCount;
    }

    public EntityStateFactory getEntityStateFactory() {
        return entityStateFactory;
    }

}
