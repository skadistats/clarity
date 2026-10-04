package skadistats.clarity.processor.sendtables;

import skadistats.clarity.event.Provides;
import skadistats.clarity.model.DTClass;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Registry of all {@link DTClass}es of the replay, by class id and by DT name.
 * <p>
 * Active when a processor declares {@link UsesDTClasses}. Populated from the send tables / serializers of the
 * replay, so it is complete once {@link OnDTClassesComplete} was raised.
 */
@Provides(UsesDTClasses.class)
public class DTClasses {

    final Map<Integer, DTClass> byClassId = new TreeMap<>();
    private final Map<String, DTClass> byDtName = new TreeMap<>();
    int classBits;

    /** Event handler bound by the runtime; not for direct use. */
    @OnDTClass
    public void onDTClass(DTClass dtClass) {
        byDtName.put(dtClass.getDtName(), dtClass);
    }

    /**
     * @param id the class id
     * @return the class with that id, or {@code null} if there is none
     */
    public DTClass forClassId(int id) {
        return byClassId.get(id);
    }

    /**
     * @param dtName the DT name of the class
     * @return the class with that name, or {@code null} if there is none
     */
    public DTClass forDtName(String dtName) {
        return byDtName.get(dtName);
    }

    /**
     * @return an iterator over all classes, ordered by class id
     */
    public Iterator<DTClass> iterator() {
        return byClassId.values().iterator();
    }

    /**
     * @return the number of classes
     */
    public int getClassCount() {
        return byClassId.size();
    }

    /**
     * @return the number of bits needed to encode a class id (computed from the class count)
     */
    public int getClassBits() {
        return classBits;
    }

}
