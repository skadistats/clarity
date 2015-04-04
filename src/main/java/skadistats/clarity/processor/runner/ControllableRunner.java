package skadistats.clarity.processor.runner;

import com.google.common.collect.Iterators;
import skadistats.clarity.decoder.DemoInputStream;
import skadistats.clarity.processor.reader.ResetPhase;
import skadistats.clarity.source.LoopControlCommand;
import skadistats.clarity.source.Source;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

    private final File demoFile;
    private Thread runnerThread;

    private DemoInputStream dis;
    private int cisBaseOffset;
    private TreeSet<DemoInputStream.PacketPosition> fullPacketPositions = new TreeSet<>();
    private ResetPhase resetPhase;
    private TreeSet<DemoInputStream.PacketPosition> seekPositions;

    private Integer lastTick;

    /* tick the processor is waiting at to be signaled to continue further processing */
    private int upcomingTick = -1;
    /* tick we want to be at the end of */
    private int wantedTick = -1;
    /* tick the user wanted to be at the end of */
    private Integer demandedTick;

    public ControllableRunner(File demoFile) throws IOException {
        this.demoFile = demoFile;
        dis = newDemoStream();
        dis.ensureDemHeader();
        newCodedStream();
    }

    private DemoInputStream newDemoStream() throws FileNotFoundException {
        return new DemoInputStream(new FileInputStream(demoFile));
    }

    private void newCodedStream() throws IOException {
        cis = dis.newCodedInputStream();
        cisBaseOffset = dis.getOffset();
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

    @Override
    protected Source getSource() {
        return new AbstractSource() {
            @Override
            public LoopControlCommand doLoopControl(Context ctx, int nextTick) {
                try {
                    lock.lockInterruptibly();
                } catch (InterruptedException e) {
                    return LoopControlCommand.BREAK;
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
                            return LoopControlCommand.FALLTHROUGH;
                        } else {
                            if (tick == -1) {
                                startNewTick(ctx);
                                processorTick = upcomingTick;
                                return LoopControlCommand.FALLTHROUGH;
                            }
                            if (resetPhase == null) {
                                endTicksUntil(ctx, tick);
                            }
                            if (demandedTick != null) {
                                resetPhase = ResetPhase.CLEAR;
                                wantedTick = demandedTick;
                                demandedTick = null;
                                seekPositions = newDemoStream().getFullPacketsBeforeTick(wantedTick, fullPacketPositions);
                            }
                            dis = newDemoStream();
                            DemoInputStream.PacketPosition pos = seekPositions.pollFirst();
                            dis.skipTo(pos);
                            newCodedStream();
                            processorTick = pos.getTick();
                            return LoopControlCommand.CONTINUE;
                        }
                    }
                } catch (IOException e) {
                    log.error("IO error in runner thread", e);
                    return LoopControlCommand.BREAK;
                } catch (InterruptedException e) {
                    return LoopControlCommand.BREAK;
                } finally {
                    lock.unlock();
                }
            }

            @Override
            public Iterator<ResetPhase> evaluateResetPhases(int tick, int cisOffset) throws IOException {
                if (resetPhase == null) {
                    fullPacketPositions.add(new DemoInputStream.PacketPosition(tick, cisBaseOffset + cisOffset));
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
            DemoInputStream dis = new DemoInputStream(new FileInputStream(demoFile));
            int fileInfoOffset = dis.ensureDemHeader();
            dis.skipBytes(fileInfoOffset - 12);
            dis.readRawVarint32();
            lastTick = dis.readRawVarint32();
            dis.close();
        }
        return lastTick;
    }

}
