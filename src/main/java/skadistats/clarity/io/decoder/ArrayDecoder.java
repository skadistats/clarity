package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

@RegisterDecoder
public final class ArrayDecoder extends Decoder {

    private final Decoder decoder;
    private final int nSizeBits;

    public ArrayDecoder(Decoder decoder, int nSizeBits) {
        this.decoder = decoder;
        this.nSizeBits = nSizeBits;
    }

    public static Object[] decode(BitStream bs, ArrayDecoder d) {
        var count = bs.readUBitInt(d.nSizeBits);
        var result = new Object[count];
        var i = 0;
        while (i < count) {
            result[i++] = DecoderDispatch.decode(bs, d.decoder);
        }
        return result;
    }

}
