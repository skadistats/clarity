package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

@RegisterDecoder
public final class CUtlBinaryBlockDecoder extends Decoder {

    public static byte[] decode(BitStream bs) {
        var n = bs.readVarUInt();
        var out = new byte[n];
        bs.readBitsIntoByteArray(out, n * 8);
        return out;
    }

}
