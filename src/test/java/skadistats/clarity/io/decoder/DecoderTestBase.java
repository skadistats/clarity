package skadistats.clarity.io.decoder;

/**
 * Shared scaffolding for per-decoder branch-coverage tests.
 *
 * <p>{@code assertSkipParity} is added alongside the first decoder skip method
 * (chunk B of entity-class-filter). For now this base only exposes the
 * {@link BitstreamBuilder} entry point.
 */
public abstract class DecoderTestBase {

    protected static BitstreamBuilder bitstream() {
        return BitstreamBuilder.bitstream();
    }
}
