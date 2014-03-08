package skadistats.clarity.model;

import skadistats.clarity.decoder.prop.ArrayDecoder;
import skadistats.clarity.decoder.prop.FloatDecoder;
import skadistats.clarity.decoder.prop.Int64Decoder;
import skadistats.clarity.decoder.prop.IntDecoder;
import skadistats.clarity.decoder.prop.PropDecoder;
import skadistats.clarity.decoder.prop.StringDecoder;
import skadistats.clarity.decoder.prop.VectorDecoder;
import skadistats.clarity.decoder.prop.VectorXYDecoder;

public enum PropType {
    INT(new IntDecoder()),
    FLOAT(new FloatDecoder()),
    VECTOR(new VectorDecoder()),
    VECTOR_XY(new VectorXYDecoder()),
    STRING(new StringDecoder()),
    ARRAY(new ArrayDecoder()),
    DATATABLE(null),
    INT64(new Int64Decoder());

    private final PropDecoder<?> decoder;

    private PropType(PropDecoder<?> decoder) {
        this.decoder = decoder;
    }

    public PropDecoder<?> getDecoder() {
        return decoder;
    }

}