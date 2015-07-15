package skadistats.clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.match.Match;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.PropType;
import skadistats.clarity.model.SendProp;
import skadistats.clarity.model.SendTable;
import skadistats.clarity.parser.Handler;
import skadistats.clarity.parser.HandlerHelper;
import skadistats.clarity.parser.RegisterHandler;
import skadistats.clarity.wire.s1.proto.Netmessages.CSVCMsg_SendTable;
import skadistats.clarity.wire.s1.proto.Netmessages.CSVCMsg_SendTable.sendprop_t;

import java.util.LinkedList;

@RegisterHandler(CSVCMsg_SendTable.class)
public class SvcSendTableHandler implements Handler<CSVCMsg_SendTable> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CSVCMsg_SendTable message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);

        LinkedList<SendProp> props = new LinkedList<SendProp>();
        SendTable st = new SendTable(
            message.getNetTableName(),
            message.getNeedsDecoder(),
            props
        );

        for (sendprop_t sp : message.getPropsList()) {
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
        
        match.getDtClasses().add(new DTClass(message.getNetTableName(), st));
    }

}
