package skadistats.clarity.processor.stringtables;

import com.google.protobuf.ByteString;
import com.google.protobuf.ZeroCopy;
import skadistats.clarity.ClarityException;
import skadistats.clarity.event.Provides;
import skadistats.clarity.io.Util;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.wire.shared.common.proto.CommonNetMessages;
import skadistats.clarity.wire.shared.s1.proto.S1NetMessages;

import java.util.Objects;

@Provides(value = {OnStringTableCreated.class, OnStringTableEntry.class, OnStringTableClear.class}, engine = { EngineId.DOTA_S1, EngineId.CSGO_S1})
@StringTableEmitter
public class S1StringTableEmitter extends BaseStringTableEmitter {

    @OnMessage(S1NetMessages.CSVCMsg_CreateStringTable.class)
    public void onCreateStringTable(S1NetMessages.CSVCMsg_CreateStringTable message) {
        if (isProcessed(message.getName())) {
            var table = new StringTable(
                message.getName(),
                message.getMaxEntries(),
                message.getUserDataFixedSize(),
                message.getUserDataSize(),
                message.getUserDataSizeBits(),
                message.getFlags(),
   false
            );
            decodeEntries(table, message.getStringData(), message.getNumEntries());
            table.markInitialState();
            evCreated.raise(numTables, table);
            raiseUpdateEntryEvents();
        }
        numTables++;
    }

    @OnMessage(CommonNetMessages.CSVCMsg_UpdateStringTable.class)
    public void onUpdateStringTable(CommonNetMessages.CSVCMsg_UpdateStringTable message) {
        var table = stringTables.forId(message.getTableId());
        if (table != null) {
            decodeEntries(table, message.getStringData(), message.getNumChangedEntries());
            raiseUpdateEntryEvents();
        }
    }

    private void decodeEntries(StringTable table, ByteString encodedData, int numEntries) {
        var stream = BitStream.createBitStream(encodedData);
        var bitsPerIndex = Util.calcBitsNeededFor(table.getMaxEntries() - 1);
        var keyHistory = new String[KEY_HISTORY_SIZE];

        var mysteryFlag = stream.readBitFlag();
        var index = -1;
        for (var i = 0; i < numEntries; i++) {
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
                    var base = i > KEY_HISTORY_SIZE ? i : 0;
                    var offs = stream.readUBitInt(5);
                    var len = stream.readUBitInt(5);
                    var str = keyHistory[(base + offs) & KEY_HISTORY_MASK];
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
                var valueBuf = new byte[(bitLength + 7) / 8];
                stream.readBitsIntoByteArray(valueBuf, bitLength);
                data = ZeroCopy.wrap(valueBuf);
            }

            var entryCount = table.getEntryCount();
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

            queueUpdateEntryEvent(table, index, name, data);
        }
    }

}
