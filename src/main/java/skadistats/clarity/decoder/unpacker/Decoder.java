package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;

public interface Decoder<T> {
    T decode(BitStream bs);
}
