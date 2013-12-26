package clarity.parser.handler;

import java.util.List;
import java.util.Map;

import clarity.match.Match;
import clarity.model.ReceiveProp;
import clarity.model.SendTable;
import clarity.model.SendTableFlattener;

import com.dota2.proto.Demo.CDemoSyncTick;

public class DemSyncTickHandler implements Handler<CDemoSyncTick> {

	@Override
	public void apply(CDemoSyncTick message, Match match) {
		// last packet of the prologue: compile receive tables!
		
		for (Map.Entry<String, SendTable> e : match.getSendTableByDT().entrySet()) {
			String dt = e.getKey();
			SendTable sendTable = e.getValue();
			//System.out.println();
			//System.out.println("------------------------------------- processing send table " + sendTable.getMessage().getNetTableName());
			if (!sendTable.getMessage().getNeedsDecoder()) {
				continue;
			}
			Integer cls = match.getClassByDT().get(dt);
			List<ReceiveProp> rps = new SendTableFlattener(match.getSendTableByDT(), sendTable).flatten();
			match.getReceivePropsByClass().put(cls, rps);
		}
	}
	
}
