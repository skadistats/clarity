package skadistats.clarity.bench;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import skadistats.clarity.model.state.S2EntityStateType;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, batchSize = 1)
@Measurement(iterations = 10, batchSize = 1)
@Fork(1)
public class EntityStateParseBench {

    @Param({"NESTED_ARRAY", "TREE_MAP", "FLAT"})
    public String impl;

    @Param({""})
    public String replay;

    @UsesEntities
    public static class EmptyProcessor {
    }

    @Benchmark
    public void parse() throws Exception {
        var type = S2EntityStateType.valueOf(impl);
        try (var src = new MappedFileSource(replay)) {
            var runner = new SimpleRunner(src);
            runner.withS2EntityState(type);
            runner.runWith(new EmptyProcessor());
        }
    }
}
