package skadistats.clarity.processor.runner;

import skadistats.clarity.decoder.DemoInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ControllableRunner extends AbstractRunner<ControllableRunner> {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition wantedTickReached = lock.newCondition();
    private final Condition moreProcessingNeeded = lock.newCondition();

    private final File demoFile;
    private Thread runnerThread;

    private Integer lastTick;

    /* tick the processor is waiting at to be signaled to continue further processing */
    private int upcomingTick = -1;
    /* tick we want to be at the end of */
    private int wantedTick = -1;
    /* tick the user wanted to be at the end of */
    private Integer demandedTick;

    public ControllableRunner(File demoFile) throws IOException {
        this.demoFile = demoFile;
        initCodedInputStreamForTick(-1);
    }

    private int initCodedInputStreamForTick(int initTick) throws IOException {
        int fpTick;
        DemoInputStream dis = new DemoInputStream(new FileInputStream(demoFile));
        if (initTick == - 1) {
            dis.ensureDemHeader();
            fpTick = -1;
        } else {
            DemoInputStream.PacketPosition pos = dis.getFullPacketBeforeTick(initTick, fullPacketPositions);
            dis = new DemoInputStream(new FileInputStream(demoFile));
            dis.skipBytes(pos.getOffset());
            fpTick = pos.getTick();
            shouldEmitFullPacket = true;
        }
        cis = dis.newCodedInputStream();
        cisBaseOffset = dis.getOffset();
        return fpTick;
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
                    return Source.LoopControlCommand.BREAK;
                }
                try {
                    upcomingTick = nextTick;
                    if (demandedTick != null) {
                        processorTick = upcomingTick;
                        endTicksUntil(ctx, tick);
                        return processDemandedTick(ctx);
                    } else if (upcomingTick <= wantedTick) {
                        processorTick = upcomingTick;
                        endTicksUntil(ctx, upcomingTick - 1);
                        return processDemandedTick(ctx);
                    } else {
                        endTicksUntil(ctx, wantedTick);
                        wantedTickReached.signalAll();
                        moreProcessingNeeded.await();
                        processorTick = upcomingTick;
                        return processDemandedTick(ctx);
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
        };
    }

    private Source.LoopControlCommand processDemandedTick(Context ctx) throws IOException {
        boolean headerProcessed = tick != - 1;
        if (demandedTick == null || !headerProcessed) {
            startNewTick(ctx);
            return Source.LoopControlCommand.FALLTHROUGH;
        } else {
            wantedTick = demandedTick;
            demandedTick = null;
            int diff = wantedTick - upcomingTick;
            if (diff >= 0 && diff < 1800) {
                startNewTick(ctx);
                return Source.LoopControlCommand.FALLTHROUGH;
            } else {
                upcomingTick = initCodedInputStreamForTick(wantedTick);
                processorTick = upcomingTick;
                tick =  upcomingTick - 1;
                startNewTick(ctx);
                return Source.LoopControlCommand.CONTINUE;
            }
        }
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
