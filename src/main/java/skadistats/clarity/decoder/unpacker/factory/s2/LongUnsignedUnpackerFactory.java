package skadistats.clarity.decoder.unpacker.factory.s2;

import skadistats.clarity.decoder.unpacker.LongFixedUnsignedUnpacker;
import skadistats.clarity.decoder.unpacker.LongVarUnsignedUnpacker;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.s2.field.FieldProperties;

public class LongUnsignedUnpackerFactory implements UnpackerFactory<Long> {

    public static Unpacker<Long> createUnpackerStatic(FieldProperties f) {
        if ("fixed64".equals(f.getEncoder())) {
            return new LongFixedUnsignedUnpacker();
        }
        return new LongVarUnsignedUnpacker();
    }

    @Override
    public Unpacker<Long> createUnpacker(FieldProperties f) {
        return createUnpackerStatic(f);
    }

}
