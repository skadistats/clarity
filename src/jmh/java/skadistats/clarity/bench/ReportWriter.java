package skadistats.clarity.bench;

import org.openjdk.jmh.results.RunResult;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ReportWriter {

    private static final String BASELINE = "NESTED_ARRAY";
    private static final String[] IMPL_ORDER = {"NESTED_ARRAY", "TREE_MAP", "FLAT"};

    private ReportWriter() {}

    public static void write(Collection<RunResult> results, Path out) throws IOException {
        var sw = new StringWriter();
        var w = new PrintWriter(sw);

        w.println("EntityStateParseBench");
        w.println("generated: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        w.println();

        var byReplay = groupByReplay(results);

        // Per-replay tables
        for (var e : byReplay.entrySet()) {
            writeTable(w, e.getKey(), e.getValue());
            w.println();
        }

        // Memory / GC pressure
        writeGc(w, byReplay);
        w.println();

        // Per-engine winner + verdict
        writeWinners(w, byReplay);
        w.println();

        // Cross-replay summary
        writeSummary(w, byReplay);

        w.flush();
        Files.writeString(out, sw.toString());
        System.out.println();
        System.out.println(sw);
    }

    private static Map<String, List<RunResult>> groupByReplay(Collection<RunResult> results) {
        var map = new LinkedHashMap<String, List<RunResult>>();
        var ordered = new ArrayList<>(results);
        ordered.sort(Comparator
            .comparing((RunResult r) -> r.getParams().getParam("replay"))
            .thenComparingInt(r -> implIndex(r.getParams().getParam("impl"))));
        for (var r : ordered) {
            map.computeIfAbsent(r.getParams().getParam("replay"), k -> new ArrayList<>()).add(r);
        }
        return map;
    }

    private static int implIndex(String impl) {
        for (var i = 0; i < IMPL_ORDER.length; i++) if (IMPL_ORDER[i].equals(impl)) return i;
        return Integer.MAX_VALUE;
    }

    private static void writeTable(PrintWriter w, String replay, List<RunResult> rows) {
        w.println("=== " + replay + " ===");
        w.printf("  %-14s %9s %9s %9s %9s %18s %9s%n",
            "impl", "median", "min", "max", "p95", "score ± err (ms)", "Δ vs " + BASELINE);

        Double baselineScore = null;
        for (var r : rows) {
            if (BASELINE.equals(r.getParams().getParam("impl"))) {
                baselineScore = r.getPrimaryResult().getScore();
                break;
            }
        }

        var bestImpl = winner(rows);
        for (var r : rows) {
            var stats = r.getPrimaryResult().getStatistics();
            var score = r.getPrimaryResult().getScore();
            var err = r.getPrimaryResult().getScoreError();
            var impl = r.getParams().getParam("impl");
            var delta = (baselineScore == null || BASELINE.equals(impl))
                ? "—"
                : String.format("%+.1f%%", (score - baselineScore) / baselineScore * 100.0);
            var marker = impl.equals(bestImpl) ? "*" : " ";
            w.printf("  %s %-14s %7.1f ms %6.1f ms %6.1f ms %6.1f ms %8.1f ± %-7.1f %9s%n",
                marker,
                impl,
                stats.getPercentile(50),
                stats.getMin(),
                stats.getMax(),
                stats.getPercentile(95),
                score, err,
                delta);
        }
    }

    private static String winner(List<RunResult> rows) {
        String best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        for (var r : rows) {
            var s = r.getPrimaryResult().getScore();
            if (s < bestScore) {
                bestScore = s;
                best = r.getParams().getParam("impl");
            }
        }
        return best;
    }

    private static String engineOf(String replayPath) {
        // replays/<engine>/... → engine folder; map cs2/csgo/deadlock/dota/s2 sensibly
        var parts = replayPath.replace('\\', '/').split("/");
        if (parts.length < 2) return replayPath;
        var head = parts[0].equalsIgnoreCase("replays") && parts.length >= 3 ? parts[1] : parts[0];
        // disambiguate s1 vs s2 for dota (we only bench s2, but keep safe)
        if (head.equalsIgnoreCase("dota") && parts.length >= 3) {
            var sub = parts[2];
            if (sub.equalsIgnoreCase("s1") || sub.equalsIgnoreCase("s2")) head = "dota/" + sub;
        }
        return head;
    }

    private static Double sec(RunResult r, String key) {
        var map = r.getSecondaryResults();
        var sr = map.get(key);
        if (sr == null) sr = map.get("·" + key);
        return sr == null ? null : sr.getScore();
    }

    private static String fmtBytes(Double b) {
        if (b == null) return "?";
        if (b >= 1024.0 * 1024 * 1024) return String.format("%.2f GB", b / (1024.0 * 1024 * 1024));
        if (b >= 1024.0 * 1024) return String.format("%.1f MB", b / (1024.0 * 1024));
        if (b >= 1024.0) return String.format("%.1f KB", b / 1024.0);
        return String.format("%.0f B", b);
    }

    private static void writeGc(PrintWriter w, Map<String, List<RunResult>> byReplay) {
        w.println("=== Memory / GC pressure ===");
        w.printf("  %-50s %-14s %10s %10s %8s %8s%n",
            "replay", "impl", "alloc/op", "alloc rate", "GCs", "GC time");
        for (var e : byReplay.entrySet()) {
            var replay = e.getKey();
            for (var r : e.getValue()) {
                var impl = r.getParams().getParam("impl");
                var allocNorm = sec(r, "gc.alloc.rate.norm");          // bytes/op
                var allocRate = sec(r, "gc.alloc.rate");                // MB/sec
                var gcCount = sec(r, "gc.count");                       // count
                var gcTime = sec(r, "gc.time");                         // ms
                w.printf("  %-50s %-14s %10s %8.0f MB/s %8.0f %7.0f ms%n",
                    shorten(replay, 50),
                    impl,
                    fmtBytes(allocNorm),
                    allocRate == null ? 0.0 : allocRate,
                    gcCount == null ? 0.0 : gcCount,
                    gcTime == null ? 0.0 : gcTime);
            }
        }
    }

    private static String shorten(String s, int max) {
        return s.length() <= max ? s : "…" + s.substring(s.length() - (max - 1));
    }

    private static void writeWinners(PrintWriter w, Map<String, List<RunResult>> byReplay) {
        w.println("=== Per-engine winners ===");
        w.printf("  %-10s %-14s %s%n", "engine", "best impl", "Δ vs " + BASELINE);

        var winners = new LinkedHashMap<String, String>();
        for (var e : byReplay.entrySet()) {
            var engine = engineOf(e.getKey());
            var rows = e.getValue();
            var best = winner(rows);
            Double baseScore = null;
            Double bestScore = null;
            for (var r : rows) {
                var i = r.getParams().getParam("impl");
                if (BASELINE.equals(i)) baseScore = r.getPrimaryResult().getScore();
                if (best.equals(i)) bestScore = r.getPrimaryResult().getScore();
            }
            var delta = (baseScore == null || bestScore == null || BASELINE.equals(best))
                ? "(baseline)"
                : String.format("%+.1f%%", (bestScore - baseScore) / baseScore * 100.0);
            w.printf("  %-10s %-14s %s%n", engine, best, delta);
            winners.put(engine, best);
        }

        var distinct = winners.values().stream().distinct().count();
        w.println();
        if (distinct <= 1 && !winners.isEmpty()) {
            w.println("  Verdict: all engines agree — " + winners.values().iterator().next() + " is fastest.");
        } else {
            w.println("  Verdict: winners differ across engines — impl choice is engine-dependent.");
        }
    }

    private static void writeSummary(PrintWriter w, Map<String, List<RunResult>> byReplay) {
        w.println("=== Summary across replays ===");
        w.printf("  %-14s %12s %12s %s%n", "impl", "mean Δ", "worst Δ", "replays faster / equal / slower vs " + BASELINE);

        for (var impl : IMPL_ORDER) {
            if (impl.equals(BASELINE)) continue;
            var deltas = new ArrayList<Double>();
            var faster = 0;
            var slower = 0;
            for (var rows : byReplay.values()) {
                Double base = null;
                Double other = null;
                for (var r : rows) {
                    var i = r.getParams().getParam("impl");
                    if (BASELINE.equals(i)) base = r.getPrimaryResult().getScore();
                    if (impl.equals(i)) other = r.getPrimaryResult().getScore();
                }
                if (base == null || other == null) continue;
                var d = (other - base) / base * 100.0;
                deltas.add(d);
                if (d < 0) faster++;
                else if (d > 0) slower++;
            }
            if (deltas.isEmpty()) continue;
            var mean = deltas.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            var worst = deltas.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            w.printf("  %-14s %+11.1f%% %+11.1f%% %d / %d / %d%n",
                impl, mean, worst, faster, deltas.size() - faster - slower, slower);
        }
    }
}
