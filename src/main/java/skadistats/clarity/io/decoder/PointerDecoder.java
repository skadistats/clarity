package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.s2.Pointer;
import skadistats.clarity.model.s2.SerializerId;

@RegisterDecoder
public final class PointerDecoder extends Decoder {

    private final SerializerId[] types;

    public PointerDecoder(SerializerId[] types) {
        this.types = types;
    }

    public static Pointer decode(BitStream bs, PointerDecoder d) {
        var enabled = bs.readBitFlag();
        Integer index = null;
        if (enabled && d.types.length > 1) {
            index = bs.readUBitVar();
        }
        var description = index == null ? "null" : d.types[index].toString();
        return new Pointer(index, description);
    }

}
