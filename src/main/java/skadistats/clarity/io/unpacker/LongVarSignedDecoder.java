package skadistats.clarity.io.unpacker;

import skadistats.clarity.io.bitstream.BitStream;

public class LongVarSignedDecoder implements Decoder<Long> {

    @Override
    public Long decode(BitStream bs) {
        return bs.readVarSLong();
    }

}
