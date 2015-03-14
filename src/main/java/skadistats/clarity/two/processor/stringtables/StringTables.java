package skadistats.clarity.two.processor.stringtables;

import com.dota2.proto.Netmessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.decoder.StringTableDecoder;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.model.StringTableEntry;
import skadistats.clarity.two.framework.annotation.Initializer;
import skadistats.clarity.two.framework.annotation.Provides;
import skadistats.clarity.two.framework.invocation.UsagePoint;
import skadistats.clarity.two.processor.reader.OnMessage;
import skadistats.clarity.two.processor.runner.Context;

import java.util.*;

@Provides(UsesStringTable.class)
public class StringTables {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Set<String> processedLists = new HashSet<>();

    private final List<StringTable> byId = new ArrayList<>();
    private final Map<String, StringTable> byName = new TreeMap<String, StringTable>();

    @Initializer(UsesStringTable.class)
    public void initStringTableUsage(final Context ctx, final UsagePoint<UsesStringTable> usagePoint) {
        processedLists.add(usagePoint.getAnnotation().value());
    }

    @OnMessage(Netmessages.CSVCMsg_CreateStringTable.class)
    public void onCreateStringTable(Context context, Netmessages.CSVCMsg_CreateStringTable message) {
        if (processedLists.contains(message.getName())) {
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
            List<StringTableEntry> changes = StringTableDecoder.decode(table, message.getStringData().toByteArray(), message.getNumEntries());
            for (StringTableEntry t : changes) {
                table.set(t.getIndex(), t.getKey(), t.getValue());
            }
        } else {
            byId.add(null);
        }
    }

    @OnMessage(Netmessages.CSVCMsg_UpdateStringTable.class)
    public void onUpdateStringTable(Context context, Netmessages.CSVCMsg_UpdateStringTable message) {
        StringTable table = byId.get(message.getTableId());
        if (table != null) {
            List<StringTableEntry> changes = StringTableDecoder.decode(table, message.getStringData().toByteArray(), message.getNumChangedEntries());
            for (StringTableEntry t : changes) {
                table.set(t.getIndex(), t.getKey(), t.getValue());
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
