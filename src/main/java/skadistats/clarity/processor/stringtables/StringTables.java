package skadistats.clarity.processor.stringtables;

import skadistats.clarity.event.Provides;
import skadistats.clarity.model.StringTable;

import java.util.Map;
import java.util.TreeMap;

@Provides({UsesStringTable.class, StringTableEmitter.class})
public class StringTables {

    final Map<Integer, StringTable> byId = new TreeMap<>();
    final Map<String, StringTable> byName = new TreeMap<>();

    @OnStringTableCreated
    public void onStringTableCreated(int numTables, StringTable table) {
        byId.put(numTables, table);
        byName.put(table.getName(), table);
    }

    public StringTable forName(String name) {
        return byName.get(name);
    }

    public StringTable forId(int id) {
        return byId.get(id);
    }

}
