package skadistats.clarity.decoder;

import java.util.LinkedList;
import java.util.List;

import skadistats.clarity.model.StringTable;
import skadistats.clarity.model.StringTableEntry;

import com.google.protobuf.ByteString;

public class StringTableDecoder {

    private static final int MAX_NAME_LENGTH = 0x400;
    private static final int KEY_HISTORY_SIZE = 32;

    public static List<StringTableEntry> decode(StringTable table, byte[] data, int numEntries) {
        BitStream stream = new BitStream(data);
        int bitsPerIndex = Util.calcBitsNeededFor(table.getMaxEntries() - 1);
        LinkedList<String> keyHistory = new LinkedList<String>();
        List<StringTableEntry> result = new LinkedList<StringTableEntry>();
        
        boolean mysteryFlag = stream.readBit();
        int index = -1;
        StringBuffer nameBuf = new StringBuffer();
        while (result.size() < numEntries) {
            // read index
            if (stream.readBit()) {
                index++;
            } else {
                index = stream.readNumericBits(bitsPerIndex);
            }
            // read name
            nameBuf.setLength(0);
            if (stream.readBit()) {
                if (mysteryFlag && stream.readBit()) {
                    throw new RuntimeException("mystery_flag assert failed!");
                }
                if (stream.readBit()) {
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
            if (stream.readBit()) {
                int bitLength = 0;
                if (table.getUserDataFixedSize()) {
                    bitLength = table.getUserDataSizeBits();
                } else {
                    bitLength = stream.readNumericBits(14) * 8;
                }

                value = ByteString.copyFrom(stream.readBits(bitLength));
            }
            result.add(new StringTableEntry(index, nameBuf.toString(), value));
        }
        return result;
    }
}
