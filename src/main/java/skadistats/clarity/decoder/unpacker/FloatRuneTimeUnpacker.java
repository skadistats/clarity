package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;

public class FloatRuneTimeUnpacker implements Unpacker<Float> {

    @Override
    public Float unpack(BitStream bs) {
	return Float.intBitsToFloat(bs.readUBitInt(4));
    }
    
}
