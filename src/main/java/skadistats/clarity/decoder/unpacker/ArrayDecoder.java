package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;

public class ArrayDecoder<T> implements Decoder<T[]> {

    private final Decoder<T> decoder;
    private final int nSizeBits;

    public ArrayDecoder(Decoder<T> decoder, int nSizeBits) {
        this.decoder = decoder;
        this.nSizeBits = nSizeBits;
    }

    @Override
    public T[] decode(BitStream bs) {
        int count = bs.readUBitInt(nSizeBits);
        T[] result = (T[]) new Object[count];
        int i = 0;
        while (i < count) {
            result[i++] = decoder.decode(bs);
        }
        return result;
    }

}
