package skadistats.clarity.processor.stringtables;

import com.google.protobuf.ByteString;
import com.google.protobuf.ZeroCopy;
import org.xerial.snappy.Snappy;
import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.wire.s2.proto.S2NetMessages;

import java.io.IOException;
import java.util.LinkedList;

@Provides(value = {OnStringTableCreated.class, OnStringTableEntry.class}, engine = EngineType.SOURCE2)
@StringTableEmitter
public class S2StringTableEmitter extends BaseStringTableEmitter {

    @OnMessage(S2NetMessages.CSVCMsg_CreateStringTable.class)
    public void onCreateStringTable(Context ctx, S2NetMessages.CSVCMsg_CreateStringTable message) throws IOException {
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
                data = ZeroCopy.wrap(Snappy.uncompress(ZeroCopy.extract(data)));
                //byte[] unp = new byte[message.getUncompressedSize()];
                //LZSS.unpack(ZeroCopy.extract(data), unp);
                //data = ZeroCopy.wrap(unp);
            }
            decodeEntries(ctx, table, 3, data, message.getNumEntries());
            ctx.createEvent(OnStringTableCreated.class, int.class, StringTable.class).raise(numTables, table);
        }
        numTables++;
    }

    @OnMessage(S2NetMessages.CSVCMsg_UpdateStringTable.class)
    public void onUpdateStringTable(Context ctx, S2NetMessages.CSVCMsg_UpdateStringTable message) {
        StringTables stringTables = ctx.getProcessor(StringTables.class);
        StringTable table = stringTables.forId(message.getTableId());
        if (table != null) {
            decodeEntries(ctx, table, 2, message.getStringData(), message.getNumChangedEntries());
        }
    }

    private void decodeEntries(Context ctx, StringTable table, int mode, ByteString data, int numEntries) {
        BitStream stream = new BitStream(data);
        LinkedList<String> keyHistory = new LinkedList<>();

        int index = -1;
        StringBuilder nameBuf = new StringBuilder();
        while (numEntries-- > 0) {
            // read index
            if (stream.readUBitInt(1) == 1) {
                index++;
            } else {
                index = stream.readVarUInt() + 1;
            }
            // read name
            nameBuf.setLength(0);
            if (stream.readUBitInt(1) == 1) {
                if (stream.readUBitInt(1) == 1) {
                    int basis = stream.readUBitInt(5);
                    int length = stream.readUBitInt(5);
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
            if (stream.readUBitInt(1) == 1) {
                int bitLength;
                if (table.getUserDataFixedSize()) {
                    bitLength = table.getUserDataSizeBits();
                } else {
                    bitLength = stream.readUBitInt(14) * 8;
                    int mysteryBits = stream.readUBitInt(3);
                    if (mysteryBits != 0) {
                        log.info("mystery bits are NOT zero, but " + mysteryBits);
                    }
                }
                value = ByteString.copyFrom(stream.readBitsAsByteArray(bitLength));
            }
            setSingleEntry(ctx, table, mode, index, nameBuf.toString(), value);
        }
    }

}
