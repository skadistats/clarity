package skadistats.clarity.state;

import com.google.protobuf.ByteString;
import org.testng.annotations.Test;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.decoder.StringLenDecoder;
import skadistats.clarity.model.s2.Serializer;
import skadistats.clarity.state.s2.S2FlatEntityState;

import java.nio.charset.StandardCharsets;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertThrows;
import static skadistats.clarity.state.TestFields.fp;
import static skadistats.clarity.state.TestFields.named;
import static skadistats.clarity.state.TestFields.rootField;
import static skadistats.clarity.state.TestFields.serializer;
import static skadistats.clarity.state.TestFields.stringField;

/**
 * CP-5 tests: verify inline-string leaf decodeInto, write, getValueForFieldPath,
 * and copy-on-write behavior.
 */
public class S2FlatEntityStateInlineStringTest {

    private static S2FlatEntityState makeFlat(Serializer root) {
        var rf = rootField(root);
        var built = new FieldLayoutBuilder().buildSerializer(rf.getSerializer());
        return new S2FlatEntityState(rf, 1024, built.layout(), built.totalBytes());
    }

    /** Builds a BitStream encoding a single StringLenDecoder-formatted string. */
    private static BitStream encodedStringStream(String s) {
        var bytes = s.getBytes(StandardCharsets.UTF_8);
        // 9-bit length prefix + bytes, little-endian bit order via readUBitInt.
        var totalBits = 9 + bytes.length * 8;
        var buf = new byte[(totalBits + 7) / 8 + 32];
        writeBits(buf, 0, bytes.length, 9);
        for (var i = 0; i < bytes.length; i++) {
            writeBits(buf, 9 + i * 8, bytes[i] & 0xFF, 8);
        }
        return BitStream.createBitStream(ByteString.copyFrom(buf));
    }

    private static void writeBits(byte[] buf, int bitPos, int value, int nBits) {
        for (var i = 0; i < nBits; i++) {
            var bit = (value >>> i) & 1;
            var byteIdx = (bitPos + i) >>> 3;
            var bitIdx = (bitPos + i) & 7;
            buf[byteIdx] |= (byte) (bit << bitIdx);
        }
    }

    // ---------- 5.8: inline-string roundtrip ----------

    @Test
    public void inlineStringRoundtripViaWrite() {
        var ser = serializer("S", named("s", stringField()));
        var st = makeFlat(ser);

        st.write(fp(0), "hello, clarity");
        assertEquals(st.getValueForFieldPath(fp(0)), "hello, clarity");

        // Verify byte-level layout: flag byte at offset 0, 2-byte length prefix, bytes.
        var root = st.rootDataForTest();
        var bytes = "hello, clarity".getBytes(StandardCharsets.UTF_8);
        assertEquals(root[0], (byte) 1, "flag byte set after write");
        var len = (root[1] & 0xFF) | ((root[2] & 0xFF) << 8);
        assertEquals(len, bytes.length, "length prefix matches UTF-8 byte count");
        for (var i = 0; i < bytes.length; i++) {
            assertEquals(root[3 + i], bytes[i], "byte " + i + " of inline payload");
        }
    }

    @Test
    public void inlineStringRoundtripViaDecodeInto() {
        var ser = serializer("S", named("s", stringField()));
        var st = makeFlat(ser);

        var decoder = new StringLenDecoder();
        var bs = encodedStringStream("hello, clarity");
        st.decodeInto(fp(0), decoder, bs);

        assertEquals(st.getValueForFieldPath(fp(0)), "hello, clarity",
            "decodeInto writes inline; getValueForFieldPath reads back the same String");

        var root = st.rootDataForTest();
        var bytes = "hello, clarity".getBytes(StandardCharsets.UTF_8);
        assertEquals(root[0], (byte) 1, "flag byte set");
        var len = (root[1] & 0xFF) | ((root[2] & 0xFF) << 8);
        assertEquals(len, bytes.length, "length prefix from decodeInto matches");
    }

    @Test
    public void inlineStringWriteThenClear() {
        var ser = serializer("S", named("s", stringField()));
        var st = makeFlat(ser);

        st.write(fp(0), "set");
        assertEquals(st.getValueForFieldPath(fp(0)), "set");

        st.write(fp(0), null);
        assertNull(st.getValueForFieldPath(fp(0)), "null write clears flag byte");
    }

    @Test
    public void inlineStringEmptyWriteReadsBackEmpty() {
        var ser = serializer("S", named("s", stringField()));
        var st = makeFlat(ser);

        st.write(fp(0), "");
        assertEquals(st.getValueForFieldPath(fp(0)), "",
            "empty string distinguishable from null via flag byte");
    }

    // ---------- 5.9: schema-violation on over-length write ----------

    @Test
    public void writeExceedingMaxLengthThrows() {
        var ser = serializer("S", named("s", stringField()));
        var st = makeFlat(ser);
        // stringField() uses the "test" FieldType which maps to the unbounded
        // 512-byte reservation. Writing a larger string trips the schema-violation guard.
        var tooLong = "x".repeat(FieldLayoutBuilder.UNBOUNDED_STRING_MAX_LENGTH + 1);
        assertThrows(IllegalStateException.class, () -> st.write(fp(0), tooLong));
    }

    // ---------- inline-string write after copy is independent ----------

    @Test
    public void inlineStringWriteAfterCopyIsIndependent() {
        var ser = serializer("S", named("s", stringField()));
        var st = makeFlat(ser);
        st.write(fp(0), "original");

        var cp = (S2FlatEntityState) st.copy();
        cp.write(fp(0), "replaced");

        assertEquals(st.getValueForFieldPath(fp(0)), "original");
        assertEquals(cp.getValueForFieldPath(fp(0)), "replaced");
    }
}
