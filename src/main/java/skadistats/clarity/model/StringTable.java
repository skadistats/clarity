package skadistats.clarity.model;

import skadistats.clarity.protobuf.ByteString;
import skadistats.clarity.util.TextTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static skadistats.clarity.util.TextTable.Alignment;

/**
 * A replay string table: an ordered list of entries, each a string key with an
 * optional binary value. Tables are filled and updated by the parser; the
 * fixed-size and flag accessors expose the parameters the table was created
 * with.
 */
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

    /** Created by the parser from the table's create message. */
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

    /** Internal to the parser. */
    public void setValueForIndex(int index, ByteString value) {
        entries.get(index).value = value;
    }

    /** Internal to the parser. */
    public void addEntry(String name, ByteString value) {
        entries.add(new Entry(name, value));
    }

    /**
     * @return true if {@code index} addresses an existing entry
     */
    public boolean hasIndex(int index) {
        return index >= 0 && index < entries.size();
    }

    /**
     * @return the value of the entry at {@code index}, possibly {@code null}
     * @throws IndexOutOfBoundsException if there is no such entry
     */
    public ByteString getValueByIndex(int index) {
        return entries.get(index).value;
    }

    /**
     * @return the key of the entry at {@code index}
     * @throws IndexOutOfBoundsException if there is no such entry
     */
    public String getNameByIndex(int index) {
        return entries.get(index).name;
    }

    /** Internal to the parser: remembers the current entries for {@link #reset()}. */
    public void markInitialState() {
        initialEntries = entries.stream()
                .map(e -> new Entry(e.name, e.value))
                .collect(Collectors.toList());
    }

    /** Internal to the parser: restores the entries remembered by {@code markInitialState}. */
    public void reset() {
        entries.clear();
        initialEntries.stream()
                .map(e -> new Entry(e.name, e.value))
                .forEach(entries::add);
    }

    /**
     * @return the maximum number of entries the table was declared with; {@code null} on Source 2
     */
    public Integer getMaxEntries() {
        return maxEntries;
    }

    /** @return whether entry values have a fixed size, as declared by the create message */
    public boolean getUserDataFixedSize() {
        return userDataFixedSize;
    }

    /** @return the fixed value size in bytes, as declared by the create message */
    public int getUserDataSize() {
        return userDataSize;
    }

    /** @return the fixed value size in bits, as declared by the create message */
    public int getUserDataSizeBits() {
        return userDataSizeBits;
    }

    /**
     * @return the table name
     */
    public String getName() {
        return name;
    }

    /** @return the table flags from the create message */
    public int getFlags() {
        return flags;
    }

    /** @return the {@code using_varint_bitcounts} flag from the create message; always {@code false} on Source 1 */
    public boolean isVarIntBitCounts() {
        return varIntBitCounts;
    }

    /**
     * @return the current number of entries
     */
    public int getEntryCount() {
        return entries.size();
    }

    /**
     * @return a table dump of all entries, with value sizes in bytes
     */
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
