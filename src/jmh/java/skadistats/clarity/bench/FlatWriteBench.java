package skadistats.clarity.bench;

import com.google.protobuf.ByteString;
import org.openjdk.jmh.annotations.*;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.decoder.DecoderDispatch;
import skadistats.clarity.io.decoder.FloatDefaultDecoder;
import skadistats.clarity.io.decoder.FloatNoScaleDecoder;
import skadistats.clarity.io.decoder.IntSignedDecoder;
import skadistats.clarity.io.decoder.VectorDecoder;
import skadistats.clarity.model.s2.Field;
import skadistats.clarity.model.s2.FieldType;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.model.s2.Serializer;
import skadistats.clarity.model.s2.SerializerId;
import skadistats.clarity.model.s2.field.SerializerField;
import skadistats.clarity.model.s2.field.ValueField;
import skadistats.clarity.state.StateMutation;
import skadistats.clarity.state.s2.FieldLayoutBuilder;
import skadistats.clarity.state.s2.S2FlatEntityState;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * CP-4 micro: compares S2FlatEntityState primitive-leaf write via decodeInto (zero-box,
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
    private S2FlatEntityState state;
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
        state = new S2FlatEntityState(rf, 4, built.layout(), built.totalBytes());

        fp = S2FieldPath.of(0);

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
