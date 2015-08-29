package skadistats.clarity.decoder.unpacker.factory.s1;

import skadistats.clarity.decoder.unpacker.*;
import skadistats.clarity.model.s1.PropFlag;
import skadistats.clarity.model.s1.SendProp;

public class FloatUnpackerFactory implements UnpackerFactory<Float> {

    public static Unpacker<Float> createUnpackerStatic(SendProp prop) {
        int flags = prop.getFlags();
        if ((flags & PropFlag.COORD) != 0) {
            return new FloatCoordUnpacker();
        } else if ((flags & (PropFlag.COORD_MP | PropFlag.COORD_MP_LOW_PRECISION | PropFlag.COORD_MP_INTEGRAL)) != 0) {
            return new FloatCoordMpUnpacker(
                (flags & PropFlag.COORD_MP_INTEGRAL) != 0,
                (flags & PropFlag.COORD_MP_LOW_PRECISION) != 0
            );
        } else if ((flags & PropFlag.NO_SCALE) != 0) {
            return new FloatNoScaleUnpacker();
        } else if ((flags & PropFlag.NORMAL) != 0) {
            return new FloatNormalUnpacker();
        } else if ((flags & (PropFlag.CELL_COORD | PropFlag.CELL_COORD_INTEGRAL | PropFlag.CELL_COORD_LOW_PRECISION)) != 0) {
            return new FloatCellCoordUnpacker(
                prop.getNumBits(),
                (flags & PropFlag.CELL_COORD_INTEGRAL) != 0,
                (flags & PropFlag.CELL_COORD_LOW_PRECISION) != 0
            );
        } else {
            return new FloatDefaultUnpacker(prop.getNumBits(), prop.getLowValue(), prop.getHighValue());
        }
    }

    @Override
    public Unpacker<Float> createUnpacker(SendProp prop) {
        return createUnpackerStatic(prop);
    }
}
