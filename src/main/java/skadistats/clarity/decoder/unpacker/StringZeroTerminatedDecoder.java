package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;

public class StringZeroTerminatedDecoder implements Decoder<String> {

    @Override
    public String decode(BitStream bs) {
        return bs.readString(Integer.MAX_VALUE);
    }

}
