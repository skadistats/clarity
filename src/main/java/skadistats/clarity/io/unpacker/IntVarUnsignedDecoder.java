package skadistats.clarity.io.unpacker;

import skadistats.clarity.io.bitstream.BitStream;

public class IntVarUnsignedDecoder implements Decoder<Integer> {

    @Override
    public Integer decode(BitStream bs) {
        return bs.readVarUInt();
    }

}
