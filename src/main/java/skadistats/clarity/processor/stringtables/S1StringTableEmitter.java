package skadistats.clarity.processor.stringtables;

import com.google.protobuf.ByteString;
import com.google.protobuf.ZeroCopy;
import skadistats.clarity.ClarityException;
import skadistats.clarity.io.Util;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.wire.common.proto.NetMessages;
import skadistats.clarity.wire.s1.proto.S1NetMessages;

import java.util.Objects;

@Provides(value = {OnStringTableCreated.class, OnStringTableEntry.class, OnStringTableClear.class}, engine = { EngineId.SOURCE1, EngineId.CSGO })
@StringTableEmitter
public class S1StringTableEmitter extends BaseStringTableEmitter {

    @OnMessage(S1NetMessages.CSVCMsg_CreateStringTable.class)
    public void onCreateStringTable(S1NetMessages.CSVCMsg_CreateStringTable message) {
        if (isProcessed(message.getName())) {
            StringTable table = new StringTable(
                message.getName(),
                message.getMaxEntries(),
                message.getUserDataFixedSize(),
                message.getUserDataSize(),
                message.getUserDataSizeBits(),
                message.getFlags()
            );
            decodeEntries(table, message.getStringData(), message.getNumEntries());
            table.markInitialState();
            evCreated.raise(numTables, table);
        }
        numTables++;
    }

    @OnMessage(NetMessages.CSVCMsg_UpdateStringTable.class)
    public void onUpdateStringTable(NetMessages.CSVCMsg_UpdateStringTable message) {
        StringTable table = stringTables.forId(message.getTableId());
        if (table != null) {
            decodeEntries(table, message.getStringData(), message.getNumChangedEntries());
        }
    }

    private void decodeEntries(StringTable table, ByteString encodedData, int numEntries) {
        BitStream stream = BitStream.createBitStream(encodedData);
        int bitsPerIndex = Util.calcBitsNeededFor(table.getMaxEntries() - 1);
        String[] keyHistory = new String[KEY_HISTORY_SIZE];

        boolean mysteryFlag = stream.readBitFlag();
        int index = -1;
        for (int i = 0; i < numEntries; i++) {
            // read index
            if (stream.readBitFlag()) {
                index++;
            } else {
                index = stream.readUBitInt(bitsPerIndex);
            }

            // read name
            String name = null;
            if (stream.readBitFlag()) {
                if (mysteryFlag && stream.readBitFlag()) {
                    throw new ClarityException("mystery_flag assert failed!");
                }
                if (stream.readBitFlag()) {
                    int base = i > KEY_HISTORY_SIZE ? i : 0;
                    int offs = stream.readUBitInt(5);
                    int len = stream.readUBitInt(5);
                    String str = keyHistory[(base + offs) & KEY_HISTORY_MASK];
                    name = str.substring(0, len) + stream.readString(MAX_NAME_LENGTH);
                } else {
                    name = stream.readString(MAX_NAME_LENGTH);
                }
            }
            // read value
            ByteString data = null;
            if (stream.readBitFlag()) {
                int bitLength;
                if (table.getUserDataFixedSize()) {
                    bitLength = table.getUserDataSizeBits();
                } else {
                    bitLength = stream.readUBitInt(14) * 8;
                }
                byte[] valueBuf = new byte[(bitLength + 7) / 8];
                stream.readBitsIntoByteArray(valueBuf, bitLength);
                data = ZeroCopy.wrap(valueBuf);
            }

            int entryCount = table.getEntryCount();
            if (index < entryCount) {
                // update old entry
                table.setValueForIndex(index, data);
                assert(name == null || Objects.equals(name, table.getNameByIndex(index)));
                name = table.getNameByIndex(index);
            } else if (index == entryCount) {
                // add a new entry
                assert(name != null);
                table.addEntry(name, data);
            } else {
                throw new IllegalStateException("index > entryCount");
            }

            keyHistory[i & KEY_HISTORY_MASK] = name;

            raise(table, index, name, data);
        }
    }

}
