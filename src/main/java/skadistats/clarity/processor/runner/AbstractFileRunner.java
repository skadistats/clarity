package skadistats.clarity.processor.runner;

import skadistats.clarity.event.Event;
import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.processor.reader.OnTickEnd;
import skadistats.clarity.processor.reader.OnTickStart;
import skadistats.clarity.source.Source;

import java.io.IOException;

@Provides(value = {OnInputSource.class, OnTickStart.class, OnTickEnd.class}, runnerClass = { AbstractFileRunner.class })
public abstract class AbstractFileRunner extends AbstractRunner implements FileRunner {

    @InsertEvent
    private Event<OnTickStart> evTickStart;
    @InsertEvent
    private Event<OnTickEnd> evTickEnd;

    protected final Source source;
    protected LoopController loopController;

    /* tick the user is at the end of */
    protected int tick;
    /* tick is synthetic (does not contain replay data) */
    protected boolean synthetic = true;

    public AbstractFileRunner(Source source, EngineType engineType) throws IOException {
        super(engineType);
        engineType.readHeader(source);
        this.source = source;
        this.tick = -1;
    }

    protected void initAndRunWith(Object... processors) throws IOException {
        initWithProcessors(this, getEngineType(), source, processors);
        engineType.emitHeader();
        context.createEvent(OnInputSource.class, Source.class, LoopController.class).raise(source, loopController);
    }

    protected void endTicksUntil(int untilTick) {
        while (tick < untilTick) {
            evTickEnd.raise(synthetic);
            setTick(tick + 1);
            synthetic = true;
            evTickStart.raise(synthetic);
        }
        evTickEnd.raise(synthetic);
        synthetic = false;
    }

    protected void startNewTick(int upcomingTick) {
        setTick(tick + 1);
        synthetic = tick != upcomingTick;
        evTickStart.raise(synthetic);
    }

    protected void setTick(int tick) {
        this.tick = tick;
    }

    @Override
    public int getTick() {
        return tick;
    }

    @Override
    public Source getSource() {
        return source;
    }

    public int getLastTick() throws IOException {
        return source.getLastTick();
    }

}
