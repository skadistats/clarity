package skadistats.clarity.decoder.unpacker.factory.s2;

import skadistats.clarity.decoder.unpacker.LongUnsignedUnpacker;
import skadistats.clarity.decoder.unpacker.LongVarUnsignedUnpacker;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.s2.field.FieldProperties;

public class LongUnsignedUnpackerFactory implements UnpackerFactory<Long> {

    public static Unpacker<Long> createUnpackerStatic(FieldProperties f) {
        if ("fixed64".equals(f.getEncoder())) {
            return new LongUnsignedUnpacker(64);
        }
        return new LongVarUnsignedUnpacker();
    }

    @Override
    public Unpacker<Long> createUnpacker(FieldProperties f) {
        return createUnpackerStatic(f);
    }

}
