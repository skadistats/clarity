package skadistats.clarity.processor.stringtables;

import com.google.protobuf.ByteString;
import skadistats.clarity.event.Event;
import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.Initializer;
import skadistats.clarity.event.Insert;
import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.UsagePoint;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.reader.OnReset;
import skadistats.clarity.processor.reader.ResetPhase;
import skadistats.clarity.util.Predicate;
import skadistats.clarity.wire.common.proto.Demo;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class BaseStringTableEmitter {

    protected static final int MAX_NAME_LENGTH = 0x400;
    protected static final int KEY_HISTORY_SIZE = 32;

    protected int numTables = 0;

    private Set<String> requestedTables = new HashSet<>();
    private Set<String> updateEventTables = new HashSet<>();

    private final Map<String, Demo.CDemoStringTables.table_t> resetStringTables = new TreeMap<>();

    @Insert
    protected StringTables stringTables;

    @InsertEvent
    private Event<OnStringTableEntry> updateEvent;
    @InsertEvent
    protected Event<OnStringTableCreated> evCreated;
    @InsertEvent
    protected Event<OnStringTableClear> evClear;

    @Initializer(UsesStringTable.class)
    public void initStringTableUsage(final UsagePoint<UsesStringTable> usagePoint) {
        requestedTables.add(usagePoint.getAnnotation().value());
    }

    @Initializer(OnStringTableEntry.class)
    public void initStringTableEntryEvent(final EventListener<OnStringTableEntry> eventListener) {
        final String tableName = eventListener.getAnnotation().value();
        requestedTables.add(tableName);
        if ("*".equals(tableName)) {
            updateEventTables = requestedTables;
        } else {
            updateEventTables.add(tableName);
        }
        eventListener.setInvocationPredicate(new Predicate<Object[]>() {
            @Override
            public boolean apply(Object[] args) {
                StringTable t = (StringTable) args[0];
                return "*".equals(tableName) || t.getName().equals(tableName);
            }
        });
    }

    protected boolean isProcessed(String tableName) {
        return requestedTables.contains("*") || requestedTables.contains(tableName);
    }

    protected void setSingleEntry(StringTable table, int mode, int index, Demo.CDemoStringTables.items_tOrBuilder it) {
        String name = null;
        if (it.hasStr()) {
            name = it.getStr();
        } else {
            // With console recorded replays, the replay sometimes has no name entry,
            // and supposedly expects us to use the one that is existing
            // see: https://github.com/skadistats/clarity/issues/147#issuecomment-409619763
            //      and Slack communication with Lukas
            // reuse the old key, and see if that works

            // 10.07.2019: Only do this if the mode is update (not CreateStringTable)
            if (mode == 2 && table.hasIndex(index)) {
                name = table.getNameByIndex(index);
            }
        }
        ByteString value = null;
        if (it.hasData()) {
            value = it.getData();
        } else {
            // 10.07.2019: CSGO console recorded replay sometimes have no data, and want us to use the old data
            // as well
            if (mode == 2 && table.hasIndex(index)) {
                value = table.getValueByIndex(index);
            }
        }
        table.set(mode, index, name, value);
        raise(table, index, name, value);
    }

    protected void raise(StringTable table, int index, String name, ByteString value) {
        updateEvent.raise(table, index, name, value);
    }

    @OnReset
    public void onReset(Demo.CDemoStringTables packet, ResetPhase phase) {
        if (phase == ResetPhase.CLEAR) {
            resetStringTables.clear();
            for (StringTable table : stringTables.byName.values()) {
                table.reset();
            }
        } else if (phase == ResetPhase.ACCUMULATE) {
            for (Demo.CDemoStringTables.table_t tt : packet.getTablesList()) {
                if (!stringTables.byName.containsKey(tt.getTableName())) {
                    continue;
                }
                resetStringTables.put(tt.getTableName(), tt);
            }
        } else if (phase == ResetPhase.APPLY) {
            for (StringTable table : stringTables.byName.values()) {
                Demo.CDemoStringTables.table_t tt = resetStringTables.get(table.getName());
                if (tt != null) {
                    for (int i = 0; i < tt.getItemsCount(); i++) {
                        Demo.CDemoStringTables.items_t it = tt.getItems(i);
                        setSingleEntry(table, 2, i, it);
                    }
                } else {
                    for (int i = 0; i < table.getEntryCount(); i++) {
                        raise(table, i, table.getNameByIndex(i), table.getValueByIndex(i));
                    }
                }
            }
        }
    }

    @OnMessage(Demo.CDemoStringTables.class)
    public void onStringTables(Demo.CDemoStringTables packet) {
        for (Demo.CDemoStringTables.table_t tt : packet.getTablesList()) {
            StringTable table = stringTables.byName.get(tt.getTableName());
            if (table == null) {
                continue;
            }
            for (int i = 0; i < tt.getItemsCount(); i++) {
                Demo.CDemoStringTables.items_t it = tt.getItems(i);
                setSingleEntry(table, 2, i, it);
            }
        }
    }

}
