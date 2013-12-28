package clarity.parser.handler;

import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.decoder.SendTableFlattener;
import clarity.match.Match;
import clarity.model.DTClass;
import clarity.model.ReceiveProp;
import clarity.parser.Handler;

import com.dota2.proto.Demo.CDemoSyncTick;

public class DemSyncTickHandler implements Handler<CDemoSyncTick> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void apply(CDemoSyncTick message, Match match) {
        log.trace("{}\n{}", message.getClass().getSimpleName(), message);
        
        // last packet of the prologue: compile receive tables!

        for (Iterator<DTClass> i = match.getDtClasses().iterator(); i.hasNext();) {
            DTClass dtc = i.next();
            if (!dtc.getSendTable().getMessage().getNeedsDecoder()) {
                continue;
            }
            List<ReceiveProp> rps = new SendTableFlattener(match.getDtClasses(), dtc.getSendTable()).flatten();
            dtc.setReceiveProps(rps);
        }
    }

}
