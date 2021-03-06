package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

public class StringZeroTerminatedDecoder implements Decoder<String> {

    @Override
    public String decode(BitStream bs) {
        return bs.readString(Integer.MAX_VALUE);
    }

}
