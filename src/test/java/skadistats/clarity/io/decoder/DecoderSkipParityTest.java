package skadistats.clarity.io.decoder;

import com.google.protobuf.ByteString;
import org.testng.annotations.Test;
import skadistats.clarity.io.bitstream.BitStream;

import java.util.Random;

import static org.testng.Assert.assertEquals;

/**
 * Per-decoder skip-parity tests: for every concrete decoder, {@code skip(bs, d)}
 * SHALL advance the bitstream cursor by the same number of bits as
 * {@code decode(bs, d)} would for the same input. Drift corrupts every
 * downstream field in the packet.
 *
 * <p>Random-byte input gives stochastic coverage of branches in the conditional
 * decoders (FloatCoord, FloatCoordMp, …); curated branch-specific cases live in
 * {@link DecoderSkipCuratedTest}.
 */
public class DecoderSkipParityTest {

    private static final long SEED = 0xC01D_CAFEL;

    private static BitStream freshStream(byte[] bytes) {
        return BitStream.createBitStream(ByteString.copyFrom(bytes));
    }

    private static byte[] randomBytes(int n) {
        var r = new Random(SEED);
        var b = new byte[n];
        r.nextBytes(b);
        return b;
    }

    private static void assertParity(Decoder d) {
        for (var leadingSkip : new int[]{0, 1, 3, 7, 13, 31, 63, 65}) {
            assertParity(d, leadingSkip);
        }
    }

    private static void assertParityCurated(BitstreamBuilder b, Decoder d) {
        var decodeStream = b.build();
        var skipStream = b.build();
        DecoderDispatch.decode(decodeStream, d);
        DecoderDispatch.skip(skipStream, d);
        assertEquals(skipStream.pos(), decodeStream.pos(),
                d.getClass().getSimpleName() + " curated skip diverged");
    }

    private static void assertParity(Decoder d, int leadingSkip) {
        var bytes = randomBytes(512);
        var decodeStream = freshStream(bytes);
        var skipStream = freshStream(bytes);
        decodeStream.skip(leadingSkip);
        skipStream.skip(leadingSkip);
        DecoderDispatch.decode(decodeStream, d);
        DecoderDispatch.skip(skipStream, d);
        assertEquals(skipStream.pos(), decodeStream.pos(),
                d.getClass().getSimpleName() + " skip diverged at leadingSkip=" + leadingSkip);
    }

    // ---------- Category 1: bit-count ----------

    @Test public void bool()                 { assertParity(new BoolDecoder()); }
    @Test public void fixedPointer()         { assertParity(new FixedPointerDecoder()); }
    @Test public void intSigned1bit()        { assertParity(new IntSignedDecoder(1)); }
    @Test public void intSigned17bit()       { assertParity(new IntSignedDecoder(17)); }
    @Test public void intSigned32bit()       { assertParity(new IntSignedDecoder(32)); }
    @Test public void intUnsigned1bit()      { assertParity(new IntUnsignedDecoder(1)); }
    @Test public void intUnsigned24bit()     { assertParity(new IntUnsignedDecoder(24)); }
    @Test public void intUnsigned32bit()     { assertParity(new IntUnsignedDecoder(32)); }
    @Test public void longSigned1bit()       { assertParity(new LongSignedDecoder(1)); }
    @Test public void longSigned60bit()      { assertParity(new LongSignedDecoder(60)); }
    @Test public void longSigned64bit()      { assertParity(new LongSignedDecoder(64)); }
    @Test public void longUnsigned1bit()     { assertParity(new LongUnsignedDecoder(1)); }
    @Test public void longUnsigned48bit()    { assertParity(new LongUnsignedDecoder(48)); }
    @Test public void longUnsigned64bit()    { assertParity(new LongUnsignedDecoder(64)); }
    @Test public void floatNoScale()         { assertParity(new FloatNoScaleDecoder()); }
    @Test public void floatDefault4()        { assertParity(new FloatDefaultDecoder(4, -1f, 1f)); }
    @Test public void floatDefault16()       { assertParity(new FloatDefaultDecoder(16, -1f, 1f)); }
    @Test public void floatCellCoord()       { assertParity(new FloatCellCoordDecoder(8, false, false)); }
    @Test public void floatCellCoordInt()    { assertParity(new FloatCellCoordDecoder(8, true, false)); }
    @Test public void floatCellCoordLow()    { assertParity(new FloatCellCoordDecoder(8, false, true)); }
    @Test public void floatNormal()          { assertParity(new FloatNormalDecoder()); }
    @Test public void qAngleBitCount8()      { assertParity(new QAngleBitCountDecoder(8)); }
    @Test public void qAngleBitCount24()     { assertParity(new QAngleBitCountDecoder(24)); }
    @Test public void qAngleNoScale()        { assertParity(new QAngleNoScaleDecoder()); }
    @Test public void qAnglePitchYaw12()     { assertParity(new QAnglePitchYawOnlyDecoder(12)); }
    @Test public void qAnglePitchYaw32()     { assertParity(new QAnglePitchYawOnlyDecoder(32)); }

    // ---------- Category 2: length-then-skip ----------

    @Test public void cUtlBinaryBlock() {
        // Curated: random bytes produce negative array sizes; build a valid varU + body.
        var d = new CUtlBinaryBlockDecoder();
        for (var len : new int[]{0, 1, 7, 127, 200}) {
            var b = BitstreamBuilder.bitstream().addVarUInt(len);
            for (var i = 0; i < len; i++) b.add(0xAB, 8);
            assertParityCurated(b, d);
        }
    }

    // ---------- Category 3: string walking ----------

    @Test public void stringLen() {
        // Wire-format invariant: producer writes exactly wireLen bytes (no early-zero
        // termination). Random bytes hit embedded zeros and disagree with readString's
        // break-on-zero — but the skip path matches the wire contract, not readString's
        // truncation. Curate inputs without intermediate zeros.
        var d = new StringLenDecoder();
        for (var len : new int[]{0, 1, 5, 100, 500}) {
            var b = BitstreamBuilder.bitstream().add(len, 9);
            for (var i = 0; i < len; i++) b.add('a', 8); // no zero bytes
            assertParityCurated(b, d);
        }
    }

    // StringZeroTerminated under random bytes has high chance of hitting a zero
    // byte early, but skip and decode both walk the same way — parity holds.
    @Test public void stringZeroTerminated() { assertParity(new StringZeroTerminatedDecoder()); }

    // ---------- Category 4: conditional ----------

    @Test public void floatCoord()           { assertParity(new FloatCoordDecoder()); }
    @Test public void floatCoordMp()         { assertParity(new FloatCoordMpDecoder(false, false)); }
    @Test public void floatCoordMpInt()      { assertParity(new FloatCoordMpDecoder(true, false)); }
    @Test public void floatCoordMpLow()      { assertParity(new FloatCoordMpDecoder(false, true)); }
    @Test public void floatQuantized()       { assertParity(new FloatQuantizedDecoder("t", 12, 0, -100f, 100f)); }
    @Test public void floatQuantizedRoundDown() {
        // ROUNDDOWN flag → one conditional flag bit
        assertParity(new FloatQuantizedDecoder("t", 12, 0x1, -100f, 100f));
    }
    @Test public void floatQuantizedRoundUp() {
        assertParity(new FloatQuantizedDecoder("t", 12, 0x2, -100f, 100f));
    }
    @Test public void polymorphicPointer() {
        // Construct inputs covering all four tail-width regimes of readUBitVar
        // (a=0,1,2,3 → tail of 0/4/8/28 bits). decode accesses d.types[index], so
        // payload values are kept under 8.
        var types = new skadistats.clarity.model.s2.SerializerId[8];
        for (var i = 0; i < types.length; i++) {
            types[i] = new skadistats.clarity.model.s2.SerializerId("T" + i, 0);
        }
        var d = new PolymorphicPointerDecoder(types);
        // flag=0: null
        assertParityCurated(BitstreamBuilder.bitstream().add(0, 1), d);
        // a=0 (low 4 bits = index, no tail) — index 0..7
        assertParityCurated(BitstreamBuilder.bitstream().add(1, 1).add(0x05, 6), d);
        // a=1,2,3: tail of 4/8/28 bits respectively. Payload tail=0 means index = v & 15.
        for (var a : new int[]{1, 2, 3}) {
            var tail = new int[]{0, 4, 8, 28}[a];
            var header = (a << 4) | 0x3; // low 4 bits = 0x3 → index 3
            assertParityCurated(BitstreamBuilder.bitstream().add(1, 1).add(header, 6).add(0, tail), d);
        }
    }
    @Test public void qAngleNoBitCount()     { assertParity(new QAngleNoBitCountDecoder()); }
    @Test public void qAnglePrecise()        { assertParity(new QAnglePreciseDecoder()); }

    // ---------- Category 5: composite ----------

    @Test public void vector()               { assertParity(new VectorDecoder(new FloatNoScaleDecoder(), false)); }
    @Test public void vectorNormalReconstruct() {
        assertParity(new VectorDecoder(new FloatCoordDecoder(), true));
    }
    @Test public void vectorXY()             { assertParity(new VectorXYDecoder(new FloatNoScaleDecoder())); }
    @Test public void vectorNormal()         { assertParity(new VectorNormalDecoder()); }
    @Test public void vectorDefault4()       { assertParity(new VectorDefaultDecoder(4, new FloatNoScaleDecoder())); }
    @Test public void array() {
        assertParity(new ArrayDecoder(new IntUnsignedDecoder(4), 3));
    }

    // ---------- Category 6: varint walking ----------

    @Test public void intVarUnsigned()       { assertParity(new IntVarUnsignedDecoder()); }
    @Test public void intVarSigned()         { assertParity(new IntVarSignedDecoder()); }
    @Test public void intMinusOne()          { assertParity(new IntMinusOneDecoder()); }
    @Test public void longVarUnsigned()      { assertParity(new LongVarUnsignedDecoder()); }
    @Test public void longVarSigned()        { assertParity(new LongVarSignedDecoder()); }
}
