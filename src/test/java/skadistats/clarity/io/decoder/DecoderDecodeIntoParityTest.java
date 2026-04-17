package skadistats.clarity.io.decoder;

import com.google.protobuf.ByteString;
import org.testng.annotations.Test;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.Vector;
import skadistats.clarity.state.PrimitiveType;

import java.util.Arrays;
import java.util.Random;

import static org.testng.Assert.assertEquals;

/**
 * CP-3 parity tests: for each decoder with {@code decodeInto}, verify that
 * {@code decodeInto(bs, d, data, 0)} produces byte-for-byte the same result as
 * {@code PrimitiveType.write(data, 0, decode(bs))} against an identical bitstream.
 */
public class DecoderDecodeIntoParityTest {

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

    private static void assertParity(Decoder d, PrimitiveType t, int bufSize) {
        var bytes = randomBytes(256);

        var bs1 = freshStream(bytes);
        var bs2 = freshStream(bytes);

        var bufDecodeInto = new byte[bufSize];
        DecoderDispatch.decodeInto(bs1, d, bufDecodeInto, 0);

        var boxed = DecoderDispatch.decode(bs2, d);
        var bufWrite = new byte[bufSize];
        t.write(bufWrite, 0, boxed);

        assertEquals(bs1.pos(), bs2.pos(), "both paths consume identical bits");
        assertEquals(bufDecodeInto, bufWrite,
            "decodeInto produces byte-identical result to decode + PrimitiveType.write; "
                + "diff=" + Arrays.toString(bufDecodeInto) + " vs " + Arrays.toString(bufWrite));
    }

    // ---------- Scalars ----------

    @Test public void intSigned()      { assertParity(new IntSignedDecoder(17), PrimitiveType.Scalar.INT, 4); }
    @Test public void intUnsigned()    { assertParity(new IntUnsignedDecoder(24), PrimitiveType.Scalar.INT, 4); }
    @Test public void intMinusOne()    { assertParity(new IntMinusOneDecoder(), PrimitiveType.Scalar.INT, 4); }
    @Test public void intVarSigned()   { assertParity(new IntVarSignedDecoder(), PrimitiveType.Scalar.INT, 4); }
    @Test public void intVarUnsigned() { assertParity(new IntVarUnsignedDecoder(), PrimitiveType.Scalar.INT, 4); }

    @Test public void longSigned()       { assertParity(new LongSignedDecoder(60), PrimitiveType.Scalar.LONG, 8); }
    @Test public void longUnsigned()     { assertParity(new LongUnsignedDecoder(48), PrimitiveType.Scalar.LONG, 8); }
    @Test public void longVarSigned()    { assertParity(new LongVarSignedDecoder(), PrimitiveType.Scalar.LONG, 8); }
    @Test public void longVarUnsigned()  { assertParity(new LongVarUnsignedDecoder(), PrimitiveType.Scalar.LONG, 8); }

    @Test public void bool()             { assertParity(new BoolDecoder(), PrimitiveType.Scalar.BOOL, 1); }

    @Test public void floatNoScale()    { assertParity(new FloatNoScaleDecoder(), PrimitiveType.Scalar.FLOAT, 4); }
    @Test public void floatCoord()      { assertParity(new FloatCoordDecoder(), PrimitiveType.Scalar.FLOAT, 4); }
    @Test public void floatCoordMp()    { assertParity(new FloatCoordMpDecoder(false, false), PrimitiveType.Scalar.FLOAT, 4); }
    @Test public void floatCellCoord()  { assertParity(new FloatCellCoordDecoder(8, false, false), PrimitiveType.Scalar.FLOAT, 4); }
    @Test public void floatNormal()     { assertParity(new FloatNormalDecoder(), PrimitiveType.Scalar.FLOAT, 4); }
    @Test public void floatDefault()    { assertParity(new FloatDefaultDecoder(16, -1.0f, 1.0f), PrimitiveType.Scalar.FLOAT, 4); }
    @Test public void floatQuantized()  { assertParity(new FloatQuantizedDecoder("test", 12, 0, -100.0f, 100.0f), PrimitiveType.Scalar.FLOAT, 4); }

    // ---------- Compounds ----------

    @Test public void vector3()         { assertParity(new VectorDecoder(new FloatNoScaleDecoder(), false), new PrimitiveType.VectorType(PrimitiveType.Scalar.FLOAT, 3), 12); }
    @Test public void vectorNormalReconstruct() { assertParity(new VectorDecoder(new FloatCoordDecoder(), true), new PrimitiveType.VectorType(PrimitiveType.Scalar.FLOAT, 3), 12); }
    @Test public void vectorXY()        { assertParity(new VectorXYDecoder(new FloatNoScaleDecoder()), new PrimitiveType.VectorType(PrimitiveType.Scalar.FLOAT, 2), 8); }
    @Test public void vectorNormal()    { assertParity(new VectorNormalDecoder(), new PrimitiveType.VectorType(PrimitiveType.Scalar.FLOAT, 3), 12); }
    @Test public void vectorDefault4()  { assertParity(new VectorDefaultDecoder(4, new FloatNoScaleDecoder()), new PrimitiveType.VectorType(PrimitiveType.Scalar.FLOAT, 4), 16); }

    @Test public void qAngleBitCount()   { assertParity(new QAngleBitCountDecoder(8), new PrimitiveType.VectorType(PrimitiveType.Scalar.FLOAT, 3), 12); }
    @Test public void qAngleNoBitCount() { assertParity(new QAngleNoBitCountDecoder(), new PrimitiveType.VectorType(PrimitiveType.Scalar.FLOAT, 3), 12); }
    @Test public void qAngleNoScale()    { assertParity(new QAngleNoScaleDecoder(), new PrimitiveType.VectorType(PrimitiveType.Scalar.FLOAT, 3), 12); }
    @Test public void qAnglePitchYaw()   { assertParity(new QAnglePitchYawOnlyDecoder(12), new PrimitiveType.VectorType(PrimitiveType.Scalar.FLOAT, 3), 12); }
    @Test public void qAnglePrecise()    { assertParity(new QAnglePreciseDecoder(), new PrimitiveType.VectorType(PrimitiveType.Scalar.FLOAT, 3), 12); }

    // ---------- Boxed result identity check (sanity — catches decode returning Vector that doesn't match byte[] path) ----------

    @Test
    public void vectorDecodedIsRoundtripEqual() {
        var d = new VectorDecoder(new FloatNoScaleDecoder(), false);
        var bytes = randomBytes(256);
        var bs = freshStream(bytes);
        var decoded = (Vector) DecoderDispatch.decode(bs, d);

        // Write the decoded Vector to byte[] via PrimitiveType
        var buf = new byte[12];
        new PrimitiveType.VectorType(PrimitiveType.Scalar.FLOAT, 3).write(buf, 0, decoded);

        // decodeInto produces the same bytes
        var buf2 = new byte[12];
        DecoderDispatch.decodeInto(freshStream(bytes), d, buf2, 0);
        assertEquals(buf2, buf);
    }
}
