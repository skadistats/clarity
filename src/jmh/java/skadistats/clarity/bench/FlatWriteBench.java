package skadistats.clarity.bench;

import com.google.protobuf.ByteString;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.decoder.DecoderDispatch;
import skadistats.clarity.io.decoder.FloatDefaultDecoder;
import skadistats.clarity.io.decoder.IntSignedDecoder;
import skadistats.clarity.io.decoder.VectorDecoder;
import skadistats.clarity.io.decoder.FloatNoScaleDecoder;
import skadistats.clarity.io.s2.Field;
import skadistats.clarity.io.s2.FieldType;
import skadistats.clarity.io.s2.Serializer;
import skadistats.clarity.io.s2.SerializerId;
import skadistats.clarity.io.s2.field.SerializerField;
import skadistats.clarity.io.s2.field.ValueField;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.model.s2.S2ModifiableFieldPath;
import skadistats.clarity.model.state.FieldLayoutBuilder;
import skadistats.clarity.model.state.FlatEntityState;
import skadistats.clarity.model.state.StateMutation;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * CP-4 micro: compares FlatEntityState primitive-leaf write via decodeInto (zero-box,
 * zero-StateMutation-alloc) vs applyMutation(WriteValue(decode)) (boxing + record alloc).
 * Run with -prof gc to observe the allocation delta on the decodeInto path.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(value = 1, jvmArgsAppend = {"-Xmx2g"})
public class FlatWriteBench {

    private static final int ITERS = 256;
    private static final FieldType TYPE = FieldType.forString("test");

    @Param({"INT_SIGNED", "FLOAT_DEFAULT", "VECTOR"})
    public String kind;

    private byte[] sourceBytes;
    private Decoder decoder;
    private FlatEntityState state;
    private S2FieldPath fp;
    private BitStream bs;

    @Setup(Level.Trial)
    public void setupTrial() {
        var r = new Random(0xC0FFEEL);
        sourceBytes = new byte[65536];
        r.nextBytes(sourceBytes);
        switch (kind) {
            case "INT_SIGNED" -> decoder = new IntSignedDecoder(17);
            case "FLOAT_DEFAULT" -> decoder = new FloatDefaultDecoder(16, -1.0f, 1.0f);
            case "VECTOR" -> decoder = new VectorDecoder(new FloatNoScaleDecoder(), false);
            default -> throw new IllegalStateException(kind);
        }

        var root = new Serializer(
            new SerializerId("S", 0),
            new Field[]{new ValueField(TYPE, decoder, null)},
            new String[]{"v"});
        var rf = new SerializerField(TYPE, root);
        var built = new FieldLayoutBuilder().buildSerializer(root);
        state = new FlatEntityState(rf, 4, built.layout(), built.totalBytes());

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
        // Fresh BitStream per invocation, hoisted out of the timed region.
        bs = BitStream.createBitStream(ByteString.copyFrom(sourceBytes));
    }

    @Benchmark
    public boolean decodeInto() {
        var s = bs;
        var st = state;
        var p = fp;
        var dec = decoder;
        var cap = false;
        for (var i = 0; i < ITERS; i++) {
            cap ^= st.decodeInto(p, dec, s);
        }
        return cap;
    }

    @Benchmark
    public boolean applyMutationWriteValue() {
        var s = bs;
        var st = state;
        var p = fp;
        var dec = decoder;
        var cap = false;
        for (var i = 0; i < ITERS; i++) {
            var decoded = DecoderDispatch.decode(s, dec);
            cap ^= st.applyMutation(p, new StateMutation.WriteValue(decoded));
        }
        return cap;
    }
}
