package skadistats.clarity.processor.sendtables;

import skadistats.clarity.decoder.Util;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.wire.common.proto.Demo;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

@Provides({UsesDTClasses.class})
public class DTClasses {

    final Map<Integer, DTClass> byClassId = new TreeMap<>();
    private final Map<String, DTClass> byDtName = new TreeMap<>();
    private int classBits;

    @OnDTClass
    public void onDTClass(Context ctx, DTClass dtClass) {
        byDtName.put(dtClass.getDtName(), dtClass);
    }

    @OnMessage(Demo.CDemoSyncTick.class)
    public void onSyncTick(Context ctx, Demo.CDemoSyncTick message) {
        classBits = Util.calcBitsNeededFor(byClassId.size() - 1);
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

    public int getClassBits() {
        return classBits;
    }

}
