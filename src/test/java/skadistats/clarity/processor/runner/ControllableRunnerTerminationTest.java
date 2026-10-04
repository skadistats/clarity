package skadistats.clarity.processor.runner;

import org.testng.SkipException;
import org.testng.annotations.Test;
import skadistats.clarity.processor.reader.OnTickStart;
import skadistats.clarity.source.MappedFileSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.expectThrows;

public class ControllableRunnerTerminationTest {

    private static final String REPLAY_DIR =
            System.getProperty("clarity.test.replayDir", "/home/spheenik/projects/replays");
    private static final String REPLAY = "dota/s2/normal/1560289528.dem";

    private static Path replay() {
        var replay = Paths.get(REPLAY_DIR, REPLAY);
        if (!Files.isReadable(replay)) {
            throw new SkipException("replay not available: " + replay);
        }
        return replay;
    }

    public static class Crasher {
        static final RuntimeException FAILURE = new RuntimeException("boom");
        final int crashAt;

        Crasher(int crashAt) {
            this.crashAt = crashAt;
        }

        @OnTickStart
        public void onTickStart(Context ctx, boolean synthetic) {
            if (ctx.getTick() == crashAt) throw FAILURE;
        }
    }

    public static class Sleeper {
        @OnTickStart
        public void onTickStart(Context ctx, boolean synthetic) throws InterruptedException {
            if (ctx.getTick() == 2) Thread.sleep(60_000);
        }
    }

    @Test(timeOut = 60_000)
    public void seekAndTickWork() throws Exception {
        try (var source = new MappedFileSource(replay().toString())) {
            var runner = new ControllableRunner(source).runWith(new Object());
            runner.seek(1000);
            assertEquals(runner.getTick(), 1000);
            runner.tick();
            assertEquals(runner.getTick(), 1001);
            runner.seek(10);
            assertEquals(runner.getTick(), 10);
            runner.halt();
            runner.join();
        }
    }

    @Test(timeOut = 60_000)
    public void crashWakesWaiterAndLaterCallsThrow() throws Exception {
        try (var source = new MappedFileSource(replay().toString())) {
            var runner = new ControllableRunner(source).runWith(new Crasher(3));
            var pending = expectThrows(InterruptedException.class, () -> runner.seek(5));
            assertSame(pending.getCause(), Crasher.FAILURE);
            runner.join();
            assertFalse(runner.isRunning());
            var later = expectThrows(InterruptedException.class, () -> runner.seek(10));
            assertSame(later.getCause(), Crasher.FAILURE);
            expectThrows(InterruptedException.class, runner::tick);
        }
    }

    @Test(timeOut = 60_000)
    public void callsAfterHaltThrow() throws Exception {
        try (var source = new MappedFileSource(replay().toString())) {
            var runner = new ControllableRunner(source).runWith(new Object());
            runner.seek(100);
            runner.halt();
            runner.join();
            var e = expectThrows(InterruptedException.class, () -> runner.seek(200));
            assertNull(e.getCause());
            expectThrows(InterruptedException.class, runner::tick);
        }
    }

    @Test(timeOut = 60_000)
    public void haltWhileSeekingWakesWaiter() throws Exception {
        try (var source = new MappedFileSource(replay().toString())) {
            var runner = new ControllableRunner(source).runWith(new Sleeper());
            runner.seek(0);
            var failure = new Throwable[1];
            var waiter = new Thread(() -> {
                try {
                    runner.seek(4);
                } catch (Throwable t) {
                    failure[0] = t;
                }
            });
            waiter.start();
            Thread.sleep(200);
            runner.halt();
            waiter.join();
            assertNotNull(failure[0]);
            assertEquals(failure[0].getClass(), InterruptedException.class);
            runner.join();
        }
    }
}
