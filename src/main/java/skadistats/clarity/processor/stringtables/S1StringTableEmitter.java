package skadistats.clarity.processor.stringtables;

import com.google.protobuf.ByteString;
import com.google.protobuf.ZeroCopy;
import skadistats.clarity.decoder.Util;
import skadistats.clarity.decoder.bitstream.BitStream;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.wire.common.proto.NetMessages;
import skadistats.clarity.wire.s1.proto.S1NetMessages;

import java.util.LinkedList;

@Provides(value = {OnStringTableCreated.class, OnStringTableEntry.class}, engine = EngineType.SOURCE1)
@StringTableEmitter
public class S1StringTableEmitter extends BaseStringTableEmitter {

    @OnMessage(S1NetMessages.CSVCMsg_CreateStringTable.class)
    public void onCreateStringTable(Context ctx, S1NetMessages.CSVCMsg_CreateStringTable message) {
        if (isProcessed(message.getName())) {
            StringTable table = new StringTable(
                message.getName(),
                message.getMaxEntries(),
                message.getUserDataFixedSize(),
                message.getUserDataSize(),
                message.getUserDataSizeBits(),
                message.getFlags()
            );
            decodeEntries(ctx, table, 3, message.getStringData(), message.getNumEntries());
            ctx.createEvent(OnStringTableCreated.class, int.class, StringTable.class).raise(numTables, table);
        }
        numTables++;
    }

    @OnMessage(NetMessages.CSVCMsg_UpdateStringTable.class)
    public void onUpdateStringTable(Context ctx, NetMessages.CSVCMsg_UpdateStringTable message) {
        StringTables stringTables = ctx.getProcessor(StringTables.class);
        StringTable table = stringTables.forId(message.getTableId());
        if (table != null) {
            decodeEntries(ctx, table, 2, message.getStringData(), message.getNumChangedEntries());
        }
    }

    private void decodeEntries(Context ctx, StringTable table, int mode, ByteString data, int numEntries) {
        BitStream stream = BitStream.createBitStream(data);
        int bitsPerIndex = Util.calcBitsNeededFor(table.getMaxEntries() - 1);
        LinkedList<String> keyHistory = new LinkedList<>();

        boolean mysteryFlag = stream.readBitFlag();
        int index = -1;
        StringBuilder nameBuf = new StringBuilder();
        while (numEntries-- > 0) {
            // read index
            if (stream.readBitFlag()) {
                index++;
            } else {
                index = stream.readUBitInt(bitsPerIndex);
            }
            // read name
            nameBuf.setLength(0);
            if (stream.readBitFlag()) {
                if (mysteryFlag && stream.readBitFlag()) {
                    throw new RuntimeException("mystery_flag assert failed!");
                }
                if (stream.readBitFlag()) {
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
            if (stream.readBitFlag()) {
                int bitLength;
                if (table.getUserDataFixedSize()) {
                    bitLength = table.getUserDataSizeBits();
                } else {
                    bitLength = stream.readUBitInt(14) * 8;
                }
                byte[] valueBuf = new byte[(bitLength + 7) / 8];
                stream.readBitsIntoByteArray(valueBuf, bitLength);
                value = ZeroCopy.wrap(valueBuf);
            }
            setSingleEntry(ctx, table, mode, index, nameBuf.toString(), value);
        }
    }

}
