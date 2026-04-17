package skadistats.clarity.bench;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import skadistats.clarity.bench.trace.BirthMaterializer;
import skadistats.clarity.bench.trace.CapturedTrace;
import skadistats.clarity.bench.trace.MutationTraceCapture;
import skadistats.clarity.state.EntityState;
import skadistats.clarity.state.s2.S2EntityStateType;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Isolated copy() micro. Materializes all entity states from a captured trace at Trial
 * level, then measures the cost of one full snapshot pass (copy each state once) per
 * invocation.
 *
 * <p>Used as CP-0 baseline for accelerate-flat-entity-state: on current master the
 * FLAT copy path walks the FieldLayout tree via markSubEntriesNonModifiable for every
 * Entry. After CP-1 the copy becomes O(1) reference-sharing.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(value = 1, jvmArgsAppend = {"-Xmx16g"})
public class FlatCopyBench {

    @Param({"FLAT", "NESTED_ARRAY"})
    public String impl;

    @Param({""})
    public String replay;

    private EntityState[] states;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        var type = S2EntityStateType.valueOf(impl);
        CapturedTrace trace = MutationTraceCapture.capture(Path.of(replay));
        states = BirthMaterializer.materialize(trace, type);
    }

    @Benchmark
    public void copyAll(Blackhole bh) {
        var src = states;
        for (var s : src) {
            bh.consume(s.copy());
        }
    }
}
