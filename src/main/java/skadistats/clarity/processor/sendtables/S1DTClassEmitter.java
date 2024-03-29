package skadistats.clarity.processor.sendtables;

import skadistats.clarity.ClarityException;
import skadistats.clarity.event.Event;
import skadistats.clarity.event.Insert;
import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.Provides;
import skadistats.clarity.io.Util;
import skadistats.clarity.io.s1.S1DTClass;
import skadistats.clarity.io.s1.SendProp;
import skadistats.clarity.io.s1.SendTable;
import skadistats.clarity.io.s1.SendTableFlattener;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.model.s1.PropType;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.wire.shared.demo.proto.Demo;
import skadistats.clarity.wire.shared.s1.proto.S1NetMessages;

import java.util.Iterator;
import java.util.LinkedList;

@Provides(value = {OnDTClass.class, OnDTClassesComplete.class}, engine = {EngineId.DOTA_S1, EngineId.CSGO_S1})
public class S1DTClassEmitter {

    @Insert
    private DTClasses dtClasses;
    @Insert
    private EngineType engineType;

    @InsertEvent
    private Event<OnDTClassesComplete> evClassesComplete;
    @InsertEvent
    private Event<OnDTClass> evDtClass;

    @OnMessage(S1NetMessages.CSVCMsg_SendTable.class)
    public void onSendTable(S1NetMessages.CSVCMsg_SendTable message) {

        if (message.getIsEnd()) {
            return;
        }

        var props = new LinkedList<SendProp>();
        var st = new SendTable(
            message.getNetTableName(),
            props
        );

        for (var sp : message.getPropsList()) {
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
        DTClass dtClass = new S1DTClass(message.getNetTableName(), st);
        evDtClass.raise(dtClass);
    }

    @OnMessage(Demo.CDemoClassInfo.class)
    public void onClassInfo(Demo.CDemoClassInfo message) {
        for (var ct : message.getClassesList()) {
            var dt = dtClasses.forDtName(ct.getTableName());
            if (dt == null) {
                throw new ClarityException("DTClass for '%s' not found.", ct.getTableName());
            }
            dt.setClassId(ct.getClassId());
            dtClasses.byClassId.put(ct.getClassId(), dt);
        }
        dtClasses.classBits = Util.calcBitsNeededFor(dtClasses.byClassId.size() - 1);
    }

    @OnMessage(Demo.CDemoSyncTick.class)
    public void onSyncTick(Demo.CDemoSyncTick message) {
        var iter = dtClasses.iterator();
        while (iter.hasNext()) {
            var dtc = (S1DTClass) iter.next();
            var flattened = new SendTableFlattener(dtClasses, dtc.getSendTable()).flatten();
            dtc.setReceiveProps(flattened.receiveProps);
            dtc.setIndexMapping(flattened.indexMapping);
        }
        iter = dtClasses.iterator();
        while (iter.hasNext()) {
            var dtc = (S1DTClass) iter.next();
            var superClassName = dtc.getSendTable().getBaseClass();
            if (superClassName != null) {
                dtc.setSuperClass((S1DTClass) dtClasses.forDtName(superClassName));
            }
        }
        evClassesComplete.raise();
    }

}
