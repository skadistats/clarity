package clarity.parser.handler;

import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.match.Match;
import clarity.model.DTClass;
import clarity.model.PropType;
import clarity.model.SendProp;
import clarity.model.SendTable;
import clarity.parser.Handler;
import clarity.parser.HandlerHelper;
import clarity.parser.RegisterHandler;

import com.dota2.proto.Netmessages.CSVCMsg_SendTable;
import com.dota2.proto.Netmessages.CSVCMsg_SendTable.sendprop_t;

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
