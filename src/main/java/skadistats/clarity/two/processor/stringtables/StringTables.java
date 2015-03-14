package skadistats.clarity.two.processor.stringtables;

import com.dota2.proto.Netmessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.decoder.StringTableDecoder;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.model.StringTableEntry;
import skadistats.clarity.two.processor.reader.OnMessage;
import skadistats.clarity.two.runner.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class StringTables {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<StringTable> byId = new ArrayList<StringTable>();
    private final Map<String, StringTable> byName = new TreeMap<String, StringTable>();

    @OnMessage(Netmessages.CSVCMsg_CreateStringTable.class)
    public void onCreateStringTable(Context context, Netmessages.CSVCMsg_CreateStringTable message) {
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
    }

    @OnMessage(Netmessages.CSVCMsg_UpdateStringTable.class)
    public void onUpdateStringTable(Context context, Netmessages.CSVCMsg_UpdateStringTable message) {
        StringTable table = byId.get(message.getTableId());
        List<StringTableEntry> changes = StringTableDecoder.decode(table, message.getStringData().toByteArray(), message.getNumChangedEntries());
        for (StringTableEntry t : changes) {
            table.set(t.getIndex(), t.getKey(), t.getValue());
        }
    }

}
