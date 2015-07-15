package skadistats.clarity.parser.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.match.Match;
import skadistats.clarity.parser.Handler;
import skadistats.clarity.parser.HandlerHelper;
import skadistats.clarity.parser.RegisterHandler;
import skadistats.clarity.wire.s1.proto.Demo.CDemoStringTables;
import skadistats.clarity.wire.s1.proto.Demo.CDemoStringTables.items_t;
import skadistats.clarity.wire.s1.proto.Demo.CDemoStringTables.table_t;

import java.util.List;

@RegisterHandler(CDemoStringTables.class)
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
