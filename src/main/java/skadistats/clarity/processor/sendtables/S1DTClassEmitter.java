package skadistats.clarity.processor.sendtables;

import skadistats.clarity.decoder.s1.S1DTClass;
import skadistats.clarity.decoder.s1.SendProp;
import skadistats.clarity.decoder.s1.SendTable;
import skadistats.clarity.decoder.s1.SendTableFlattener;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.model.s1.PropType;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.wire.common.proto.Demo;
import skadistats.clarity.wire.s1.proto.S1NetMessages;

import java.util.Iterator;
import java.util.LinkedList;

@Provides(value = OnDTClass.class, engine = EngineType.SOURCE1)
public class S1DTClassEmitter {

    @OnMessage(S1NetMessages.CSVCMsg_SendTable.class)
    public void onSendTable(Context ctx, S1NetMessages.CSVCMsg_SendTable message) {

        LinkedList<SendProp> props = new LinkedList<SendProp>();
        SendTable st = new SendTable(
            message.getNetTableName(),
            props
        );

        for (S1NetMessages.CSVCMsg_SendTable.sendprop_t sp : message.getPropsList()) {
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
        ctx.createEvent(OnDTClass.class, DTClass.class).raise(dtClass);
    }

    @OnMessage(Demo.CDemoClassInfo.class)
    public void onClassInfo(Context ctx, Demo.CDemoClassInfo message) {
        DTClasses dtClasses = ctx.getProcessor(DTClasses.class);
        for (Demo.CDemoClassInfo.class_t ct : message.getClassesList()) {
            DTClass dt = dtClasses.forDtName(ct.getTableName());
            dt.setClassId(ct.getClassId());
            dtClasses.byClassId.put(ct.getClassId(), dt);
        }
    }

    @OnMessage(Demo.CDemoSyncTick.class)
    public void onSyncTick(Context ctx, Demo.CDemoSyncTick message) {
        DTClasses dtClasses = ctx.getProcessor(DTClasses.class);
        Iterator<DTClass> iter = dtClasses.iterator();
        while (iter.hasNext()) {
            S1DTClass dtc = (S1DTClass) iter.next();
            SendTableFlattener.Result flattened = new SendTableFlattener(dtClasses, dtc.getSendTable()).flatten();
            dtc.setReceiveProps(flattened.receiveProps);
            dtc.setIndexMapping(flattened.indexMapping);
        }
        iter = dtClasses.iterator();
        while (iter.hasNext()) {
            S1DTClass dtc = (S1DTClass) iter.next();
            String superClassName = dtc.getSendTable().getBaseClass();
            if (superClassName != null) {
                dtc.setSuperClass((S1DTClass) dtClasses.forDtName(superClassName));
            }
        }
    }

}
