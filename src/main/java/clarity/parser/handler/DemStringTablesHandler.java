package clarity.parser.handler;

import java.util.List;

import clarity.match.Match;
import clarity.model.StringTable;

import com.dota2.proto.Demo.CDemoStringTables;
import com.dota2.proto.Demo.CDemoStringTables.items_t;
import com.dota2.proto.Demo.CDemoStringTables.table_t;

public class DemStringTablesHandler implements Handler<CDemoStringTables> {

    @Override
    public void apply(CDemoStringTables message, Match match) {
        for (table_t t : message.getTablesList()) {
            StringTable st = match.getStringTables().forName(t.getTableName());
            List<items_t> l = t.getItemsList();
            for (int i = 0; i < l.size(); i++) {
                st.set(i, l.get(i).getStr(), l.get(i).getData());
            }
        }
    }

}
