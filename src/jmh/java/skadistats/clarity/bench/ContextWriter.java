package skadistats.clarity.bench;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

public final class ContextWriter {

    private ContextWriter() {}

    public static void write(String[] replays, Path out) throws IOException {
        var sw = new StringWriter();
        var w = new PrintWriter(sw);

        w.println("timestamp:  " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        w.println("git HEAD:   " + gitHead());
        w.println("git branch: " + gitBranch());
        w.println("git status: " + gitStatus());
        w.println("JVM:        " + System.getProperty("java.vendor") + " "
                                 + System.getProperty("java.version") + " ("
                                 + System.getProperty("java.vm.name") + " "
                                 + System.getProperty("java.vm.version") + ")");
        w.println("OS:         " + System.getProperty("os.name") + " "
                                 + System.getProperty("os.version") + " ("
                                 + System.getProperty("os.arch") + ")");
        w.println("CPU:        " + cpuInfo());
        w.println("cores:      " + Runtime.getRuntime().availableProcessors());
        w.println("heap max:   " + Runtime.getRuntime().maxMemory() / (1024 * 1024) + " MiB");
        w.println("JVM args:   " + ManagementFactory.getRuntimeMXBean().getInputArguments());
        w.println();

        w.println("replays:");
        for (var r : replays) {
            var p = Path.of(r);
            w.printf("  %s%n", r);
            w.printf("    size: %.1f MiB%n", Files.size(p) / (1024.0 * 1024.0));
            w.printf("    sha1: %s%n", sha1(p));
        }

        w.flush();
        Files.writeString(out, sw.toString());
    }

    public static String gitBranchSha() {
        var branch = gitBranch().replaceAll("[^A-Za-z0-9._-]", "-");
        var sha = gitHead();
        var shortSha = sha.length() >= 7 ? sha.substring(0, 7) : sha;
        return branch + "-" + shortSha;
    }

    private static String gitHead() {
        return run("git", "rev-parse", "HEAD").trim();
    }

    private static String gitBranch() {
        var b = run("git", "rev-parse", "--abbrev-ref", "HEAD").trim();
        return b.isEmpty() ? "detached" : b;
    }

    private static String gitStatus() {
        var s = run("git", "status", "--porcelain").trim();
        return s.isEmpty() ? "clean" : "dirty";
    }

    private static String cpuInfo() {
        try {
            var lines = Files.readAllLines(Path.of("/proc/cpuinfo"));
            for (var line : lines) {
                if (line.startsWith("model name")) {
                    var i = line.indexOf(':');
                    if (i >= 0) return line.substring(i + 1).trim();
                }
            }
        } catch (IOException ignored) {
        }
        return "unknown";
    }

    private static String sha1(Path path) throws IOException {
        try {
            var md = MessageDigest.getInstance("SHA-1");
            try (var in = Files.newInputStream(path)) {
                var buf = new byte[1 << 16];
                int n;
                while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    private static String run(String... cmd) {
        try {
            var pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            var p = pb.start();
            var out = new String(p.getInputStream().readAllBytes());
            p.waitFor();
            return out;
        } catch (Exception e) {
            return "";
        }
    }
}
