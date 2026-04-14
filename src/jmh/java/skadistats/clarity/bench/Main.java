package skadistats.clarity.bench;

import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

public class Main {

    private static final String[] DEFAULT_SINGLE = {
        "replays/dota/s2/340/8168882574_1198277651.dem",
    };

    private static final String[] DEFAULT_ALL = {
        "replays/dota/s2/340/8168882574_1198277651.dem",
        "replays/cs2/350/3dmax-vs-falcons-m1-anubis.dem",
        "replays/deadlock/newer/19206063.dem",
    };

    public static void main(String[] args) throws Exception {
        String[] replays;
        if (args.length == 0) {
            replays = DEFAULT_SINGLE;
        } else if (args.length == 1 && args[0].equals("--all")) {
            replays = DEFAULT_ALL;
        } else {
            replays = args;
        }

        for (var r : replays) {
            if (!Files.isRegularFile(Path.of(r))) {
                System.err.println("replay not found: " + r);
                System.exit(2);
            }
        }

        var branchSha = ContextWriter.gitBranchSha();
        var timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"));
        var outDir = Path.of("bench-results", timestamp + "_" + branchSha);
        Files.createDirectories(outDir);

        var jsonPath = outDir.resolve("results.json");
        var opts = new OptionsBuilder()
            .include(EntityStateParseBench.class.getSimpleName())
            .param("replay", replays)
            .jvmArgsAppend("-Xmx4g")
            .addProfiler(GCProfiler.class)
            .resultFormat(ResultFormatType.JSON)
            .result(jsonPath.toString())
            .build();

        System.out.println("Running benchmark, results will be written to " + outDir.toAbsolutePath());
        Collection<RunResult> results = new Runner(opts).run();

        ContextWriter.write(replays, outDir.resolve("context.txt"));
        ReportWriter.write(results, outDir.resolve("results.txt"));

        System.out.println();
        System.out.println("Done. Artifacts in " + outDir.toAbsolutePath());
        System.out.println("  results.json  raw JMH output");
        System.out.println("  results.txt   human-readable comparison");
        System.out.println("  context.txt   run environment + replay hashes");
    }
}
