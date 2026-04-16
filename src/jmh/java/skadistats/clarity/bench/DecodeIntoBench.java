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
import org.openjdk.jmh.infra.Blackhole;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.decoder.DecoderDispatch;
import skadistats.clarity.io.decoder.FloatDefaultDecoder;
import skadistats.clarity.io.decoder.IntSignedDecoder;
import skadistats.clarity.io.decoder.LongSignedDecoder;
import skadistats.clarity.io.decoder.VectorDecoder;
import skadistats.clarity.io.decoder.FloatNoScaleDecoder;
import skadistats.clarity.model.state.PrimitiveType;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * CP-3 micro: compares decodeInto (direct byte[] write) vs decode + PrimitiveType.write
 * (boxing path). Run with -prof gc to observe allocation delta on the decodeInto path.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(value = 1, jvmArgsAppend = {"-Xmx2g"})
public class DecodeIntoBench {

    // Each parameter names a decoder kind with its primitive type and buffer size.
    @Param({"INT_SIGNED", "LONG_SIGNED", "FLOAT_DEFAULT", "VECTOR"})
    public String kind;

    private byte[] sourceBytes;
    private Decoder decoder;
    private PrimitiveType primitiveType;
    private int bufSize;

    private byte[] dst;
    private BitStream bs;

    private static final int ITERS = 256;

    @Setup(Level.Trial)
    public void setupTrial() {
        var r = new Random(0xC0FFEEL);
        sourceBytes = new byte[65536];
        r.nextBytes(sourceBytes);
        switch (kind) {
            case "INT_SIGNED" -> {
                decoder = new IntSignedDecoder(17);
                primitiveType = PrimitiveType.Scalar.INT;
                bufSize = 4;
            }
            case "LONG_SIGNED" -> {
                decoder = new LongSignedDecoder(48);
                primitiveType = PrimitiveType.Scalar.LONG;
                bufSize = 8;
            }
            case "FLOAT_DEFAULT" -> {
                decoder = new FloatDefaultDecoder(16, -1.0f, 1.0f);
                primitiveType = PrimitiveType.Scalar.FLOAT;
                bufSize = 4;
            }
            case "VECTOR" -> {
                decoder = new VectorDecoder(new FloatNoScaleDecoder(), false);
                primitiveType = new PrimitiveType.VectorType(PrimitiveType.Scalar.FLOAT, 3);
                bufSize = 12;
            }
            default -> throw new IllegalStateException(kind);
        }
        dst = new byte[bufSize];
    }

    @Setup(Level.Invocation)
    public void setupInvocation() {
        // Fresh BitStream per invocation, hoisted out of the timed region so the
        // bench measures only the decode loop's cost + allocation profile.
        bs = BitStream.createBitStream(ByteString.copyFrom(sourceBytes));
    }

    @Benchmark
    public void decodeInto(Blackhole bh) {
        var s = bs;
        for (var i = 0; i < ITERS; i++) {
            DecoderDispatch.decodeInto(s, decoder, dst, 0);
            bh.consume(dst);
        }
    }

    @Benchmark
    public void decodeThenWrite(Blackhole bh) {
        var s = bs;
        for (var i = 0; i < ITERS; i++) {
            var boxed = DecoderDispatch.decode(s, decoder);
            primitiveType.write(dst, 0, boxed);
            bh.consume(dst);
        }
    }
}
