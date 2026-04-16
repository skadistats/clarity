package skadistats.clarity.bench;

import skadistats.clarity.model.state.S1EntityStateType;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;

import java.nio.file.Files;
import java.nio.file.Path;

public class S1SmokeMain {

    @UsesEntities
    public static class EmptyProcessor {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: S1SmokeMain [--type=OBJECT_ARRAY|FLAT] <replay> [<replay> ...]");
            System.exit(2);
        }
        var stateType = S1EntityStateType.OBJECT_ARRAY;
        var startIdx = 0;
        if (args[0].startsWith("--type=")) {
            stateType = S1EntityStateType.valueOf(args[0].substring("--type=".length()));
            startIdx = 1;
        }
        for (var i = startIdx; i < args.length; i++) {
            var replay = Path.of(args[i]);
            if (!Files.isRegularFile(replay)) {
                System.err.println("replay not found: " + replay);
                System.exit(2);
            }
            var t0 = System.nanoTime();
            try (var src = new MappedFileSource(replay)) {
                var runner = new SimpleRunner(src);
                runner.withS1EntityState(stateType);
                runner.runWith(new EmptyProcessor());
            }
            var ms = (System.nanoTime() - t0) / 1_000_000;
            System.out.printf("OK %s [%s] parsed in %d ms%n", replay, stateType, ms);
        }
    }
}
