package skadistats.clarity.model.s2;

public class S2LongFieldPathFormat {

    private static final int[] BITS_PER_COMPONENT = { 11, 11, 11, 8, 8, 4, 4 };

    public static final int MAX_FIELDPATH_LENGTH = BITS_PER_COMPONENT.length;

    private static final long[] CLEAR_MASK = new long[MAX_FIELDPATH_LENGTH - 1];
    private static final long[] PRESENT_BIT = new long[MAX_FIELDPATH_LENGTH - 1];
    private static final long[] VALUE_SHIFT = new long[MAX_FIELDPATH_LENGTH];
    private static final long[] VALUE_MASK = new long[MAX_FIELDPATH_LENGTH];
    private static final long[] OFFSET = new long[MAX_FIELDPATH_LENGTH];
    private static final long PRESENT_MASK;

    static long set(long id, int i, int v) {
        assert v <= (1 << BITS_PER_COMPONENT[i]) - 1;
        return id & ~VALUE_MASK[i] | ((long)v + OFFSET[i]) << VALUE_SHIFT[i];
    }

    static int get(long id, int i) {
        return (int)(((id & VALUE_MASK[i]) >> VALUE_SHIFT[i]) - OFFSET[i]);
    }

    static long down(long id) {
        return id | PRESENT_BIT[last(id)];
    }

    static long up(long id, int n) {
        return id & CLEAR_MASK[last(id) - n];
    }

    static int last(long id) {
        return Long.bitCount(id & PRESENT_MASK);
    }

    static int hashCode(long id) {
        return Long.hashCode(id);
    }

    static int compareTo(long id1, long id2) {
        return Long.compare(id1, id2);
    }

    static {
        int bitCount = -1;
        for (int i = 0; i < MAX_FIELDPATH_LENGTH; i++) {
            bitCount += BITS_PER_COMPONENT[i] + 1;
        }
        if (bitCount > 63) {
            throw new UnsupportedOperationException("too many bits used");
        }
        int cur = bitCount;
        long presentMaskAkku = 0L;
        for (int i = 0; i < MAX_FIELDPATH_LENGTH; i++) {
            OFFSET[i] = i == 0 ? 1L : 0L;
            if (i != 0) {
                CLEAR_MASK[i - 1] = (-1L << cur) & ((1L << bitCount) - 1);
                cur--;
                PRESENT_BIT[i - 1] = 1L << cur;
                presentMaskAkku |= 1L << cur;
            }
            cur -= BITS_PER_COMPONENT[i];
            VALUE_SHIFT[i] = cur;
            VALUE_MASK[i] = ((1L << BITS_PER_COMPONENT[i]) - 1L) << cur;
        }
        PRESENT_MASK = presentMaskAkku;
    }

}
