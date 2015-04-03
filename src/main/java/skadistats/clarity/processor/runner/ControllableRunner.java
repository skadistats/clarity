package skadistats.clarity.processor.runner;

import com.google.protobuf.CodedInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ControllableRunner extends AbstractRunner<ControllableRunner> {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition wantedTickReached = lock.newCondition();
    private final Condition moreProcessingNeeded = lock.newCondition();

    private final CodedInputStream cis;

    private Thread runnerThread;

    /* tick the processor is waiting at to be signaled to continue further processing */
    private int upcomingTick = 0;
    /* tick the processor has last processed */
    private int processorTick = -1;
    /* tick the user wants to be at the end of */
    private int wantedTick = -1;

    public ControllableRunner(InputStream inputStream) throws IOException {
        ensureDemHeader(inputStream);
        this.cis = createCodedInputStream(inputStream);
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
        return new Source() {
            @Override
            public CodedInputStream stream() {
                return cis;
            }

            @Override
            public boolean isTickBorder(int upcomingTick) {
                return processorTick != upcomingTick;
            }

            @Override
            public LoopControlCommand doLoopControl(Context ctx, int nextTick) {
                try {
                    lock.lockInterruptibly();
                } catch (InterruptedException e) {
                    return Source.LoopControlCommand.BREAK;
                }
                try {
                    upcomingTick = nextTick;
                    if (upcomingTick <= wantedTick) {
                        processorTick = upcomingTick;
                        endTicksUntil(ctx, upcomingTick - 1);
                        startNewTick(ctx);
                        return LoopControlCommand.FALLTHROUGH;
                    } else {
                        endTicksUntil(ctx, wantedTick);
                        wantedTickReached.signalAll();
                        try {
                            moreProcessingNeeded.await();
                        } catch (InterruptedException e) {
                            return LoopControlCommand.BREAK;
                        }
                        processorTick = upcomingTick;
                        startNewTick(ctx);
                        return LoopControlCommand.FALLTHROUGH;
                    }
                } finally {
                    lock.unlock();
                }
            }
        };
    }

    public void tick() throws InterruptedException {
        lock.lock();
        try {
            if (tick != wantedTick) {
                wantedTickReached.await();
            }
            wantedTick++;
            moreProcessingNeeded.signal();
            wantedTickReached.await();
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

}
