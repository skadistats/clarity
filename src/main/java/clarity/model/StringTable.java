package clarity.model;

import clarity.decoder.Util;

import com.dota2.proto.Netmessages.CSVCMsg_CreateStringTable;
import com.google.protobuf.ByteString;

public class StringTable {

    private final CSVCMsg_CreateStringTable createMessage;
    private final String[] names;
    private final ByteString[] values;

    public StringTable(CSVCMsg_CreateStringTable createMessage) {
        this.createMessage = createMessage;
        this.names = new String[createMessage.getMaxEntries()];
        this.values = new ByteString[createMessage.getMaxEntries()];
    }

    public void set(int index, String name, ByteString value) {
        if (index < names.length) {
            this.names[index] = name;
            this.values[index] = value;
        } else {
            throw new RuntimeException("out of index (" + index + "/" + names.length + ")");
        }
    }

    public ByteString getValueByIndex(int index) {
        return values[index];
    }

    public String getNameByIndex(int index) {
        return names[index];
    }

    public ByteString getValueByName(String key) {
        for (int i = 0; i < names.length; i++) {
            if (key.equals(names[i])) {
                return values[i];
            }
        }
        return null;
    }

    public int getMaxEntries() {
        return createMessage.getMaxEntries();
    }

    public boolean getUserDataFixedSize() {
        return createMessage.getUserDataFixedSize();
    }

    public int getUserDataSize() {
        return createMessage.getUserDataSize();
    }

    public int getUserDataSizeBits() {
        return createMessage.getUserDataSizeBits();
    }

    public String getName() {
        return createMessage.getName();
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        String[] convValues = new String[names.length];
        for (int i = 0; i < names.length; i++) {
            convValues[i] = values[i] == null ? null : Util.convertByteString(values[i], "ISO-8859-1");
        }
        for (int i = 0; i < names.length; i++) {
            if (names[i] == null) {
                continue;
            }
            buf.append(i);
            buf.append(":");
            buf.append(names[i]);
            buf.append(" = ");
            buf.append(convValues[i]);
            buf.append("\r\n");
        }
        return buf.toString();
    }

}
