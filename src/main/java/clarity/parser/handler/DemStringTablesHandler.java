package clarity.parser.handler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.decoder.StringTableApplier;
import clarity.match.Match;
import clarity.parser.Handler;
import clarity.parser.HandlerHelper;

import com.dota2.proto.Demo.CDemoStringTables;
import com.dota2.proto.Demo.CDemoStringTables.items_t;
import com.dota2.proto.Demo.CDemoStringTables.table_t;

public class DemStringTablesHandler implements Handler<CDemoStringTables> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(int peekTick, CDemoStringTables message, Match match) {
        HandlerHelper.traceMessage(log, peekTick, message);
        for (table_t t : message.getTablesList()) {
            List<items_t> l = t.getItemsList();
            for (int i = 0; i < l.size(); i++) {
                StringTableApplier.forName(t.getTableName()).apply(match, t.getTableName(), i, l.get(i).getStr(), l.get(i).getData());
            }
        }
    }

}
