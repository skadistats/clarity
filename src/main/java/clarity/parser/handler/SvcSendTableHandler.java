package clarity.parser.handler;

import clarity.match.Match;
import clarity.model.SendTable;

import com.dota2.proto.Netmessages.CSVCMsg_SendTable;

public class SvcSendTableHandler implements Handler<CSVCMsg_SendTable> {

	@Override
	public void apply(CSVCMsg_SendTable message, Match match) {
		SendTable st = new SendTable(message);
		match.getSendTableByDT().put(st.getMessage().getNetTableName(), st);
	}

}
