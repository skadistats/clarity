package skadistats.clarity.processor.stringtables;

import skadistats.clarity.ClarityException;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.StringTable;

import java.util.Map;
import java.util.TreeMap;

@Provides({UsesStringTable.class, StringTableEmitter.class})
public class StringTables {

    final Map<Integer, StringTable> byId = new TreeMap<>();
    final Map<String, StringTable> byName = new TreeMap<>();

    @OnStringTableCreated
    public void onStringTableCreated(int tableNum, StringTable table) {
        if (byId.containsKey(tableNum) || byName.containsKey(table.getName())) {
            throw new ClarityException("String table %d (%s) already exists!", tableNum, table.getName());
        }
        byId.put(tableNum, table);
        byName.put(table.getName(), table);
    }

    @OnStringTableClear
    public void clearAllStringTables() {
        byId.clear();
        byName.clear();
    }

    public StringTable forName(String name) {
        return byName.get(name);
    }

    public StringTable forId(int id) {
        return byId.get(id);
    }

}
