package skadistats.clarity.processor.runner;

import skadistats.clarity.engine.EngineType;
import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.s2.S2FieldPathType;
import skadistats.clarity.processor.reader.OnTickEnd;
import skadistats.clarity.processor.reader.OnTickStart;
import skadistats.clarity.source.Source;
import skadistats.clarity.state.s1.S1EntityStateType;
import skadistats.clarity.state.s2.S2EntityStateType;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

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
    protected S2FieldPathType s2FieldPathType = S2FieldPathType.LONG;
    protected Predicate<DTClass> entityFilter;

    /* tick the user is at the end of */
    protected int tick;
    /* tick is synthetic (does not contain replay data) */
    protected boolean synthetic = true;

    public AbstractFileRunner(Source source, EngineType engineType) throws IOException {
        super(engineType);
        this.source = source;
        this.tick = -1;
    }

    @Override
    protected List<Object> infraProcessors() {
        return List.of(this, engineType, engineType.getPacketReader(), source);
    }

    @Override
    protected Context createContext(ExecutionModel em) {
        return new Context(em, s1EntityStateType, s2EntityStateType, s2FieldPathType, entityFilter);
    }

    protected void initAndRunWith(Object... processors) throws IOException {
        initWithProcessors(processors);
        engineType.emitHeader();
        OnInputSource.Event ev = context.createEvent(OnInputSource.class);
        ev.raise(source, loopController);
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

    public AbstractFileRunner withS2FieldPath(S2FieldPathType type) {
        this.s2FieldPathType = type;
        return this;
    }

    public AbstractFileRunner withEntityFilter(Predicate<DTClass> filter) {
        if (context != null) {
            throw new IllegalStateException("entity filter cannot be set after parse has started");
        }
        this.entityFilter = filter;
        return this;
    }

    public int getLastTick() throws IOException {
        return source.getLastTick();
    }

}
