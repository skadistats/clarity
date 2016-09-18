package skadistats.clarity.util;

public class TriStateTable {

    private final int[] field;

    public TriStateTable(int size) {
        field = new int[(size + 15) >> 4];
    }

    private int getInternal(int n) {
        int offs = n >> 4;
        int shift = (n & 15) << 1;
        return (field[offs] >> shift) & 3;
    }

    private void setInternal(int n, int v) {
        int offs = n >> 4;
        int shift = (n & 15) << 1;
        int mask = ~(3 << shift);
        field[offs] = (field[offs] & mask) | (v << shift);
    }

    public void set(int n, TriState state) {
        setInternal(n, state.ordinal());
    }

    public TriState get(int n) {
        return TriState.values()[getInternal(n)];
    }

}
