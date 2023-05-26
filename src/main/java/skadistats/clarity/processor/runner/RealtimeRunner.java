package skadistats.clarity.processor.runner;

import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.shared.common.proto.NetMessages;
import skadistats.clarity.wire.csgo.s1.proto.CsGoNetMessages;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static java.time.Instant.now;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;


public class RealtimeRunner extends SimpleRunner {

    private static final long SECOND_TO_NANOSECOND = NANOSECONDS.convert(1, SECONDS);

    private final Instant startTime;
    private AtomicReference<Duration> delay = new AtomicReference<>();
    private Duration tickInterval;

    public RealtimeRunner(Source s) throws IOException {
        this(s, Duration.ZERO);
    }

    public RealtimeRunner(Source s, Duration delay) throws IOException {
        this(s, delay, now());
    }

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
                Instant shouldBeAt = startTime.plus(delay.get()).plus(tickInterval.multipliedBy(upcomingTick));
                long milliDelay = Duration.between(now(), shouldBeAt).toMillis();
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

    @OnMessage(CsGoNetMessages.CSVCMsg_ServerInfo.class)
    protected void onCsgoServerInfo(CsGoNetMessages.CSVCMsg_ServerInfo serverInfo) {
        setTickInterval(serverInfo.getTickInterval());
    }


    @OnMessage(NetMessages.CSVCMsg_ServerInfo.class)
    protected void onDotaServerInfo(NetMessages.CSVCMsg_ServerInfo serverInfo) {
        setTickInterval(serverInfo.getTickInterval());
    }

    public Duration getDelay() {
        return delay.get();
    }

    public void setDelay(Duration delay) {
        this.delay.set(delay);
    }

}
