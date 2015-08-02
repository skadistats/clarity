package skadistats.clarity.processor.stringtables;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.Util;
import skadistats.clarity.event.*;
import skadistats.clarity.event.EventListener;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.reader.OnReset;
import skadistats.clarity.processor.reader.ResetPhase;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.util.Predicate;
import skadistats.clarity.wire.common.proto.Demo;
import skadistats.clarity.wire.s1.proto.Netmessages;

import java.util.*;

@Provides({UsesStringTable.class, OnStringTableEntry.class})
public class StringTables {

    private static final int MAX_NAME_LENGTH = 0x400;
    private static final int KEY_HISTORY_SIZE = 32;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private int numTables = 0;
    private final Map<Integer, StringTable> byId = new TreeMap<>();
    private final Map<String, StringTable> byName = new TreeMap<>();

    private final Map<String, Demo.CDemoStringTables.table_t> resetStringTables = new TreeMap<>();

    private Set<String> requestedTables = new HashSet<>();
    private Set<String> updateEventTables = new HashSet<>();
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

    @OnReset
    public void onReset(Context ctx, Demo.CDemoFullPacket packet, ResetPhase phase) {
        if (phase == ResetPhase.CLEAR) {
            resetStringTables.clear();
            for (StringTable table : byName.values()) {
                table.reset();
            }
        } else if (phase == ResetPhase.STRINGTABLE_ACCUMULATION) {
            for (Demo.CDemoStringTables.table_t tt : packet.getStringTable().getTablesList()) {
                if (!byName.containsKey(tt.getTableName())) {
                    continue;
                }
                resetStringTables.put(tt.getTableName(), tt);
            }
        } else if (phase == ResetPhase.STRINGTABLE_APPLY) {
            for (StringTable table : byName.values()) {
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

    @OnMessage(Netmessages.CSVCMsg_CreateStringTable.class)
    public void onCreateStringTable(Context ctx, Netmessages.CSVCMsg_CreateStringTable message) {
        if (requestedTables.contains("*") || requestedTables.contains(message.getName())) {
            StringTable table = new StringTable(
                message.getName(),
                message.getMaxEntries(),
                message.getUserDataFixedSize(),
                message.getUserDataSize(),
                message.getUserDataSizeBits(),
                message.getFlags()
            );
            byId.put(numTables, table);
            byName.put(table.getName(), table);
            decode(ctx, table, 3, message.getStringData(), message.getNumEntries());
        }
        numTables++;
    }

    @OnMessage(Netmessages.CSVCMsg_UpdateStringTable.class)
    public void onUpdateStringTable(Context ctx, Netmessages.CSVCMsg_UpdateStringTable message) {
        StringTable table = byId.get(message.getTableId());
        if (table != null) {
            decode(ctx, table, 2, message.getStringData(), message.getNumChangedEntries());
        }
    }

    private void decode(Context ctx, StringTable table, int mode, ByteString data, int numEntries) {
        BitStream stream = new BitStream(data);
        int bitsPerIndex = Util.calcBitsNeededFor(table.getMaxEntries() - 1);
        LinkedList<String> keyHistory = new LinkedList<>();

        boolean mysteryFlag = stream.readNumericBits(1) == 1;
        int index = -1;
        StringBuffer nameBuf = new StringBuffer();
        while (numEntries-- > 0) {
            // read index
            if (stream.readNumericBits(1) == 1) {
                index++;
            } else {
                index = stream.readNumericBits(bitsPerIndex);
            }
            // read name
            nameBuf.setLength(0);
            if (stream.readNumericBits(1) == 1) {
                if (mysteryFlag && stream.readNumericBits(1) == 1) {
                    throw new RuntimeException("mystery_flag assert failed!");
                }
                if (stream.readNumericBits(1) == 1) {
                    int basis = stream.readNumericBits(5);
                    int length = stream.readNumericBits(5);
                    nameBuf.append(keyHistory.get(basis).substring(0, length));
                    nameBuf.append(stream.readString(MAX_NAME_LENGTH - length));
                } else {
                    nameBuf.append(stream.readString(MAX_NAME_LENGTH));
                }
                if (keyHistory.size() == KEY_HISTORY_SIZE) {
                    keyHistory.remove(0);
                }
                keyHistory.add(nameBuf.toString());
            }
            // read value
            ByteString value = null;
            if (stream.readNumericBits(1) == 1) {
                int bitLength = 0;
                if (table.getUserDataFixedSize()) {
                    bitLength = table.getUserDataSizeBits();
                } else {
                    bitLength = stream.readNumericBits(14) * 8;
                }

                value = ByteString.copyFrom(stream.readBits(bitLength));
            }
            setSingleEntry(ctx, table, mode, index, nameBuf.toString(), value);
        }
    }

    private void setSingleEntry(Context ctx, StringTable table, int mode, int index, String key, ByteString value) {
        table.set(mode, index, key, value);
        raise(table, index, key, value);
    }

    private void raise(StringTable table, int index, String key, ByteString value) {
        if (updateEvent != null) {
            updateEvent.raise(table, index, key, value);
        }
    }

    public StringTable forName(String name) {
        return byName.get(name);
    }

    public StringTable forId(int id) {
        return byId.get(id);
    }

}
