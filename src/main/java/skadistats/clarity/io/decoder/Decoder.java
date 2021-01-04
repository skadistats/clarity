package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

public interface Decoder<T> {
    T decode(BitStream bs);
}
