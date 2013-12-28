package clarity.decoder;

import java.util.LinkedList;

import clarity.model.StringTable;

import com.google.protobuf.ByteString;

public class StringTableDecoder {

    private static final int MAX_NAME_LENGTH = 0x400;
    private static final int KEY_HISTORY_SIZE = 32;

    public static void decode(StringTable table, byte[] data, int numEntries) {
        BitStream stream = new BitStream(data);
        int bitsPerIndex = Util.calcBitsNeededFor(table.getMaxEntries() - 1);
        LinkedList<String> keyHistory = new LinkedList<String>();

        boolean mysteryFlag = stream.readBit();
        int index = -1;
        int c = 0;
        StringBuffer nameBuf = new StringBuffer();
        while (c < numEntries) {
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
            if (table.getName().equals("ActiveModifiers")) {
                System.out.println(String.format("modifier changed at %s, value has len %s", index, value == null ? 0 : value.size()));
            }
            table.set(index, nameBuf.toString(), value);
            c++;
        }
    }
}
