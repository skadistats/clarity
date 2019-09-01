package skadistats.clarity.decoder.unpacker.factory.s2;

import skadistats.clarity.decoder.s2.field.UnpackerProperties;
import skadistats.clarity.decoder.unpacker.LongUnsignedUnpacker;
import skadistats.clarity.decoder.unpacker.LongVarUnsignedUnpacker;
import skadistats.clarity.decoder.unpacker.Unpacker;

public class LongUnsignedUnpackerFactory implements UnpackerFactory<Long> {

    public static Unpacker<Long> createUnpackerStatic(UnpackerProperties f) {
        if ("fixed64".equals(f.getEncoderType())) {
            return new LongUnsignedUnpacker(64);
        }
        return new LongVarUnsignedUnpacker();
    }

    @Override
    public Unpacker<Long> createUnpacker(UnpackerProperties f) {
        return createUnpackerStatic(f);
    }

}
