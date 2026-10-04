package skadistats.clarity.processor.stringtables;

import skadistats.clarity.ClarityException;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.StringTable;

import java.util.Map;
import java.util.TreeMap;

/**
 * Registry of the string tables created during the replay, available by table id and by table name.
 * <p>
 * Active when a processor declares {@link UsesStringTable}. All tables are dropped when
 * {@link OnStringTableClear} is raised. Only tables that were
 * requested via {@link UsesStringTable} or {@link OnStringTableEntry} are registered.
 */
@Provides({UsesStringTable.class, StringTableEmitter.class})
public class StringTables {

    final Map<Integer, StringTable> byId = new TreeMap<>();
    final Map<String, StringTable> byName = new TreeMap<>();

    /** Event handler bound by the runtime; not for direct use. */
    @OnStringTableCreated
    public void onStringTableCreated(int tableNum, StringTable table) {
        if (byId.containsKey(tableNum) || byName.containsKey(table.getName())) {
            throw new ClarityException("String table %d (%s) already exists!", tableNum, table.getName());
        }
        byId.put(tableNum, table);
        byName.put(table.getName(), table);
    }

    /** Event handler bound by the runtime; not for direct use. */
    @OnStringTableClear
    public void clearAllStringTables() {
        byId.clear();
        byName.clear();
    }

    /**
     * @param name the table name, e.g. {@code "userinfo"}
     * @return the table with that name, or {@code null} if no such table exists (yet) or it was not requested
     *         via {@link UsesStringTable}
     */
    public StringTable forName(String name) {
        return byName.get(name);
    }

    /**
     * @param id the table id, as assigned by the order of creation in the replay
     * @return the table with that id, or {@code null} if no such table exists (yet) or it was not requested
     *         via {@link UsesStringTable}
     */
    public StringTable forId(int id) {
        return byId.get(id);
    }

}
