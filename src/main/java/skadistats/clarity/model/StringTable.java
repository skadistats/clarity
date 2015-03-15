package skadistats.clarity.model;

import com.google.protobuf.ByteString;
import skadistats.clarity.decoder.Util;

public class StringTable {
    
    private final String name;
    private final int maxEntries;
    private final boolean userDataFixedSize;
    private final int userDataSize;
    private final int userDataSizeBits;
    private final int flags;
    
    private final String[] names;
    private final ByteString[] values;

    public StringTable(String name, int maxEntries, boolean userDataFixedSize, int userDataSize, int userDataSizeBits, int flags) {
        this.name = name;
        this.maxEntries = maxEntries;
        this.userDataFixedSize = userDataFixedSize;
        this.userDataSize = userDataSize;
        this.userDataSizeBits = userDataSizeBits;
        this.flags = flags;
        this.names = new String[maxEntries];
        this.values = new ByteString[maxEntries];
    }

    public void set(int index, String name, ByteString value) {
        if (index < names.length) {
            this.names[index] = name;
            this.values[index] = value;
        } else {
            throw new RuntimeException("out of index (" + index + "/" + names.length + ")");
        }
    }

    public StringTableEntry getByIndex(int index) {
        if (index < names.length) {
            return new StringTableEntry(index, this.names[index], this.values[index]);
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
        return maxEntries;
    }

    public boolean getUserDataFixedSize() {
        return userDataFixedSize;
    }

    public int getUserDataSize() {
        return userDataSize;
    }

    public int getUserDataSizeBits() {
        return userDataSizeBits;
    }

    public String getName() {
        return name;
    }
    
    public int getFlags() {
        return flags;
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
