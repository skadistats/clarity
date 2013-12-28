package clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.match.Match;
import clarity.model.DTClass;
import clarity.model.SendTable;
import clarity.parser.Handler;

import com.dota2.proto.Netmessages.CSVCMsg_SendTable;

public class SvcSendTableHandler implements Handler<CSVCMsg_SendTable> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(CSVCMsg_SendTable message, Match match) {
        log.trace("{}\n{}", message.getClass().getSimpleName(), message);
        SendTable st = new SendTable(message);
        match.getDtClasses().add(new DTClass(st.getMessage().getNetTableName(), st));
    }

}
