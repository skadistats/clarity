package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;

public class StringLenDecoder implements Decoder<String> {

    @Override
    public String decode(BitStream bs) {
        return bs.readString(bs.readUBitInt(9));
    }

}
