package skadistats.clarity.processor.stringtables;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.event.Event;
import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.Initializer;
import skadistats.clarity.event.UsagePoint;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.reader.OnReset;
import skadistats.clarity.processor.reader.ResetPhase;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.util.Predicate;
import skadistats.clarity.wire.common.proto.Demo;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class BaseStringTableEmitter {

    protected static final int MAX_NAME_LENGTH = 0x400;
    protected static final int KEY_HISTORY_SIZE = 32;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected int numTables = 0;

    private Set<String> requestedTables = new HashSet<>();
    private Set<String> updateEventTables = new HashSet<>();

    private final Map<String, Demo.CDemoStringTables.table_t> resetStringTables = new TreeMap<>();

    private Event<OnStringTableEntry> updateEvent = null;

    @Initializer(UsesStringTable.class)
    public void initStringTableUsage(final Context ctx, final UsagePoint<UsesStringTable> usagePoint) {
        requestedTables.add(usagePoint.getAnnotation().value());
    }

    @Initializer(OnStringTableEntry.class)
    public void initStringTableEntryEvent(final Context ctx, final EventListener<OnStringTableEntry> eventListener) {
        final String tableName = eventListener.getAnnotation().value();
        requestedTables.add(tableName);
        if ("*".equals(tableName)) {
            updateEventTables = requestedTables;
        } else {
            updateEventTables.add(tableName);
        }
        updateEvent = ctx.createEvent(OnStringTableEntry.class, StringTable.class, int.class, String.class, ByteString.class);
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

    protected void setSingleEntry(Context ctx, StringTable table, int mode, int index, String key, ByteString value) {
        table.set(mode, index, key, value);
        raise(table, index, key, value);
    }

    protected void raise(StringTable table, int index, String key, ByteString value) {
        if (updateEvent != null) {
            updateEvent.raise(table, index, key, value);
        }
    }

    @OnReset
    public void onReset(Context ctx, Demo.CDemoStringTables packet, ResetPhase phase) {
        StringTables stringTables = ctx.getProcessor(StringTables.class);
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
                        setSingleEntry(ctx, table, 2, i, it.getStr(), it.getData());
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
    public void onStringTables(Context ctx, Demo.CDemoStringTables packet) {
        StringTables stringTables = ctx.getProcessor(StringTables.class);
        for (Demo.CDemoStringTables.table_t tt : packet.getTablesList()) {
            StringTable table = stringTables.byName.get(tt.getTableName());
            if (table == null) {
                continue;
            }
            for (int i = 0; i < tt.getItemsCount(); i++) {
                Demo.CDemoStringTables.items_t it = tt.getItems(i);
                setSingleEntry(ctx, table, 2, i, it.getStr(), it.getData());
            }
        }

    }


}
