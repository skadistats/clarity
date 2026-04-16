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

public class S1Main {

    private static final String[] DEFAULT_S1 = {
        "replays/dota/s1/normal/271145478.dem",
        "replays/csgo/s1/luminosity-vs-azio-cache.dem",
    };

    public static void main(String[] args) throws Exception {
        var replays = args.length == 0 ? DEFAULT_S1 : args;

        for (var r : replays) {
            if (!Files.isRegularFile(Path.of(r))) {
                System.err.println("replay not found: " + r);
                System.exit(2);
            }
        }

        var branchSha = ContextWriter.gitBranchSha();
        var timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"));
        var outDir = Path.of("bench-results", timestamp + "_s1_" + branchSha);
        Files.createDirectories(outDir);

        var jsonPath = outDir.resolve("results.json");
        var opts = new OptionsBuilder()
            .include(S1EntityStateParseBench.class.getSimpleName())
            .param("replay", replays)
            .jvmArgsAppend("-Xmx4g")
            .addProfiler(GCProfiler.class)
            .resultFormat(ResultFormatType.JSON)
            .result(jsonPath.toString())
            .build();

        System.out.println("Running S1 benchmark, results will be written to " + outDir.toAbsolutePath());
        Collection<RunResult> results = new Runner(opts).run();

        ContextWriter.write(replays, outDir.resolve("context.txt"));
        ReportWriter.write(results, outDir.resolve("results.txt"));

        System.out.println();
        System.out.println("Done. Artifacts in " + outDir.toAbsolutePath());
    }
}
