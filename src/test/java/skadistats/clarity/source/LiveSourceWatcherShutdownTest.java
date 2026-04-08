package skadistats.clarity.source;

import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Regression tests for skadistats/clarity#260: the watcher thread spawned by
 * {@link LiveSource} must terminate when the source is closed or stopped, so
 * that the directory handle is released and sequential live parses do not
 * accumulate watcher threads.
 */
public class LiveSourceWatcherShutdownTest {

    private static final String WATCHER_THREAD_NAME = "clarity-livesource-watcher";

    @Test
    public void closeTerminatesWatcherThread() throws Exception {
        var dir = Files.createTempDirectory("clarity-livesource-test");
        var file = dir.resolve("nonexistent.dem");
        try {
            var source = new LiveSource(file, 5, TimeUnit.SECONDS);
            assertTrue(awaitWatcherStarted(), "watcher thread should have started");

            source.close();

            assertTrue(awaitWatcherGone(), "watcher thread should terminate after close()");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(dir);
        }
    }

    @Test
    public void stopTerminatesWatcherThread() throws Exception {
        var dir = Files.createTempDirectory("clarity-livesource-test");
        var file = dir.resolve("nonexistent.dem");
        try {
            var source = new LiveSource(file, 5, TimeUnit.SECONDS);
            assertTrue(awaitWatcherStarted(), "watcher thread should have started");

            source.stop();

            assertTrue(awaitWatcherGone(), "watcher thread should terminate after stop()");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(dir);
        }
    }

    @Test
    public void sequentialLiveSourcesDoNotAccumulateWatcherThreads() throws Exception {
        var dir = Files.createTempDirectory("clarity-livesource-test");
        try {
            for (var i = 0; i < 5; i++) {
                var file = dir.resolve("nonexistent-" + i + ".dem");
                var source = new LiveSource(file, 5, TimeUnit.SECONDS);
                assertTrue(awaitWatcherStarted(), "watcher thread should have started");
                source.close();
                assertTrue(awaitWatcherGone(), "watcher thread should terminate after close()");
            }
            assertFalse(watcherThreadExists(), "no watcher threads should remain");
        } finally {
            Files.walk(dir).sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (Exception ignored) {}
            });
        }
    }

    private static boolean watcherThreadExists() {
        return Thread.getAllStackTraces().keySet().stream()
                .anyMatch(t -> WATCHER_THREAD_NAME.equals(t.getName()) && t.isAlive());
    }

    private static boolean awaitWatcherStarted() throws InterruptedException {
        return awaitCondition(LiveSourceWatcherShutdownTest::watcherThreadExists, 2000);
    }

    private static boolean awaitWatcherGone() throws InterruptedException {
        return awaitCondition(() -> !watcherThreadExists(), 5000);
    }

    private static boolean awaitCondition(java.util.function.BooleanSupplier cond, long timeoutMs) throws InterruptedException {
        var deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (cond.getAsBoolean()) return true;
            Thread.sleep(25);
        }
        return cond.getAsBoolean();
    }
}
