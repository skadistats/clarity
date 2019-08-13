package skadistats.clarity.model.s2;

public class S2LongFieldPath extends S2FieldPath<S2LongFieldPath> {

    private static final int[] BITS_PER_COMPONENT = { 11, 11, 11, 11, 11 };

    private static final long[] OFFSET = new long[BITS_PER_COMPONENT.length];
    private static final long[] VALUE_MASK = new long[BITS_PER_COMPONENT.length];
    private static final long[] VALUE_SHIFT = new long[BITS_PER_COMPONENT.length];
    private static final long[] PRESENT_BIT = new long[BITS_PER_COMPONENT.length - 1];
    private static final long[] CLEAR_MASK = new long[BITS_PER_COMPONENT.length - 1];
    private static final long PRESENT_MASK;

    private long id;

    S2LongFieldPath() {
        id = 0;
    }

    S2LongFieldPath(S2LongFieldPath other) {
        id = other.id;
    }

    @Override
    S2LongFieldPath copy() {
        return new S2LongFieldPath(this);
    }

    @Override
    public void set(int i, int v) {
        id = id & ~VALUE_MASK[i] | ((long)v + OFFSET[i]) << VALUE_SHIFT[i];
    }

    @Override
    public int get(int i) {
        return (int)(((id & VALUE_MASK[i]) >> VALUE_SHIFT[i]) - OFFSET[i]);
    }

    @Override
    public void down() {
        id |= PRESENT_BIT[last()];
    }

    @Override
    public void up(int n) {
        id &= CLEAR_MASK[last() - n];
    }

    @Override
    public int last() {
        return Long.bitCount(id & PRESENT_MASK);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof S2LongFieldPath) {
            return id == ((S2LongFieldPath) o).id;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public int compareTo(S2LongFieldPath o) {
        return Long.compare(id, o.id);
    }

    static {
        int bitCount = -1;
        for (int i = 0; i < BITS_PER_COMPONENT.length; i++) {
            bitCount += BITS_PER_COMPONENT[i] + 1;
        }
        int cur = bitCount;
        long presentMaskAkku = 0L;
        for (int i = 0; i < BITS_PER_COMPONENT.length; i++) {
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
