package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

public class IntMinusOneDecoder implements Decoder<Integer> {

    @Override
    public Integer decode(BitStream bs) {
        return bs.readVarUInt() - 1;
    }

}
