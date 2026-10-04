package skadistats.clarity.processor.runner;

import skadistats.clarity.ClarityException;
import skadistats.clarity.source.PacketPosition;
import skadistats.clarity.source.ResetRelevantKind;
import skadistats.clarity.source.Source;

import java.io.EOFException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Asynchronous runner that processes a replay in a background thread and supports
 * seeking and tick-by-tick advance via {@link #tick()}, {@link #seek(int)}, etc.
 *
 * <p>The runner does <b>not</b> own the {@link Source}: the caller is responsible
 * for closing it, but must wait for the runner thread to terminate first via
 * {@link #join()}. The recommended pattern is try-with-resources:
 *
 * <pre>{@code
 * try (var source = new MappedFileSource(path)) {
 *     var runner = new ControllableRunner(source).runWith(processor);
 *     // ... seek/tick ...
 *     runner.halt();
 *     runner.join();
 * }
 * }</pre>
 *
 * <p>Failing to close the source will leak file descriptors and memory mappings
 * until the JVM Cleaner releases them — see issue #289.
 *
 * <p><b>Threading:</b> {@link #runWith(Object...)} starts a dedicated thread ({@code clarity-runner}) that
 * runs all event listeners. {@link #seek(int)}, {@link #tick()} and {@link #setDemandedTick(int)} are meant to
 * be called from a controlling thread and synchronize with the runner thread via a lock. {@link #seek(int)}
 * and {@link #tick()} block until the runner thread has reached the requested tick, so the processors' state
 * is stable when they return. {@link #halt()}, {@link #join()}, {@link #isRunning()}, {@link #isResetting()}
 * and {@link #isAtEnd()} may be called from any thread. {@link #getTick()} is not synchronized.
 *
 * <p><b>Tick semantics:</b> after {@code runWith}, the runner stops at the end of tick 0. {@code tick()} moves
 * to the end of the next tick, {@code seek(n)} to the end of tick {@code n}. At those points the listeners
 * have been called for the tick, including {@code OnTickEnd}.
 */
public class ControllableRunner extends AbstractFileRunner {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition wantedTickReached = lock.newCondition();
    private final Condition moreProcessingNeeded = lock.newCondition();

    private volatile Thread runnerThread;
    private Exception runnerException;
    private boolean terminated;
    private long ticksReached;
    private Consumer<Throwable> onException;

    private final TreeSet<PacketPosition> resetRelevantPackets = new TreeSet<>();
    private int resetRelevantOffset = -1;
    private LinkedList<ResetStep> resetSteps;

    /* tick the processor is waiting at to be signaled to continue further processing */
    private volatile int upcomingTick;
    /* tick we want to be at the end of */
    private int wantedTick;
    /* tick the user wanted to be at the end of */
    private Integer demandedTick;

    private long t0;

    private final LoopController.Func normalLoopControl = new LoopController.Func() {
        @Override
        public LoopController.Command doLoopControl(int nextTickWithData) throws Exception {
            if (!loopController.isSyncTickSeen()) {
                if (tick == -1) {
                    wantedTick = 0;
                    startNewTick(0);
                }
                return LoopController.Command.FALLTHROUGH;
            }
            upcomingTick = nextTickWithData;
            if (upcomingTick == tick) {
                return LoopController.Command.FALLTHROUGH;
            }
            if (demandedTick != null) {
                handleDemandedTick();
                return LoopController.Command.AGAIN;
            }
            endTicksUntil(tick);
            if (tick == wantedTick) {
                if (log.isDebugEnabled() && t0 != 0) {
                    log.debug("now at %d. Took %d microns.", tick, (System.nanoTime() - t0) / 1000);
                    t0 = 0;
                }
                ticksReached++;
                wantedTickReached.signalAll();
                moreProcessingNeeded.await();
                if (demandedTick != null) {
                    handleDemandedTick();
                    return LoopController.Command.AGAIN;
                }
            }
            startNewTick(upcomingTick);
            if (wantedTick < upcomingTick) {
                return LoopController.Command.AGAIN;
            }
            return LoopController.Command.FALLTHROUGH;
        }

        private void handleDemandedTick() throws IOException {
            wantedTick = demandedTick;
            demandedTick = null;
            var diff = wantedTick - tick;

            if (diff >= 0 && diff <= 5) {
                return;
            }

            resetSteps = new LinkedList<>();
            resetSteps.add(new ResetStep(LoopController.Command.RESET_START, null));
            if (diff < 0 || engineType.isFullPacketSeekAllowed()) {
                var seekPositions = getResetPacketsBeforeTick(wantedTick);
                resetSteps.add(new ResetStep(LoopController.Command.RESET_CLEAR, null));
                while (seekPositions.size() > 0) {
                    var pp = seekPositions.pollFirst();
                    switch (pp.getKind()) {
                        case STRINGTABLE:
                        case FULL_PACKET:
                            resetSteps.add(new ResetStep(LoopController.Command.CONTINUE, pp.getOffset()));
                            resetSteps.add(new ResetStep(LoopController.Command.RESET_ACCUMULATE, null));
                            if (seekPositions.size() == 0) {
                                resetSteps.add(new ResetStep(LoopController.Command.RESET_APPLY, null));
                            }
                            break;
                        case SYNC:
                            if (seekPositions.size() == 0) {
                                resetSteps.add(new ResetStep(LoopController.Command.CONTINUE, pp.getOffset()));
                                resetSteps.add(new ResetStep(LoopController.Command.RESET_APPLY, null));
                            }
                            break;
                    }
                }
            }
            resetSteps.add(new ResetStep(LoopController.Command.RESET_FORWARD, null));
            resetSteps.add(new ResetStep(LoopController.Command.RESET_COMPLETE, null));
            loopController.controllerFunc = seekLoopControl;
        }
    };

    private final LoopController.Func seekLoopControl = new LoopController.Func() {
        @Override
        public LoopController.Command doLoopControl(int nextTickWithData) throws Exception {
            upcomingTick = nextTickWithData;
            var step = resetSteps.peekFirst();
            switch (step.command) {
                case CONTINUE:
                    resetSteps.pollFirst();
                    source.setPosition(step.offset);
                    return step.command;
                case RESET_FORWARD:
                    if (wantedTick >= upcomingTick) {
                        return LoopController.Command.FALLTHROUGH;
                    }
                    resetSteps.pollFirst();
                    return LoopController.Command.AGAIN;
                case RESET_COMPLETE:
                    resetSteps = null;
                    loopController.controllerFunc = normalLoopControl;
                    tick = wantedTick - 1;
                    startNewTick(upcomingTick);
                    return LoopController.Command.RESET_COMPLETE;
                default:
                    resetSteps.pollFirst();
                    return step.command;
            }
        }
    };

    /**
     * Loop controller that serializes all loop control and reset-relevant-packet registration with the runner lock.
     */
    public class LockingLoopController extends LoopController {

        /** Internal. */
        public LockingLoopController(Func controllerFunc) {
            super(controllerFunc);
        }

        @Override
        public Command doLoopControl(int nextTick) throws Exception {
            try {
                lock.lockInterruptibly();
            } catch (InterruptedException e) {
                return Command.BREAK;
            }
            try {
                return super.doLoopControl(nextTick);
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void markResetRelevantPacket(int tick, ResetRelevantKind kind, int offset) throws IOException {
            lock.lock();
            try {
                var pp = newResetRelevantPacketPosition(tick, kind, offset);
                if (pp == null) {
                    throw new ClarityException("tried to mark non reset relevant packet");
                }
                addResetRelevant(pp);
            } finally {
                lock.unlock();
            }
        }
    }

    private void addResetRelevant(PacketPosition pp) {
        if (pp.getOffset() > resetRelevantOffset) {
            resetRelevantPackets.add(pp);
            resetRelevantOffset = pp.getOffset();
        }
    }

    private PacketPosition newResetRelevantPacketPosition(int tick, ResetRelevantKind kind, int offset) {
        return kind == null ? null : PacketPosition.createPacketPosition(loopController.isSyncTickSeen() ? tick : -1, kind, offset);
    }

    private TreeSet<PacketPosition> getResetPacketsBeforeTick(int wantedTick) throws IOException {
        var backup = source.getPosition();
        var wanted = PacketPosition.createPacketPosition(wantedTick, ResetRelevantKind.FULL_PACKET, 0);
        if (resetRelevantPackets.tailSet(wanted, true).size() == 0) {
            var basePos = resetRelevantPackets.floor(wanted);
            source.setPosition(basePos.getOffset());
            var lastFullFoundAtTick = basePos.getTick();
            try {
                while (true) {
                    var at = source.getPosition();
                    var pi = engineType.getNextPacketInstance(source);
                    var pp = newResetRelevantPacketPosition(pi.getTick(), pi.getResetRelevantKind(), at);
                    if (pp != null) {
                        lastFullFoundAtTick = pp.getTick();
                        addResetRelevant(pp);
                    }
                    if (pi.getTick() >= wantedTick) {
                        break;
                    }
                    if (pi.getTick() >= lastFullFoundAtTick + engineType.getExpectedFullPacketInterval() + 100) {
                        break;
                    }
                    pi.skip();
                }
            } catch (EOFException e) {
            }
        }
        source.setPosition(backup);
        return new TreeSet<>(resetRelevantPackets.headSet(wanted, true));
    }

    /**
     * One step of a seek plan; internal.
     */
    public static class ResetStep {
        private final LoopController.Command command;
        private final Integer offset;
        /** Internal. */
        public ResetStep(LoopController.Command command, Integer offset) {
            this.command = command;
            this.offset = offset;
        }
    }

    /**
     * @param s the source to read from; the engine type is determined by reading its magic
     * @throws IOException if the source does not contain a valid replay
     */
    public ControllableRunner(Source s) throws IOException {
        super(s, s.determineEngineType());
        upcomingTick = tick;
        wantedTick = tick;
        this.loopController = new LockingLoopController(normalLoopControl);
    }

    /**
     * Starts processing on a new non-daemon thread named {@code clarity-runner} and returns immediately.
     * The thread runs up to the end of tick 0 and then waits for {@link #tick()}, {@link #seek(int)} or {@link #setDemandedTick(int)}.
     *
     * <p>If the runner thread terminates with an exception, it is logged, remembered, passed to the callback set
     * with {@link #setOnException(Consumer)} (not for {@link InterruptedException}), and set as the cause of the
     * {@link InterruptedException} thrown by {@link #seek(int)} and {@link #tick()}. Once the runner thread has
     * terminated for any reason, including {@link #halt()}, those methods throw instead of blocking.
     *
     * @param processors processor instances whose {@code @On*} methods are registered; arrays are flattened
     * @return this runner
     */
    public ControllableRunner runWith(final Object... processors) {
        markStarted();
        runnerThread = new Thread(() -> {
            log.debug("runner started");
            Exception failure = null;
            try {
                initAndRunWith(processors);
            } catch (Exception e) {
                failure = e;
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                } else {
                    log.error("Runner thread crashed", e);
                }
            } finally {
                lock.lock();
                try {
                    runnerException = failure instanceof InterruptedException ? null : failure;
                    terminated = true;
                    wantedTickReached.signalAll();
                } finally {
                    lock.unlock();
                }
            }
            if (failure != null && !(failure instanceof InterruptedException) && onException != null) {
                try {
                    onException.accept(failure);
                } catch (Throwable t) {
                    log.error("onException handler threw", t);
                }
            }
            log.debug("runner finished");
            runnerThread = null;
        });
        runnerThread.setName("clarity-runner");
        runnerThread.setDaemon(false);
        runnerThread.start();
        return this;
    }

    /**
     * Sets a callback invoked on the runner thread when it dies with an exception other than {@link InterruptedException}.
     *
     * @param onException the callback
     */
    public void setOnException(Consumer<Throwable> onException) {
        this.onException = onException;
    }

    /**
     * @return {@code true} while the runner is repositioning the source for a backwards or far-forward seek
     */
    public boolean isResetting() {
        lock.lock();
        try {
            return loopController.controllerFunc == seekLoopControl;
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return {@code true} while the runner thread exists, i.e. from {@code runWith} until the thread has finished
     */
    public boolean isRunning() {
        return runnerThread != null;
    }

    /**
     * Requests to be positioned at the end of the given tick without waiting for it to be reached.
     * The runner thread picks the request up at its next tick boundary. Use {@link #seek(int)} to block until done.
     *
     * @param demandedTick the tick to move to
     */
    public void setDemandedTick(int demandedTick) {
        lock.lock();
        try {
            this.demandedTick = demandedTick;
            t0 = System.nanoTime();
            moreProcessingNeeded.signal();
        } finally {
            lock.unlock();
        }
    }

    private void waitForTickReached() throws InterruptedException {
        var reached = ticksReached;
        while (reached == ticksReached) {
            if (terminated) {
                throw terminatedException();
            }
            wantedTickReached.await();
        }
    }

    private InterruptedException terminatedException() {
        if (runnerException == null) {
            return new InterruptedException("Runner thread has terminated");
        }
        var e = new InterruptedException("Runner thread was terminated by an exception");
        e.initCause(runnerException);
        return e;
    }

    /**
     * Moves to the end of the given tick and blocks until it has been reached.
     *
     * <p>Seeking forward by up to 5 ticks just processes the ticks in between. Otherwise the source is repositioned to
     * the nearest preceding reset-relevant packets (full packets, string tables, sync) and processing resumes from there.
     * For a source that cannot move backwards (e.g. {@link skadistats.clarity.source.InputStreamSource}), only forward
     * seeking is possible.
     *
     * @param demandedTick the tick to move to
     * @throws InterruptedException if the calling thread is interrupted, or if the runner thread has terminated
     *                              (its exception, if any, is available via {@link Throwable#getCause()})
     */
    public void seek(int demandedTick) throws InterruptedException {
        lock.lock();
        try {
            this.demandedTick = demandedTick;
            t0 = System.nanoTime();
            moreProcessingNeeded.signal();
            waitForTickReached();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Advances by one tick and blocks until the end of that tick has been reached. If the runner has not yet
     * reached the previously requested tick, waits for it first.
     *
     * @throws InterruptedException if the calling thread is interrupted, or if the runner thread has terminated
     *                              (its exception, if any, is available via {@link Throwable#getCause()})
     */
    public void tick() throws InterruptedException {
        lock.lock();
        try {
            if (tick != wantedTick) {
                waitForTickReached();
            }
            wantedTick++;
            t0 = System.nanoTime();
            moreProcessingNeeded.signal();
            waitForTickReached();
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return {@code true} once the runner has reached the end of the replay data
     */
    public boolean isAtEnd() {
        return upcomingTick == Integer.MAX_VALUE;
    }

    /**
     * Interrupts the runner thread, ending the run. Does not wait; call {@link #join()} afterwards.
     */
    public void halt() {
        var t = runnerThread;
        if (t != null && t.isAlive()) {
            t.interrupt();
        }
    }

    /**
     * Waits for the runner thread to terminate. Call after {@link #halt()} before closing the {@link Source}
     * — otherwise the thread may still be reading from it. The runner thread does not end on its own at the end
     * of the replay; it keeps waiting for further commands, so call {@link #halt()} first.
     */
    public void join() throws InterruptedException {
        Thread t = runnerThread;
        if (t != null) {
            t.join();
        }
    }

    /**
     * Returns the last tick of the replay. Takes the runner lock; the source position is not changed while
     * the runner is waiting.
     *
     * @return the last tick
     * @throws RuntimeException wrapping the {@link IOException} if the last tick cannot be determined
     */
    @Override
    public int getLastTick() {
        lock.lock();
        try {
            try {
                return source.getLastTick();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } finally {
            lock.unlock();
        }
    }

}
