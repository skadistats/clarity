package skadistats.clarity.bench;

import org.openjdk.jmh.annotations.*;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.state.s2.S2EntityStateType;

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
