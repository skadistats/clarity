package clarity.parser.handler;

import java.util.Iterator;
import java.util.List;

import clarity.match.Match;
import clarity.model.DTClass;
import clarity.model.ReceiveProp;
import clarity.model.SendTableFlattener;

import com.dota2.proto.Demo.CDemoSyncTick;

public class DemSyncTickHandler implements Handler<CDemoSyncTick> {

    @Override
    public void apply(CDemoSyncTick message, Match match) {
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
