package skadistats.clarity.processor.runner;

import com.google.common.collect.Iterators;
import skadistats.clarity.processor.reader.ResetPhase;
import skadistats.clarity.source.PacketPosition;
import skadistats.clarity.source.Source;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ControllableRunner extends AbstractRunner<ControllableRunner> {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition wantedTickReached = lock.newCondition();
    private final Condition moreProcessingNeeded = lock.newCondition();

    private Thread runnerThread;

    private TreeSet<PacketPosition> fullPacketPositions = new TreeSet<>();
    private ResetPhase resetPhase;
    private TreeSet<PacketPosition> seekPositions;

    private Integer lastTick;

    /* tick the processor is waiting at to be signaled to continue further processing */
    private int upcomingTick = -1;
    /* tick we want to be at the end of */
    private int wantedTick = -1;
    /* tick the user wanted to be at the end of */
    private Integer demandedTick;

    public ControllableRunner(Source s) throws IOException {
        super(s);
        source.ensureDemoHeader();
        source.skipBytes(4);
        this.loopController = new LoopController() {
            @Override
            public boolean isTickBorder(int upcomingTick) {
                return processorTick != upcomingTick;
            }
            @Override
            public Command doLoopControl(Context ctx, int nextTick) {
                try {
                    lock.lockInterruptibly();
                } catch (InterruptedException e) {
                    return Command.BREAK;
                }
                try {
                    upcomingTick = nextTick;
                    while (true) {
                        if ((demandedTick == null && resetPhase == null)) {
                            endTicksUntil(ctx, tick);
                            if (tick == wantedTick) {
                                wantedTickReached.signalAll();
                                moreProcessingNeeded.await();
                            }
                            if (demandedTick != null) {
                                continue;
                            }
                            startNewTick(ctx);
                            if (wantedTick < upcomingTick) {
                                continue;
                            }
                            processorTick = upcomingTick;
                            return Command.FALLTHROUGH;
                        } else {
                            if (tick == -1) {
                                startNewTick(ctx);
                                processorTick = upcomingTick;
                                return Command.FALLTHROUGH;
                            }
                            if (resetPhase == null) {
                                endTicksUntil(ctx, tick);
                            }
                            if (demandedTick != null) {
                                source.setPosition(0);
                                resetPhase = ResetPhase.CLEAR;
                                wantedTick = demandedTick;
                                demandedTick = null;
                                seekPositions = source.getFullPacketsBeforeTick(wantedTick, fullPacketPositions);
                            }
                            PacketPosition pos = seekPositions.pollFirst();
                            source.setPosition(pos.getOffset());
                            processorTick = pos.getTick();
                            return Command.CONTINUE;
                        }
                    }
                } catch (IOException e) {
                    log.error("IO error in runner thread", e);
                    return Command.BREAK;
                } catch (InterruptedException e) {
                    return Command.BREAK;
                } finally {
                    lock.unlock();
                }
            }

            @Override
            public Iterator<ResetPhase> evaluateResetPhases(int tick, int offset) throws IOException {
                if (resetPhase == null) {
                    fullPacketPositions.add(new PacketPosition(tick, offset));
                    return Iterators.emptyIterator();
                }
                List<ResetPhase> phaseList = new LinkedList<>();
                if (resetPhase == ResetPhase.CLEAR) {
                    resetPhase = ResetPhase.STRINGTABLE_ACCUMULATION;
                    phaseList.add(ResetPhase.CLEAR);
                    phaseList.add(ResetPhase.STRINGTABLE_ACCUMULATION);
                } else if (!seekPositions.isEmpty()) {
                    phaseList.add(ResetPhase.STRINGTABLE_ACCUMULATION);
                }
                if (seekPositions.isEmpty()) {
                    resetPhase = null;
                    phaseList.add(ResetPhase.STRINGTABLE_APPLY);
                }
                return phaseList.iterator();
            }
        };
    }

    @Override
    public ControllableRunner runWith(final Object... processors) {
        runnerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                ControllableRunner.super.runWith(processors);
            }
        });
        runnerThread.start();
        return this;
    }

    public void setDemandedTick(int demandedTick) {
        lock.lock();
        try {
            this.demandedTick = demandedTick;
            moreProcessingNeeded.signal();
        } finally {
            lock.unlock();
        }
    }

    public void seek(int demandedTick) {
        lock.lock();
        try {
            this.demandedTick = demandedTick;
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
            try {
                lastTick = source.getLastTick();
            } finally {
                lock.unlock();
            }
        }
        return lastTick;
    }

}
