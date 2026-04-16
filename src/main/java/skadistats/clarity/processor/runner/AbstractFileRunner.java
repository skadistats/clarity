package skadistats.clarity.processor.runner;

import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.model.state.EntityStateFactory;
import skadistats.clarity.model.state.S1EntityStateType;
import skadistats.clarity.model.state.S2EntityStateType;
import skadistats.clarity.processor.reader.OnTickEnd;
import skadistats.clarity.processor.reader.OnTickStart;
import skadistats.clarity.source.Source;

import java.io.IOException;

@Provides(value = {OnInputSource.class, OnTickStart.class, OnTickEnd.class}, runnerClass = { AbstractFileRunner.class })
public abstract class AbstractFileRunner extends AbstractRunner implements FileRunner {

    @InsertEvent
    private OnTickStart.Event evTickStart;
    @InsertEvent
    private OnTickEnd.Event evTickEnd;

    protected final Source source;
    protected LoopController loopController;
    protected S1EntityStateType s1EntityStateType = S1EntityStateType.FLAT;
    protected S2EntityStateType s2EntityStateType = S2EntityStateType.NESTED_ARRAY;

    /* tick the user is at the end of */
    protected int tick;
    /* tick is synthetic (does not contain replay data) */
    protected boolean synthetic = true;

    public AbstractFileRunner(Source source, EngineType engineType) throws IOException {
        super(engineType);
        this.source = source;
        this.tick = -1;
    }

    protected void initAndRunWith(Object... processors) throws IOException {
        var entityStateFactory = new EntityStateFactory(s1EntityStateType, s2EntityStateType);
        initWithProcessors(this, getEngineType().getRegisteredProcessors(), source, entityStateFactory, processors);
        engineType.emitHeader();
        ((OnInputSource.Event) context.createEvent(OnInputSource.class)).raise(source, loopController);
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

    public AbstractFileRunner withS1EntityState(S1EntityStateType type) {
        this.s1EntityStateType = type;
        return this;
    }

    public AbstractFileRunner withS2EntityState(S2EntityStateType type) {
        this.s2EntityStateType = type;
        return this;
    }

    public int getLastTick() throws IOException {
        return source.getLastTick();
    }

}
