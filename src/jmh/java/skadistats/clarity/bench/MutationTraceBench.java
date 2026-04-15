package skadistats.clarity.bench;

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
import skadistats.clarity.bench.trace.BirthMaterializer;
import skadistats.clarity.bench.trace.CapturedTrace;
import skadistats.clarity.bench.trace.Mutation;
import skadistats.clarity.bench.trace.MutationTraceCapture;
import skadistats.clarity.model.state.EntityState;
import skadistats.clarity.model.state.S2EntityStateType;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, batchSize = 1)
@Measurement(iterations = 10, batchSize = 1)
@Fork(value = 1, jvmArgsAppend = {"-Xmx16g"})
public class MutationTraceBench {

    @Param({"NESTED_ARRAY", "TREE_MAP", "FLAT"})
    public String impl;

    @Param({""})
    public String replay;

    private CapturedTrace trace;
    private S2EntityStateType implType;

    private EntityState[] states;
    private Mutation[] updateMutations;

    @Setup(Level.Trial)
    public void setupTrial() throws Exception {
        implType = S2EntityStateType.valueOf(impl);
        trace = MutationTraceCapture.capture(Path.of(replay));
    }

    @Setup(Level.Invocation)
    public void setupInvocation() {
        states = BirthMaterializer.materialize(trace, implType);
        updateMutations = trace.updateMutations();
        // Pre-touch the array — read each element so it (and the records it points at)
        // are tenured before the measured loop, keeping young-gen GC out of the window.
        long sink = 0;
        for (var m : updateMutations) {
            sink += m.stateId();
        }
        if (sink == Long.MIN_VALUE) System.out.println(sink);
    }

    @Benchmark
    public void replay(Blackhole bh) {
        var st = states;
        var muts = updateMutations;
        boolean acc = false;
        for (var m : muts) {
            acc ^= st[m.stateId()].applyMutation(m.fp(), m.mutation());
        }
        bh.consume(acc);
    }
}
