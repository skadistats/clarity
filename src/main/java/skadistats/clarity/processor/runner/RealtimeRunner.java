package skadistats.clarity.processor.runner;

import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.cs.csgo.proto.CsgoNetMessages;
import skadistats.clarity.wire.shared.demo.proto.DemoNetMessages;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static java.time.Instant.now;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;


/**
 * A {@link SimpleRunner} that paces ticks to wall-clock time.
 *
 * <p>Before each tick starts, the calling thread sleeps until {@code startTime + delay + tick * tickInterval}.
 * The tick interval is taken from the replay's {@code CSVCMsg_ServerInfo}; until it is seen, and before the
 * sync tick, no pacing is applied. If the thread is interrupted while sleeping, the interrupt flag is set
 * and processing continues without further waiting for that tick.
 */
public class RealtimeRunner extends SimpleRunner {

    private static final long SECOND_TO_NANOSECOND = NANOSECONDS.convert(1, SECONDS);

    private final Instant startTime;
    private final AtomicReference<Duration> delay = new AtomicReference<>();
    private Duration tickInterval;

    /**
     * Creates a runner with zero delay, starting now.
     *
     * @param s the source to read from
     * @throws IOException if the source does not contain a valid replay
     */
    public RealtimeRunner(Source s) throws IOException {
        this(s, Duration.ZERO);
    }

    /**
     * Creates a runner starting now.
     *
     * @param s the source to read from
     * @param delay the delay added to every tick's scheduled time
     * @throws IOException if the source does not contain a valid replay
     */
    public RealtimeRunner(Source s, Duration delay) throws IOException {
        this(s, delay, now());
    }

    /**
     * @param s the source to read from
     * @param delay the delay added to every tick's scheduled time
     * @param startTime the wall-clock instant at which tick 0 is scheduled (before the delay)
     * @throws IOException if the source does not contain a valid replay
     */
    public RealtimeRunner(Source s, Duration delay, Instant startTime) throws IOException {
        super(s);
        setDelay(delay);
        this.startTime = startTime;
    }

    private boolean canDelay() {
        return loopController.syncTickSeen && tickInterval != null;
    }

    private void delayUntil(int upcomingTick) {
        try {
            while(true) {
                var shouldBeAt = startTime.plus(delay.get()).plus(tickInterval.multipliedBy(upcomingTick));
                var milliDelay = Duration.between(now(), shouldBeAt).toMillis();
                if (milliDelay <= 0L) {
                    return;
                }
                Thread.sleep(Math.min(milliDelay, 250L));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    protected void startNewTick(int upcomingTick) {
        if (canDelay()) {
            delayUntil(upcomingTick);
        }
        super.startNewTick(upcomingTick);
    }

    private void setTickInterval(float tickIntervalFloat) {
        tickInterval = Duration.ofNanos((long) (SECOND_TO_NANOSECOND * tickIntervalFloat));
    }

    @OnMessage(CsgoNetMessages.CSVCMsg_ServerInfo.class)
    protected void onCsgoServerInfo(CsgoNetMessages.CSVCMsg_ServerInfo serverInfo) {
        setTickInterval(serverInfo.getTickInterval());
    }


    @OnMessage(DemoNetMessages.CSVCMsg_ServerInfo.class)
    protected void onDotaServerInfo(DemoNetMessages.CSVCMsg_ServerInfo serverInfo) {
        setTickInterval(serverInfo.getTickInterval());
    }

    /**
     * @return the current delay
     */
    public Duration getDelay() {
        return delay.get();
    }

    /**
     * Sets the delay added to every tick's scheduled time. Safe to call from another thread while the runner is running.
     *
     * @param delay the new delay
     */
    public void setDelay(Duration delay) {
        this.delay.set(delay);
    }

}
