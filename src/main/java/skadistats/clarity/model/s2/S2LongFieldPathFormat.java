package skadistats.clarity.model.s2;

import skadistats.clarity.ClarityException;

import static skadistats.clarity.model.s2.S2FieldPath.MAX_DEPTH;

public class S2LongFieldPathFormat {

    private static final int[] BITS_PER_COMPONENT = { 11, 12, 11, 8, 7, 4, 4 };

    static {
        if (BITS_PER_COMPONENT.length != MAX_DEPTH) {
            throw new IllegalStateException(
                "BITS_PER_COMPONENT must have exactly " + MAX_DEPTH + " entries to match S2FieldPath.MAX_DEPTH");
        }
    }

    /**
     * Returns the highest index value that can be addressed at the given
     * field path depth, i.e. {@code (1 << bitsAt(depth)) - 1}. Returns -1
     * if {@code depth} is outside the supported field path range, meaning
     * no element at that depth can be addressed at all.
     */
    public static int maxIndexAtDepth(int depth) {
        if (depth < 0 || depth >= MAX_DEPTH) {
            return -1;
        }
        return (1 << BITS_PER_COMPONENT[depth]) - 1;
    }

    private static final long[] CLEAR_MASK = new long[MAX_DEPTH - 1];
    private static final long[] PRESENT_BIT = new long[MAX_DEPTH - 1];
    private static final long[] VALUE_SHIFT = new long[MAX_DEPTH];
    private static final long[] VALUE_MASK = new long[MAX_DEPTH];
    private static final long[] OFFSET = new long[MAX_DEPTH];
    private static final long PRESENT_MASK;

    public static long set(long id, int i, int v) {
        if (v < 0 || v > maxIndexAtDepth(i)) {
            throw new ClarityException(
                "field path component %d cannot hold the value %d (max is %d). " +
                "This is a limitation of clarity's packed long field path format, " +
                "not a problem with the replay. Please open a github issue and attach the demo so the bit layout can be adjusted.",
                i, v, maxIndexAtDepth(i)
            );
        }
        return id & ~VALUE_MASK[i] | ((long)v + OFFSET[i]) << VALUE_SHIFT[i];
    }

    public static int get(long id, int i) {
        return (int)(((id & VALUE_MASK[i]) >> VALUE_SHIFT[i]) - OFFSET[i]);
    }

    public static long down(long id) {
        var l = last(id);
        if (l + 1 >= MAX_DEPTH) {
            throw new ClarityException(
                "field path depth would exceed clarity's maximum of %d. " +
                "This is a limitation of clarity's packed long field path format, " +
                "not a problem with the replay. Please open a github issue and attach the demo so the bit layout can be adjusted.",
                MAX_DEPTH
            );
        }
        return id | PRESENT_BIT[l];
    }

    public static long up(long id, int n) {
        return id & CLEAR_MASK[last(id) - n];
    }

    public static int last(long id) {
        return Long.bitCount(id & PRESENT_MASK);
    }

    public static int hashCode(long id) {
        return Long.hashCode(id);
    }

    public static int compareTo(long id1, long id2) {
        return Long.compare(id1, id2);
    }

    static {
        var bitCount = -1;
        for (var i = 0; i < MAX_DEPTH; i++) {
            bitCount += BITS_PER_COMPONENT[i] + 1;
        }
        if (bitCount > 63) {
            throw new UnsupportedOperationException("too many bits used");
        }
        var cur = bitCount;
        var presentMaskAkku = 0L;
        for (var i = 0; i < MAX_DEPTH; i++) {
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
