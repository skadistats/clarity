package skadistats.clarity.processor.sendtables;

import skadistats.clarity.decoder.SendTableFlattener;
import skadistats.clarity.decoder.Util;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.*;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.wire.common.proto.Demo;
import skadistats.clarity.wire.s1.proto.Netmessages;

import java.util.*;

@Provides({UsesDTClasses.class})
public class DTClasses {

    private final Map<Integer, DTClass> byClassId = new TreeMap<Integer, DTClass>();
    private final Map<String, DTClass> byDtName = new TreeMap<String, DTClass>();
    private int classBits;

    @OnMessage(Netmessages.CSVCMsg_SendTable.class)
    public void onSendTable(Context ctx, Netmessages.CSVCMsg_SendTable message) {

        LinkedList<SendProp> props = new LinkedList<SendProp>();
        SendTable st = new SendTable(
            message.getNetTableName(),
            message.getNeedsDecoder(),
            props
        );

        for (Netmessages.CSVCMsg_SendTable.sendprop_t sp : message.getPropsList()) {
            props.add(
                new SendProp(
                    st,
                    sp.getType() == PropType.ARRAY.ordinal() ? props.peekLast() : null,
                    sp.getType(),
                    sp.getVarName(),
                    sp.getFlags(),
                    sp.getPriority(),
                    sp.getDtName(),
                    sp.getNumElements(),
                    sp.getLowValue(),
                    sp.getHighValue(),
                    sp.getNumBits()
                )
            );
        }
        byDtName.put(message.getNetTableName(), new DTClass(message.getNetTableName(), st));
    }

    @OnMessage(Demo.CDemoClassInfo.class)
    public void onClassInfo(Context ctx, Demo.CDemoClassInfo message) {
        for (Demo.CDemoClassInfo.class_t ct : message.getClassesList()) {
            DTClass dt = forDtName(ct.getTableName());
            dt.setClassId(ct.getClassId());
            byClassId.put(ct.getClassId(), dt);
        }
    }

    @OnMessage(Demo.CDemoSyncTick.class)
    public void onSyncTick(Context ctx, Demo.CDemoSyncTick message) {
        // last packet of the prologue: compile receive tables!
        for (DTClass dtc : byClassId.values()) {
            if (!dtc.getSendTable().isDecoderNeeded()) {
                continue;
            }
            List<ReceiveProp> rps = new SendTableFlattener(this, dtc.getSendTable()).flatten();
            dtc.setReceiveProps(rps.toArray(new ReceiveProp[] {}));
        }

        // last packet of the prologue: set super classes
        for (DTClass dtc : byClassId.values()) {
            String superClassName = dtc.getSendTable().getBaseClass();
            if (superClassName != null) {
                dtc.setSuperClass(byDtName.get(superClassName));
            }
        }

        classBits = Util.calcBitsNeededFor(size() - 1);
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

    public int getClassBits() {
        return classBits;
    }

}
