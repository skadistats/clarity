package skadistats.clarity.processor.stringtables;

import com.google.protobuf.ByteString;
import com.google.protobuf.ZeroCopy;
import org.slf4j.Logger;
import org.xerial.snappy.Snappy;
import skadistats.clarity.event.Insert;
import skadistats.clarity.event.Provides;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.logger.PrintfLoggerFactory;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.util.LZSS;
import skadistats.clarity.wire.shared.common.proto.CommonNetMessages;
import skadistats.clarity.wire.shared.s2.proto.S2NetMessages;

import java.io.IOException;
import java.util.Objects;

import static skadistats.clarity.LogChannel.stringtables;

@Provides(value = {OnStringTableCreated.class, OnStringTableEntry.class, OnStringTableClear.class}, engine = { EngineId.DOTA_S2, EngineId.CSGO_S2})
@StringTableEmitter
public class S2StringTableEmitter extends BaseStringTableEmitter {

    private final Logger log = PrintfLoggerFactory.getLogger(stringtables);

    @Insert
    private Context context;

    private final byte[] tempBuf = new byte[0x4000];

    @OnMessage(S2NetMessages.CSVCMsg_ClearAllStringTables.class)
    public void clearAllStringTables(S2NetMessages.CSVCMsg_ClearAllStringTables msg) {
        numTables = 0;
        evClear.raise();
    }

    @OnMessage(S2NetMessages.CSVCMsg_CreateStringTable.class)
    public void onCreateStringTable(S2NetMessages.CSVCMsg_CreateStringTable message) throws IOException {
        if (isProcessed(message.getName())) {
            var table = new StringTable(
                message.getName(),
                null,
                message.getUserDataFixedSize(),
                message.getUserDataSize(),
                message.getUserDataSizeBits(),
                message.getFlags(),
                message.getUsingVarintBitcounts()
            );

            var data = message.getStringData();
            if (message.getDataCompressed()) {
                byte[] dst;
                if (context.getBuildNumber() != -1 && context.getBuildNumber() <= 962) {
                    dst = LZSS.unpack(data);
                } else {
                    dst = Snappy.uncompress(ZeroCopy.extract(data));
                }
                data = ZeroCopy.wrap(dst);
            }
            decodeEntries(table, data, message.getNumEntries());
            table.markInitialState();
            evCreated.raise(numTables, table);
            raiseUpdateEntryEvents();
        }
        numTables++;
    }

    @OnMessage(CommonNetMessages.CSVCMsg_UpdateStringTable.class)
    public void onUpdateStringTable(CommonNetMessages.CSVCMsg_UpdateStringTable message) throws IOException {
        var table = stringTables.forId(message.getTableId());
        if (table != null) {
            decodeEntries(table, message.getStringData(), message.getNumChangedEntries());
            raiseUpdateEntryEvents();
        }
    }

    private void decodeEntries(StringTable table, ByteString encodedData, int numEntries) throws IOException {
        var stream = BitStream.createBitStream(encodedData);
        var keyHistory = new String[KEY_HISTORY_SIZE];

        var index = -1;
        for (var i = 0; i < numEntries; i++) {
            // read index
            if (stream.readBitFlag()) {
                index++;
            } else {
                index += stream.readVarUInt() + 2;
            }

            // read name
            String name = null;
            if (stream.readBitFlag()) {
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
                var isCompressed = false;
                int bitLength;
                if (table.getUserDataFixedSize()) {
                    bitLength = table.getUserDataSizeBits();
                } else {
                    if ((table.getFlags() & 0x1) != 0) {
                        // this is the case for the instancebaseline for console recorded replays
                        isCompressed = stream.readBitFlag();
                    }
                    bitLength = (table.isVarIntBitCounts() ? stream.readUBitVar() : stream.readUBitInt(17)) * 8;
                }

                var byteLength = (bitLength + 7) / 8;
                byte[] valueBuf;
                if (isCompressed) {
                    stream.readBitsIntoByteArray(tempBuf, bitLength);
                    var byteLengthUncompressed = Snappy.uncompressedLength(tempBuf, 0, byteLength);
                    valueBuf = new byte[byteLengthUncompressed];
                    Snappy.rawUncompress(tempBuf, 0, byteLength, valueBuf, 0);
                } else {
                    valueBuf = new byte[byteLength];
                    stream.readBitsIntoByteArray(valueBuf, bitLength);
                }
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
