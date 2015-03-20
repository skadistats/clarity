package skadistats.clarity.processor.stringtables;

import com.dota2.proto.Netmessages;
import com.google.common.base.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.decoder.StringTableDecoder;
import skadistats.clarity.event.*;
import skadistats.clarity.event.EventListener;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.model.StringTableEntry;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;

import java.util.*;

@Provides({UsesStringTable.class, OnStringTableEntry.class})
public class StringTables {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Set<String> requestedTables = new HashSet<>();

    private final List<StringTable> byId = new ArrayList<>();
    private final Map<String, StringTable> byName = new TreeMap<String, StringTable>();

    @Initializer(UsesStringTable.class)
    public void initStringTableUsage(final Context ctx, final UsagePoint<UsesStringTable> usagePoint) {
        requestedTables.add(usagePoint.getAnnotation().value());
    }

    @Initializer(OnStringTableEntry.class)
    public void initStringTableUsage(final Context ctx, final EventListener<OnStringTableEntry> eventListener) {
        requestedTables.add(eventListener.getAnnotation().value());
        eventListener.setInvocationPredicate(new Predicate<Object[]>() {
            @Override
            public boolean apply(Object[] args) {
                String v = eventListener.getAnnotation().value();
                StringTable t = (StringTable) args[0];
                return v.length() == 0 || v.equals(t.getName());
            }
        });
    }

    @OnMessage(Netmessages.CSVCMsg_CreateStringTable.class)
    public void onCreateStringTable(Context ctx, Netmessages.CSVCMsg_CreateStringTable message) {
        if (requestedTables.contains(message.getName())) {
            StringTable table = new StringTable(
                message.getName(),
                message.getMaxEntries(),
                message.getUserDataFixedSize(),
                message.getUserDataSize(),
                message.getUserDataSizeBits(),
                message.getFlags()
            );
            byId.add(table);
            byName.put(table.getName(), table);
            List<StringTableEntry> changes = StringTableDecoder.decode(table, message.getStringData(), message.getNumEntries());
            applyChanges(ctx, table, changes);
        } else {
            byId.add(null);
        }
    }

    @OnMessage(Netmessages.CSVCMsg_UpdateStringTable.class)
    public void onUpdateStringTable(Context ctx, Netmessages.CSVCMsg_UpdateStringTable message) {
        StringTable table = byId.get(message.getTableId());
        if (table != null) {
            List<StringTableEntry> changes = StringTableDecoder.decode(table, message.getStringData(), message.getNumChangedEntries());
            applyChanges(ctx, table, changes);
        }
    }

    private void applyChanges(Context ctx, StringTable table, List<StringTableEntry> changes) {
        Event<OnStringTableEntry> ev = ctx.createEvent(OnStringTableEntry.class, StringTable.class, StringTableEntry.class, StringTableEntry.class);
        if (!ev.isListenedTo()) {
            ev = null;
        }
        StringTableEntry eOld;
        for (StringTableEntry eNew : changes) {
            eOld = table.getByIndex(eNew.getIndex());
            table.set(eNew.getIndex(), eNew.getKey(), eNew.getValue());
            if (ev != null) {
                ev.raise(table, eOld, eNew);
            }
        }
    }

    public StringTable forName(String name) {
        return byName.get(name);
    }

    public StringTable forId(int id) {
        return byId.get(id);
    }


}
