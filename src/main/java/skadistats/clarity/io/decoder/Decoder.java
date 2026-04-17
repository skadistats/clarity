package skadistats.clarity.io.decoder;

import skadistats.clarity.state.PrimitiveType;

public abstract class Decoder {

    protected Decoder() {}

    public PrimitiveType getPrimitiveType() {
        return null;
    }
}
