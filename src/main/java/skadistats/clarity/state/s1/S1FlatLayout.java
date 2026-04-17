package skadistats.clarity.state.s1;

import skadistats.clarity.io.decoder.StringLenDecoder;
import skadistats.clarity.model.s1.ReceiveProp;
import skadistats.clarity.state.FieldLayout;

public final class S1FlatLayout {

    public static final int INLINE_STRING_MAX_LENGTH = 512;

    private final FieldLayout[] leaves;
    private final int dataBytes;
    private final int refSlots;

    private S1FlatLayout(FieldLayout[] leaves, int dataBytes, int refSlots) {
        this.leaves = leaves;
        this.dataBytes = dataBytes;
        this.refSlots = refSlots;
    }

    public FieldLayout[] leaves() { return leaves; }
    public int dataBytes() { return dataBytes; }
    public int refSlots() { return refSlots; }

    public static S1FlatLayout build(ReceiveProp[] receiveProps) {
        var n = receiveProps.length;
        var leaves = new FieldLayout[n];
        var offset = 0;
        var refSlots = 0;
        for (var i = 0; i < n; i++) {
            var decoder = receiveProps[i].getSendProp().getDecoder();
            var primType = decoder.getPrimitiveType();
            if (primType != null) {
                leaves[i] = new FieldLayout.Primitive(offset, primType);
                offset += 1 + primType.size();
            } else if (decoder instanceof StringLenDecoder) {
                leaves[i] = new FieldLayout.InlineString(offset, INLINE_STRING_MAX_LENGTH);
                offset += 1 + 2 + INLINE_STRING_MAX_LENGTH;
            } else {
                leaves[i] = new FieldLayout.Ref(offset);
                offset += 1 + 4;
                refSlots++;
            }
        }
        return new S1FlatLayout(leaves, offset, refSlots);
    }
}
