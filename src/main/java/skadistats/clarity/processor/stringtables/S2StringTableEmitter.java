package skadistats.clarity.processor.stringtables;

import com.google.protobuf.ByteString;
import com.google.protobuf.ZeroCopy;
import org.slf4j.Logger;
import org.xerial.snappy.Snappy;
import skadistats.clarity.decoder.bitstream.BitStream;
import skadistats.clarity.event.Insert;
import skadistats.clarity.event.Provides;
import skadistats.clarity.logger.PrintfLoggerFactory;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.util.LZSS;
import skadistats.clarity.wire.common.proto.NetMessages;
import skadistats.clarity.wire.s2.proto.S2NetMessages;

import java.io.IOException;
import java.util.LinkedList;

import static skadistats.clarity.LogChannel.stringtables;

@Provides(value = {OnStringTableCreated.class, OnStringTableEntry.class, OnStringTableClear.class}, engine = EngineId.SOURCE2)
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
            StringTable table = new StringTable(
                message.getName(),
                100,
                message.getUserDataFixedSize(),
                message.getUserDataSize(),
                message.getUserDataSizeBits(),
                message.getFlags()
            );

            ByteString data = message.getStringData();
            if (message.getDataCompressed()) {
                byte[] dst;
                if (context.getBuildNumber() != -1 && context.getBuildNumber() <= 962) {
                    dst = LZSS.unpack(data);
                } else {
                    dst = Snappy.uncompress(ZeroCopy.extract(data));
                }
                data = ZeroCopy.wrap(dst);
            }
            decodeEntries(table, 3, data, message.getNumEntries());
            evCreated.raise(numTables, table);
        }
        numTables++;
    }

    @OnMessage(NetMessages.CSVCMsg_UpdateStringTable.class)
    public void onUpdateStringTable(NetMessages.CSVCMsg_UpdateStringTable message) throws IOException {
        StringTable table = stringTables.forId(message.getTableId());
        if (table != null) {
            decodeEntries(table, 2, message.getStringData(), message.getNumChangedEntries());
        }
    }

    private void decodeEntries(StringTable table, int mode, ByteString data, int numEntries) throws IOException {
        BitStream stream = BitStream.createBitStream(data);
        LinkedList<String> keyHistory = new LinkedList<>();

        int index = -1;
        StringBuilder nameBuf = new StringBuilder();
        while (numEntries-- > 0) {
            // read index
            if (stream.readBitFlag()) {
                index++;
            } else {
                index = stream.readVarUInt() + 1;
            }
            // read name
            nameBuf.setLength(0);
            if (stream.readBitFlag()) {
                if (stream.readBitFlag()) {
                    int basis = stream.readUBitInt(5);
                    int length = stream.readUBitInt(5);
                    if (basis >= keyHistory.size()) {
                        for (int k = 0; k < length; k++) {
                            nameBuf.append('_');
                        }
                        nameBuf.append(stream.readString(MAX_NAME_LENGTH));
                        log.warn("Working around keyHistory underflow. Key '%s' in table '%s' is incomplete.", nameBuf.toString(), table.getName());
                    } else {
                        nameBuf.append(keyHistory.get(basis).substring(0, length));
                        nameBuf.append(stream.readString(MAX_NAME_LENGTH - length));
                    }
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
            if (stream.readBitFlag()) {
                boolean isCompressed = false;
                int bitLength;
                if (table.getUserDataFixedSize()) {
                    bitLength = table.getUserDataSizeBits();
                } else {
                    if ((table.getFlags() & 0x1) != 0) {
                        // this is the case for the instancebaseline for console recorded replays
                        isCompressed = stream.readBitFlag();
                    }
                    bitLength = stream.readUBitInt(17) * 8;
                }

                int byteLength = (bitLength + 7) / 8;
                byte[] valueBuf;
                if (isCompressed) {
                    stream.readBitsIntoByteArray(tempBuf, bitLength);
                    int byteLengthUncompressed = Snappy.uncompressedLength(tempBuf, 0, byteLength);
                    valueBuf = new byte[byteLengthUncompressed];
                    Snappy.rawUncompress(tempBuf, 0, byteLength, valueBuf, 0);
                } else {
                    valueBuf = new byte[byteLength];
                    stream.readBitsIntoByteArray(valueBuf, bitLength);
                }

                value = ZeroCopy.wrap(valueBuf);
            }
            setSingleEntry(table, mode, index, nameBuf.toString(), value);
        }
    }

}
