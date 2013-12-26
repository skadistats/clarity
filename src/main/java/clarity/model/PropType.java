package clarity.model;

import clarity.decoder.prop.ArrayDecoder;
import clarity.decoder.prop.PropDecoder;
import clarity.decoder.prop.FloatDecoder;
import clarity.decoder.prop.Int64Decoder;
import clarity.decoder.prop.IntDecoder;
import clarity.decoder.prop.StringDecoder;
import clarity.decoder.prop.VectorDecoder;
import clarity.decoder.prop.VectorXYDecoder;

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