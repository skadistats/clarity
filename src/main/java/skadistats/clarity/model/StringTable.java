package skadistats.clarity.model;

import com.google.protobuf.ByteString;
import skadistats.clarity.util.TextTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static skadistats.clarity.util.TextTable.Alignment;

public class StringTable {

    private final String name;
    private final Integer maxEntries;
    private final boolean userDataFixedSize;
    private final int userDataSize;
    private final int userDataSizeBits;
    private final int flags;
    private final boolean varIntBitCounts;

    private final List<Entry> entries;
    private List<Entry> initialEntries;

    public StringTable(String name, Integer maxEntries, boolean userDataFixedSize, int userDataSize, int userDataSizeBits, int flags, boolean varIntBitCounts) {
        this.name = name;
        this.maxEntries = maxEntries;
        this.userDataFixedSize = userDataFixedSize;
        this.userDataSize = userDataSize;
        this.userDataSizeBits = userDataSizeBits;
        this.flags = flags;
        this.varIntBitCounts = varIntBitCounts;
        this.entries = new ArrayList<>();
        this.initialEntries = Collections.emptyList();
    }

    public void setValueForIndex(int index, ByteString value) {
        entries.get(index).value = value;
    }

    public void addEntry(String name, ByteString value) {
        entries.add(new Entry(name, value));
    }

    public boolean hasIndex(int index) {
        return index >= 0 && index < entries.size();
    }

    public ByteString getValueByIndex(int index) {
        return entries.get(index).value;
    }

    public String getNameByIndex(int index) {
        return entries.get(index).name;
    }

    public void markInitialState() {
        initialEntries = entries.stream()
                .map(e -> new Entry(e.name, e.value))
                .collect(Collectors.toList());
    }

    public void reset() {
        entries.clear();
        initialEntries.stream()
                .map(e -> new Entry(e.name, e.value))
                .forEach(entries::add);
    }

    public Integer getMaxEntries() {
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

    public boolean isVarIntBitCounts() {
        return varIntBitCounts;
    }

    public int getEntryCount() {
        return entries.size();
    }

    public String toString() {
        var t = new TextTable.Builder()
            .setTitle(getName())
            .setFrame(TextTable.FRAME_COMPAT)
            .addColumn("Index", Alignment.RIGHT)
            .addColumn("Key", Alignment.RIGHT)
            .addColumn("Value", Alignment.RIGHT)
            .build();
        var n = entries.size();
        for (var i = 0; i < n; i++) {
            var v = getValueByIndex(i);

            t.setData(i, 0, i);
            t.setData(i, 1, getNameByIndex(i));
            t.setData(i, 2, v != null ? (v.size() + " bytes") : null);
        }
        return t.toString();
    }

    private static class Entry {

        private final String name;
        private ByteString value;

        private Entry(String name, ByteString value) {
            this.name = name;
            this.value = value;
        }

    }

}
