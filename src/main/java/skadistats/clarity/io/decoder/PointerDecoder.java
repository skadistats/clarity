package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.s2.Pointer;
import skadistats.clarity.io.s2.SerializerId;

public class PointerDecoder implements Decoder<Pointer> {

    private final SerializerId[] types;

    public PointerDecoder(SerializerId[] types) {
        this.types = types;
    }

    @Override
    public Pointer decode(BitStream bs) {
        var enabled = bs.readBitFlag();
        Integer index = null;
        if (enabled && types.length > 1) {
            index = bs.readUBitVar();
        }
        var description = index == null ? "null" : types[index].toString();
        return new Pointer(index, description);
    }

}
