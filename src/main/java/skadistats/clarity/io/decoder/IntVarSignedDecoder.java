package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

public class IntVarSignedDecoder implements Decoder<Integer> {

    @Override
    public Integer decode(BitStream bs) {
        return bs.readVarSInt();
    }

}
