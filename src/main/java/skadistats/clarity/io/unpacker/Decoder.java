package skadistats.clarity.io.unpacker;

import skadistats.clarity.io.bitstream.BitStream;

public interface Decoder<T> {
    T decode(BitStream bs);
}
