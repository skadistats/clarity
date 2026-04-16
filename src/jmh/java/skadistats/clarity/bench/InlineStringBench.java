package skadistats.clarity.bench;

import com.google.protobuf.ByteString;
import org.openjdk.jmh.annotations.*;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.decoder.DecoderDispatch;
import skadistats.clarity.io.decoder.StringLenDecoder;
import skadistats.clarity.io.s2.Field;
import skadistats.clarity.io.s2.FieldType;
import skadistats.clarity.io.s2.Serializer;
import skadistats.clarity.io.s2.SerializerId;
import skadistats.clarity.io.s2.field.SerializerField;
import skadistats.clarity.io.s2.field.ValueField;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.model.s2.S2ModifiableFieldPath;
import skadistats.clarity.model.state.FieldLayoutBuilder;
import skadistats.clarity.model.state.S2FlatEntityState;
import skadistats.clarity.model.state.StateMutation;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * CP-5 micro: compares inline-string decode+read roundtrip via {@code decodeInto}
 * (zero-allocation on decode; one {@code String} allocation on read) against the
 * pre-change refs-slab path that allocates a cached {@code String} on decode and
 * returns it by reference on read.
 * <p>
 * Run with {@code -prof gc} to observe the allocation delta. The inline path
 * trades "zero allocation on decode, one allocation per read" for the refs path's
 * "one allocation on decode, zero on read" — decode-heavy workloads win.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(value = 1, jvmArgsAppend = {"-Xmx2g"})
public class InlineStringBench {

    private static final int ITERS = 256;
    private static final FieldType TYPE = FieldType.forString("test");

    /**
     * A representative S2 string. Length chosen to match real dota props like
     * m_iszPlayerName (observed 10–20 bytes typical).
     */
    private static final String SAMPLE = "stringvalue1";

    private byte[] sourceBytes;
    private StringLenDecoder decoder;
    private S2FlatEntityState state;
    private S2FieldPath fp;
    private BitStream bs;

    @Setup(Level.Trial)
    public void setupTrial() {
        decoder = new StringLenDecoder();

        // Build a stream that contains ITERS repetitions of the same 9-bit-length-prefixed
        // UTF-8 string, concatenated bit-tightly. Same format the live wire emits.
        var bytes = SAMPLE.getBytes(StandardCharsets.UTF_8);
        var perSampleBits = 9 + bytes.length * 8;
        var totalBits = perSampleBits * ITERS + 32;
        sourceBytes = new byte[(totalBits + 7) / 8];

        var bitPos = 0;
        for (var i = 0; i < ITERS; i++) {
            writeBits(sourceBytes, bitPos, bytes.length, 9);
            bitPos += 9;
            for (var b : bytes) {
                writeBits(sourceBytes, bitPos, b & 0xFF, 8);
                bitPos += 8;
            }
        }

        var root = new Serializer(
            new SerializerId("S", 0),
            new Field[]{new ValueField(TYPE, decoder, null)},
            new String[]{"s"});
        var rf = new SerializerField(TYPE, root);
        var built = new FieldLayoutBuilder().buildSerializer(root);
        state = new S2FlatEntityState(rf, 4, built.layout(), built.totalBytes());

        var mfp = S2ModifiableFieldPath.newInstance();
        mfp.set(0, 0);
        fp = mfp.unmodifiable();

        // Pre-warm: first write trips capacity-change; subsequent writes hit the
        // steady-state path we want to measure.
        var pre = BitStream.createBitStream(ByteString.copyFrom(sourceBytes));
        state.decodeInto(fp, decoder, pre);
    }

    @Setup(Level.Invocation)
    public void setupInvocation() {
        bs = BitStream.createBitStream(ByteString.copyFrom(sourceBytes));
    }

    /**
     * Inline path: zero-alloc decodeInto writes bytes directly into the leaf's
     * reserved span. Each iteration's read allocates one String.
     */
    @Benchmark
    public int decodeIntoThenRead() {
        var s = bs;
        var st = state;
        var p = fp;
        var dec = decoder;
        var acc = 0;
        for (var i = 0; i < ITERS; i++) {
            st.decodeInto(p, dec, s);
            var v = (String) st.getValueForFieldPath(p);
            acc += v.length();
        }
        return acc;
    }

    /**
     * Legacy path: decode allocates a cached interned String; applyMutation wraps
     * in a StateMutation.WriteValue record and stores the reference in the refs
     * slab; read returns the cached reference (no new String per read).
     */
    @Benchmark
    public int decodeAndWriteThenRead() {
        var s = bs;
        var st = state;
        var p = fp;
        var dec = decoder;
        var acc = 0;
        for (var i = 0; i < ITERS; i++) {
            var decoded = DecoderDispatch.decode(s, dec);
            st.applyMutation(p, new StateMutation.WriteValue(decoded));
            var v = (String) st.getValueForFieldPath(p);
            acc += v.length();
        }
        return acc;
    }

    private static void writeBits(byte[] buf, int bitPos, int value, int nBits) {
        for (var i = 0; i < nBits; i++) {
            var bit = (value >>> i) & 1;
            var byteIdx = (bitPos + i) >>> 3;
            var bitIdx = (bitPos + i) & 7;
            buf[byteIdx] |= (byte) (bit << bitIdx);
        }
    }
}
