package skadistats.clarity.decoder.unpacker.factory.s1;

import skadistats.clarity.decoder.s1.SendProp;
import skadistats.clarity.decoder.unpacker.Decoder;
import skadistats.clarity.decoder.unpacker.VectorDecoder;
import skadistats.clarity.decoder.unpacker.VectorXYDecoder;
import skadistats.clarity.model.Vector;
import skadistats.clarity.model.s1.PropFlag;

public class VectorDecoderFactory implements DecoderFactory<Vector> {

    private final int dim;

    public VectorDecoderFactory(int dim) {
        this.dim = dim;
    }

    public static Decoder<Vector> createDecoderStatic(int dim, SendProp prop) {
        if (dim == 3) {
            return new VectorDecoder(FloatDecoderFactory.createDecoderStatic(prop), (prop.getFlags() & PropFlag.NORMAL) != 0);
        } else {
            return new VectorXYDecoder(FloatDecoderFactory.createDecoderStatic(prop));
        }
    }

    @Override
    public Decoder<Vector> createDecoder(SendProp prop) {
        return createDecoderStatic(dim, prop);
    }

}
