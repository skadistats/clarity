package clarity.model;

import clarity.decoder.dt.ArrayDecoder;
import clarity.decoder.dt.DtDecoder;
import clarity.decoder.dt.FloatDecoder;
import clarity.decoder.dt.Int64Decoder;
import clarity.decoder.dt.IntDecoder;
import clarity.decoder.dt.StringDecoder;
import clarity.decoder.dt.VectorDecoder;
import clarity.decoder.dt.VectorXYDecoder;

public enum PropType {
    INT(new IntDecoder()), 
    FLOAT(new FloatDecoder()), 
    VECTOR(new VectorDecoder()), 
    VECTOR_XY(new VectorXYDecoder()), 
    STRING(new StringDecoder()), 
    ARRAY(new ArrayDecoder()), 
    DATATABLE(null), 
    INT64(new Int64Decoder());
    
    private final DtDecoder<?> decoder;

	private PropType(DtDecoder<?> decoder) {
		this.decoder = decoder;
	}

	public DtDecoder getDecoder() {
		return decoder;
	}
    
}