package skadistats.clarity.io.s2;

import skadistats.clarity.io.decoder.Decoder;

public class DecoderHolder {

    private final DecoderProperties decoderProperties;
    private final Decoder decoder;

    public DecoderHolder(DecoderProperties decoderProperties, Decoder decoder) {
        this.decoderProperties = decoderProperties;
        this.decoder = decoder;
    }

    public DecoderProperties getDecoderProperties() {
        return decoderProperties;
    }

    public Decoder getDecoder() {
        return decoder;
    }

}
