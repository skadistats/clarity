package skadistats.clarity.model.s1;

import skadistats.clarity.decoder.s1.prop.*;

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