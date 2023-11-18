package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

public class ArrayDecoder<T> implements Decoder<T[]> {

    private final Decoder<T> decoder;
    private final int nSizeBits;

    public ArrayDecoder(Decoder<T> decoder, int nSizeBits) {
        this.decoder = decoder;
        this.nSizeBits = nSizeBits;
    }

    @Override
    public T[] decode(BitStream bs) {
        var count = bs.readUBitInt(nSizeBits);
        var result = (T[]) new Object[count];
        var i = 0;
        while (i < count) {
            result[i++] = decoder.decode(bs);
        }
        return result;
    }

}
