package skadistats.clarity.io.decoder.factory.s1;

import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.decoder.FloatCellCoordDecoder;
import skadistats.clarity.io.decoder.FloatCoordDecoder;
import skadistats.clarity.io.decoder.FloatCoordMpDecoder;
import skadistats.clarity.io.decoder.FloatDefaultDecoder;
import skadistats.clarity.io.decoder.FloatNoScaleDecoder;
import skadistats.clarity.io.decoder.FloatNormalDecoder;
import skadistats.clarity.model.s1.PropFlag;
import skadistats.clarity.model.s1.SendProp;

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
