package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;

public class ArrayUnpacker<T> implements Unpacker<T[]> {

    private final Unpacker<T> unpacker;
    private final int nSizeBits;

    public ArrayUnpacker(Unpacker<T> unpacker, int nSizeBits) {
        this.unpacker = unpacker;
        this.nSizeBits = nSizeBits;
    }

    @Override
    public T[] unpack(BitStream bs) {
        int count = bs.readUBitInt(nSizeBits);
        T[] result = (T[]) new Object[count];
        int i = 0;
        while (i < count) {
            result[i++] = unpacker.unpack(bs);
        }
        return result;
    }
    
}
