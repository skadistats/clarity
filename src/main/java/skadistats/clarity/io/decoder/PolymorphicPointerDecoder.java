package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.s2.Pointer;
import skadistats.clarity.model.s2.SerializerId;

@RegisterDecoder
public final class PolymorphicPointerDecoder extends Decoder {

    private final SerializerId[] types;

    public PolymorphicPointerDecoder(SerializerId[] types) {
        this.types = types;
    }

    public static Pointer decode(BitStream bs, PolymorphicPointerDecoder d) {
        var enabled = bs.readBitFlag();
        Integer index = null;
        if (enabled) {
            index = bs.readUBitVar();
        }
        var description = index == null ? "null" : d.types[index].toString();
        return new Pointer(index, description);
    }

}
