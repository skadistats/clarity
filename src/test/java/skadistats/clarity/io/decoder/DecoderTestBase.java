package skadistats.clarity.io.decoder;

import static org.testng.Assert.assertEquals;

/**
 * Shared scaffolding for per-decoder branch-coverage tests.
 */
public abstract class DecoderTestBase {

    protected static BitstreamBuilder bitstream() {
        return BitstreamBuilder.bitstream();
    }

    /**
     * Decode and skip on the same bit pattern; assert pos delta matches.
     * Caller-built {@link BitstreamBuilder} is materialized twice so each call
     * gets a fresh cursor at the same starting position.
     */
    protected static void assertSkipParity(BitstreamBuilder b, int leadingSkip, Decoder d) {
        var decodeStream = b.build();
        var skipStream = b.build();
        decodeStream.skip(leadingSkip);
        skipStream.skip(leadingSkip);
        DecoderDispatch.decode(decodeStream, d);
        DecoderDispatch.skip(skipStream, d);
        assertEquals(skipStream.pos(), decodeStream.pos(),
                "skip pos diverged from decode for decoder " + d.getClass().getSimpleName()
                        + " (leadingSkip=" + leadingSkip + ")");
    }

    protected static void assertSkipParity(BitstreamBuilder b, Decoder d) {
        assertSkipParity(b, 0, d);
    }

    /** Convenience: assert skip advances bs by exactly {@code expectedBits}. */
    protected static void assertSkipsBits(BitstreamBuilder b, int leadingSkip, Decoder d, int expectedBits) {
        var bs = b.build();
        bs.skip(leadingSkip);
        var start = bs.pos();
        DecoderDispatch.skip(bs, d);
        assertEquals(bs.pos() - start, expectedBits,
                "skip bit count for " + d.getClass().getSimpleName()
                        + " (leadingSkip=" + leadingSkip + ")");
    }
}
