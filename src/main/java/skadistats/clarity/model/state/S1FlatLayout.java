package skadistats.clarity.model.state;

import skadistats.clarity.io.decoder.StringLenDecoder;
import skadistats.clarity.io.s1.ReceiveProp;

public final class S1FlatLayout {

    public enum LeafKind { PRIMITIVE, INLINE_STRING, REF }

    public static final int INLINE_STRING_MAX_LENGTH = 512;

    private final LeafKind[] kinds;
    private final int[] offsets;
    private final PrimitiveType[] primTypes;
    private final int[] maxLengths;
    private final int dataBytes;
    private final int refSlots;

    private S1FlatLayout(LeafKind[] kinds, int[] offsets, PrimitiveType[] primTypes,
                         int[] maxLengths, int dataBytes, int refSlots) {
        this.kinds = kinds;
        this.offsets = offsets;
        this.primTypes = primTypes;
        this.maxLengths = maxLengths;
        this.dataBytes = dataBytes;
        this.refSlots = refSlots;
    }

    public LeafKind[] kinds() { return kinds; }
    public int[] offsets() { return offsets; }
    public PrimitiveType[] primTypes() { return primTypes; }
    public int[] maxLengths() { return maxLengths; }
    public int dataBytes() { return dataBytes; }
    public int refSlots() { return refSlots; }

    public static S1FlatLayout build(ReceiveProp[] receiveProps) {
        var n = receiveProps.length;
        var kinds = new LeafKind[n];
        var offsets = new int[n];
        var primTypes = new PrimitiveType[n];
        var maxLengths = new int[n];
        var offset = 0;
        var refSlots = 0;
        for (var i = 0; i < n; i++) {
            var decoder = receiveProps[i].getSendProp().getDecoder();
            var primType = decoder.getPrimitiveType();
            offsets[i] = offset;
            if (primType != null) {
                kinds[i] = LeafKind.PRIMITIVE;
                primTypes[i] = primType;
                offset += 1 + primType.size();
            } else if (decoder instanceof StringLenDecoder) {
                kinds[i] = LeafKind.INLINE_STRING;
                maxLengths[i] = INLINE_STRING_MAX_LENGTH;
                offset += 1 + 2 + INLINE_STRING_MAX_LENGTH;
            } else {
                kinds[i] = LeafKind.REF;
                offset += 1 + 4;
                refSlots++;
            }
        }
        return new S1FlatLayout(kinds, offsets, primTypes, maxLengths, offset, refSlots);
    }
}
