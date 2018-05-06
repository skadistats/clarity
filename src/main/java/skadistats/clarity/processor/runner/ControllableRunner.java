package skadistats.clarity.processor.runner;

import com.google.protobuf.GeneratedMessage;
import skadistats.clarity.ClarityException;
import skadistats.clarity.processor.reader.PacketInstance;
import skadistats.clarity.source.PacketPosition;
import skadistats.clarity.source.ResetRelevantKind;
import skadistats.clarity.source.Source;

import java.io.EOFException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ControllableRunner extends AbstractFileRunner {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition wantedTickReached = lock.newCondition();
    private final Condition moreProcessingNeeded = lock.newCondition();

    private Thread runnerThread;
    private Exception runnerException;

    private TreeSet<PacketPosition> resetRelevantPackets = new TreeSet<>();
    private int resetRelevantOffset = -1;
    private LinkedList<ResetStep> resetSteps;

    /* tick the processor is waiting at to be signaled to continue further processing */
    private int upcomingTick;
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
            int diff = wantedTick - tick;

            if (diff >= 0 && diff <= 5) {
                return;
            }

            resetSteps = new LinkedList<>();
            resetSteps.add(new ResetStep(LoopController.Command.RESET_START, null));
            if (diff < 0 || engineType.isFullPacketSeekAllowed()) {
                TreeSet<PacketPosition> seekPositions = getResetPacketsBeforeTick(wantedTick);
                resetSteps.add(new ResetStep(LoopController.Command.RESET_CLEAR, null));
                while (seekPositions.size() > 0) {
                    PacketPosition pp = seekPositions.pollFirst();
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
            ResetStep step = resetSteps.peekFirst();
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

    public class LockingLoopController extends LoopController {

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
                PacketPosition pp = newResetRelevantPacketPosition(tick, kind, offset);
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
        int backup = source.getPosition();
        PacketPosition wanted = PacketPosition.createPacketPosition(wantedTick, ResetRelevantKind.FULL_PACKET, 0);
        if (resetRelevantPackets.tailSet(wanted, true).size() == 0) {
            PacketPosition basePos = resetRelevantPackets.floor(wanted);
            source.setPosition(basePos.getOffset());
            try {
                while (true) {
                    int at = source.getPosition();
                    PacketInstance<GeneratedMessage> pi = engineType.getNextPacketInstance(source);
                    PacketPosition pp = newResetRelevantPacketPosition(pi.getTick(), pi.getResetRelevantKind(), at);
                    if (pp != null) {
                        addResetRelevant(pp);
                    }
                    if (pi.getTick() >= wantedTick) {
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

    public static class ResetStep {
        private final LoopController.Command command;
        private final Integer offset;
        public ResetStep(LoopController.Command command, Integer offset) {
            this.command = command;
            this.offset = offset;
        }
    }

    public ControllableRunner(Source s) throws IOException {
        super(s, s.readEngineType());
        upcomingTick = tick;
        wantedTick = tick;
        this.loopController = new LockingLoopController(normalLoopControl);
    }

    public ControllableRunner runWith(final Object... processors) {
        runnerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                log.debug("runner started");
                try {
                    initAndRunWith(processors);
                } catch (Exception e) {
                    lock.lock();
                    try {
                        runnerException = e;
                        wantedTickReached.signal();
                    } finally {
                        lock.unlock();
                    }
                }
                log.debug("runner finished");
                runnerThread = null;
            }
        });
        runnerThread.setName("clarity-runner");
        runnerThread.setDaemon(false);
        runnerThread.start();
        return this;
    }

    public boolean isResetting() {
        lock.lock();
        try {
            return loopController.controllerFunc == seekLoopControl;
        } finally {
            lock.unlock();
        }
    }

    public boolean isRunning() {
        return runnerThread != null;
    }

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
        wantedTickReached.await();
        if (runnerException != null) {
            throw new InterruptedException() {
                @Override
                public String getMessage() {
                    return "Runner thread was terminated by an exception";
                }
                @Override
                public synchronized Throwable getCause() {
                    return runnerException;
                }
            };
        }
    }

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

    public boolean isAtEnd() {
        return upcomingTick == Integer.MAX_VALUE;
    }

    public void halt() {
        if (runnerThread != null && runnerThread.isAlive()) {
            runnerThread.interrupt();
        }
    }

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
