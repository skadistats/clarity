package skadistats.clarity.bench;

import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FlatCopyMain {

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
        var outDir = Path.of("bench-results", "flatcopy_" + timestamp + "_" + branchSha);
        Files.createDirectories(outDir);

        var jsonPath = outDir.resolve("results.json");
        var textPath = outDir.resolve("results.txt");
        var opts = new OptionsBuilder()
            .include(FlatCopyBench.class.getSimpleName())
            .param("replay", replays)
            .jvmArgsAppend("-Xmx16g")
            .addProfiler(GCProfiler.class)
            .resultFormat(ResultFormatType.JSON)
            .result(jsonPath.toString())
            .output(textPath.toString())
            .build();

        System.out.println("Running FlatCopyBench, results will be written to " + outDir.toAbsolutePath());
        new Runner(opts).run();

        ContextWriter.write(replays, outDir.resolve("context.txt"));

        System.out.println();
        System.out.println("Done. Artifacts in " + outDir.toAbsolutePath());
        System.out.println("  results.json  raw JMH output");
        System.out.println("  results.txt   JMH console log");
        System.out.println("  context.txt   run environment + replay hashes");
    }
}
