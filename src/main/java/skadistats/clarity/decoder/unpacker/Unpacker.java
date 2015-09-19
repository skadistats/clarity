package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;

public interface Unpacker<T> {
    T unpack(BitStream bs);
}
