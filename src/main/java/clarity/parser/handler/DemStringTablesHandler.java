package clarity.parser.handler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.match.Match;
import clarity.model.StringTable;
import clarity.parser.Handler;

import com.dota2.proto.Demo.CDemoStringTables;
import com.dota2.proto.Demo.CDemoStringTables.items_t;
import com.dota2.proto.Demo.CDemoStringTables.table_t;

public class DemStringTablesHandler implements Handler<CDemoStringTables> {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Override
    public void apply(CDemoStringTables message, Match match) {
        log.trace("{}\n{}", message.getClass().getSimpleName(), message);
        for (table_t t : message.getTablesList()) {
            StringTable st = match.getStringTables().forName(t.getTableName());
            List<items_t> l = t.getItemsList();
            for (int i = 0; i < l.size(); i++) {
                st.set(i, l.get(i).getStr(), l.get(i).getData());
            }
        }
    }

}
