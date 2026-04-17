package skadistats.clarity.bench;

import org.openjdk.jmh.annotations.*;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.state.s1.S1EntityStateType;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, batchSize = 1)
@Measurement(iterations = 10, batchSize = 1)
@Fork(1)
public class S1EntityStateParseBench {

    @Param({"OBJECT_ARRAY", "FLAT"})
    public String impl;

    @Param({""})
    public String replay;

    @UsesEntities
    public static class EmptyProcessor {
    }

    @Benchmark
    public void parse() throws Exception {
        var type = S1EntityStateType.valueOf(impl);
        try (var src = new MappedFileSource(replay)) {
            var runner = new SimpleRunner(src);
            runner.withS1EntityState(type);
            runner.runWith(new EmptyProcessor());
        }
    }
}
