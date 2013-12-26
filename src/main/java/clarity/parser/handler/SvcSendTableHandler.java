package clarity.parser.handler;

import clarity.match.Match;
import clarity.model.DTClass;
import clarity.model.SendTable;
import clarity.parser.Handler;

import com.dota2.proto.Netmessages.CSVCMsg_SendTable;

public class SvcSendTableHandler implements Handler<CSVCMsg_SendTable> {

    @Override
    public void apply(CSVCMsg_SendTable message, Match match) {
        SendTable st = new SendTable(message);
        match.getDtClasses().add(new DTClass(st.getMessage().getNetTableName(), st));
    }

}
