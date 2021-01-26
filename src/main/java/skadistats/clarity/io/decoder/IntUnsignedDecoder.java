package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

public class IntUnsignedDecoder implements Decoder<Integer> {

    private final int nBits;

    public IntUnsignedDecoder(int nBits) {
        this.nBits = nBits;
    }

    @Override
    public Integer decode(BitStream bs) {
        return bs.readUBitInt(nBits);
    }

}
