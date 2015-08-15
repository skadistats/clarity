package skadistats.clarity.decoder.s2.prop;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.s2.FieldDecoder;
import skadistats.clarity.model.Vector;
import skadistats.clarity.model.s2.Field;

public class VectorDecoder implements FieldDecoder<Vector> {

    @Override
    public Vector decode(BitStream bs, Field f) {
        Float32Decoder fd = new Float32Decoder();
        return new Vector(
          new float[] {
              fd.decode(bs, f),
              fd.decode(bs, f)
          }
        );
    }
}
