package skadistats.clarity.io.decoder.factory.s1;

import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.decoder.VectorDecoder;
import skadistats.clarity.io.decoder.VectorXYDecoder;
import skadistats.clarity.model.s1.PropFlag;
import skadistats.clarity.model.s1.SendProp;

public class VectorDecoderFactory {

    public static Decoder createDecoder(int dim, SendProp prop) {
        if (dim == 3) {
            return new VectorDecoder(FloatDecoderFactory.createDecoder(prop), (prop.getFlags() & PropFlag.NORMAL) != 0);
        } else {
            return new VectorXYDecoder(FloatDecoderFactory.createDecoder(prop));
        }
    }

}
