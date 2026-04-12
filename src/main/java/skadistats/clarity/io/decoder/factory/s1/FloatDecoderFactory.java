package skadistats.clarity.io.decoder.factory.s1;

import skadistats.clarity.io.s1.SendProp;
import skadistats.clarity.io.decoder.*;
import skadistats.clarity.model.s1.PropFlag;

public class FloatDecoderFactory {

    public static Decoder createDecoder(SendProp prop) {
        var flags = prop.getFlags();
        if ((flags & PropFlag.COORD) != 0) {
            return new FloatCoordDecoder();
        } else if ((flags & (PropFlag.COORD_MP | PropFlag.COORD_MP_LOW_PRECISION | PropFlag.COORD_MP_INTEGRAL)) != 0) {
            return new FloatCoordMpDecoder(
                (flags & PropFlag.COORD_MP_INTEGRAL) != 0,
                (flags & PropFlag.COORD_MP_LOW_PRECISION) != 0
            );
        } else if ((flags & PropFlag.NO_SCALE) != 0) {
            return new FloatNoScaleDecoder();
        } else if ((flags & PropFlag.NORMAL) != 0) {
            return new FloatNormalDecoder();
        } else if ((flags & (PropFlag.CELL_COORD | PropFlag.CELL_COORD_INTEGRAL | PropFlag.CELL_COORD_LOW_PRECISION)) != 0) {
            return new FloatCellCoordDecoder(
                prop.getNumBits(),
                (flags & PropFlag.CELL_COORD_INTEGRAL) != 0,
                (flags & PropFlag.CELL_COORD_LOW_PRECISION) != 0
            );
        } else {
            return new FloatDefaultDecoder(prop.getNumBits(), prop.getLowValue(), prop.getHighValue());
        }
    }

}
