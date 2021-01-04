package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;

public class LongVarUnsignedDecoder implements Decoder<Long> {

    @Override
    public Long decode(BitStream bs) {
        return bs.readVarULong();
    }

}
