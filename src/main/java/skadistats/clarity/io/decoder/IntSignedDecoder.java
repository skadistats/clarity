package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

public class IntSignedDecoder implements Decoder<Integer> {

    private final int nBits;

    public IntSignedDecoder(int nBits) {
        this.nBits = nBits;
    }

    @Override
    public Integer decode(BitStream bs) {
        return bs.readSBitInt(nBits);
    }

}
