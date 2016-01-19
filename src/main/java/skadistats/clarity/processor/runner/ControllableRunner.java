package skadistats.clarity.processor.runner;

import skadistats.clarity.source.PacketPosition;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.common.proto.Demo;

import java.io.IOException;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ControllableRunner extends AbstractRunner<ControllableRunner> {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition wantedTickReached = lock.newCondition();
    private final Condition moreProcessingNeeded = lock.newCondition();

    private Thread runnerThread;

    private TreeSet<PacketPosition> resetRelevantPackets = new TreeSet<>();
    private LinkedList<ResetStep> resetSteps;

    private Integer lastTick;

    /* tick the processor is waiting at to be signaled to continue further processing */
    private int upcomingTick;
    /* tick we want to be at the end of */
    private int wantedTick;
    /* tick the user wanted to be at the end of */
    private Integer demandedTick;

    private long t0;

    private final LoopController.Func normalLoopControl = new LoopController.Func() {
        @Override
        public LoopController.Command doLoopControl(Context ctx, int nextTickWithData) {
            try {
                if (!loopController.isSyncTickSeen()) {
                    if (tick == -1) {
                        startNewTick(ctx, 0);
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
                endTicksUntil(ctx, tick);
                if (tick == wantedTick) {
                    if (log.isDebugEnabled() && t0 != 0) {
                        log.debug("now at {}. Took {} microns.", tick, (System.nanoTime() - t0) / 1000);
                        t0 = 0;
                    }
                    wantedTickReached.signalAll();
                    moreProcessingNeeded.await();
                    if (demandedTick != null) {
                        handleDemandedTick();
                        return LoopController.Command.AGAIN;
                    }
                }
                startNewTick(ctx, upcomingTick);
                if (wantedTick < upcomingTick) {
                    return LoopController.Command.AGAIN;
                }
                return LoopController.Command.FALLTHROUGH;
            } catch (InterruptedException e) {
                return LoopController.Command.BREAK;
            } catch (IOException e) {
                log.error("IO error in runner thread", e);
                return LoopController.Command.BREAK;
            }
        }

        private void handleDemandedTick() throws IOException {
            wantedTick = demandedTick;
            demandedTick = null;
            int diff = wantedTick - tick;
            if (diff < 0 || diff > 200) {
                calculateResetSteps();
                loopController.controllerFunc = seekLoopControl;
            }
        }
    };

    private final LoopController.Func seekLoopControl = new LoopController.Func() {
        @Override
        public LoopController.Command doLoopControl(Context ctx, int nextTickWithData) {
            try {
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
                        startNewTick(ctx, upcomingTick);
                        return LoopController.Command.RESET_COMPLETE;
                    default:
                        resetSteps.pollFirst();
                        return step.command;
                }
            } catch (IOException e) {
                log.error("IO error in runner thread", e);
                return LoopController.Command.BREAK;
            }
        }
    };

    private void calculateResetSteps() throws IOException {
        TreeSet<PacketPosition> seekPositions = source.getResetPacketsBeforeTick(engineType, wantedTick, resetRelevantPackets);
        seekPositions.pollFirst();

        resetSteps = new LinkedList<>();
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
        resetSteps.add(new ResetStep(LoopController.Command.RESET_FORWARD, null));
        resetSteps.add(new ResetStep(LoopController.Command.RESET_COMPLETE, null));
    }


    public class LockingLoopController extends LoopController {

        public LockingLoopController(Func controllerFunc) {
            super(controllerFunc);
        }

        @Override
        public Command doLoopControl(Context ctx, int nextTick) {
            try {
                lock.lockInterruptibly();
            } catch (InterruptedException e) {
                return Command.BREAK;
            }
            try {
                return super.doLoopControl(ctx, nextTick);
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void markResetRelevantPacket(int tick, int kind, int offset) throws IOException {
            lock.lock();
            try {
                resetRelevantPackets.add(PacketPosition.createPacketPosition(tick, kind, offset));
            } finally {
                lock.unlock();
            }
        }
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
        resetRelevantPackets.add(PacketPosition.createPacketPosition(-1, Demo.EDemoCommands.DEM_SyncTick_VALUE, s.getPosition()));
        upcomingTick = tick;
        wantedTick = tick;
        this.loopController = new LockingLoopController(normalLoopControl);
    }

    @Override
    public ControllableRunner runWith(final Object... processors) {
        runnerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                log.debug("runner started");
                ControllableRunner.super.runWith(processors);
                log.debug("runner finished");
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

    public void seek(int demandedTick) {
        lock.lock();
        try {
            this.demandedTick = demandedTick;
            t0 = System.nanoTime();
            moreProcessingNeeded.signal();
            wantedTickReached.awaitUninterruptibly();
        } finally {
            lock.unlock();
        }
    }

    public void tick() {
        lock.lock();
        try {
            if (tick != wantedTick) {
                wantedTickReached.awaitUninterruptibly();
            }
            wantedTick++;
            t0 = System.nanoTime();
            moreProcessingNeeded.signal();
            wantedTickReached.awaitUninterruptibly();
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

    public int getLastTick() throws IOException {
        if (lastTick == null) {
            lock.lock();
            try {
                lastTick = source.getLastTick();
            } finally {
                lock.unlock();
            }
        }
        return lastTick;
    }

}
