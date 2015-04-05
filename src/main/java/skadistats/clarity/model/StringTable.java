package skadistats.clarity.model;

import com.google.protobuf.ByteString;
import skadistats.clarity.util.TextTable;

import static skadistats.clarity.util.TextTable.Alignment;

public class StringTable {

    private final String name;
    private final int maxEntries;
    private final boolean userDataFixedSize;
    private final int userDataSize;
    private final int userDataSizeBits;
    private final int flags;

    private final String[][] names;
    private final ByteString[][] values;

    private int initialEntryCount;
    private int entryCount;

    public StringTable(String name, int maxEntries, boolean userDataFixedSize, int userDataSize, int userDataSizeBits, int flags) {
        this.name = name;
        this.maxEntries = maxEntries;
        this.userDataFixedSize = userDataFixedSize;
        this.userDataSize = userDataSize;
        this.userDataSizeBits = userDataSizeBits;
        this.flags = flags;
        this.names = new String[maxEntries][2];
        this.values = new ByteString[maxEntries][2];
        this.initialEntryCount = 0;
        this.entryCount = 0;
    }

    public void set(int tbl, int index, String name, ByteString value) {
        if ((tbl & 1) != 0) {
            initialEntryCount = Math.max(initialEntryCount, index + 1);
            this.names[index][0] = name;
            this.values[index][0] = value;
        }
        if ((tbl & 2) != 0) {
            entryCount = Math.max(entryCount, index + 1);
            this.names[index][1] = name;
            this.values[index][1] = value;
        }
    }

    public ByteString getValueByIndex(int index) {
        return values[index][1];
    }

    public String getNameByIndex(int index) {
        return names[index][1];
    }

    public void reset() {
        for (int i = 0; i < maxEntries; i++) {
            names[i][1] = names[i][0];
            values[i][1] = values[i][0];
        }
        entryCount = initialEntryCount;
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

    public int getEntryCount() {
        return entryCount;
    }

    public String toString() {
        TextTable t = new TextTable.Builder()
            .setFrame(TextTable.FRAME_COMPAT)
            .addColumn("Index", Alignment.RIGHT)
            .addColumn("Key", Alignment.RIGHT)
            .addColumn("Value", Alignment.RIGHT)
            .build();
        for (int i = 0; i < entryCount; i++) {
            t.setData(i, 0, i);
            t.setData(i, 1, names[i][1]);
            t.setData(i, 2, values[i][1] != null ? (values[i][1].size() + " bytes") : null);
        }
        return t.toString();
    }

}
