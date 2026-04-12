package skadistats.clarity.io.decoder.factory.s1;

import skadistats.clarity.io.s1.SendProp;
import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.decoder.VectorDecoder;
import skadistats.clarity.io.decoder.VectorXYDecoder;
import skadistats.clarity.model.s1.PropFlag;

public class VectorDecoderFactory {

    public static Decoder createDecoder(int dim, SendProp prop) {
        if (dim == 3) {
            return new VectorDecoder(FloatDecoderFactory.createDecoder(prop), (prop.getFlags() & PropFlag.NORMAL) != 0);
        } else {
            return new VectorXYDecoder(FloatDecoderFactory.createDecoder(prop));
        }
    }

}
