package skadistats.clarity.processor.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.processor.reader.OnTickEnd;
import skadistats.clarity.processor.reader.OnTickStart;

@Provides({OnTickStart.class, OnTickEnd.class, })
public abstract class AbstractRunner implements Runner {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final EngineType engineType;
    protected Context context;

    /* tick the user is at the end of */
    protected int tick;
    /* tick is synthetic (does not contain replay data) */
    protected boolean synthetic = true;

    public AbstractRunner(EngineType engineType) {
        this.engineType = engineType;
        this.tick = -1;
    }

    private ExecutionModel createExecutionModel(Object... processors) {
        ExecutionModel executionModel = new ExecutionModel(this);
        for (Object p : processors) {
            executionModel.addProcessor(p);
        }
        return executionModel;
    }

    protected void initWithProcessors(Object... processors) {
        ExecutionModel em = createExecutionModel(processors);
        context = new Context(em);
        em.initialize(context);
    }

    protected void endTicksUntil(Context ctx, int untilTick) {
        while (tick < untilTick) {
            ctx.createEvent(OnTickEnd.class, boolean.class).raise(synthetic);
            setTick(tick + 1);
            synthetic = true;
            ctx.createEvent(OnTickStart.class, boolean.class).raise(synthetic);
        }
        ctx.createEvent(OnTickEnd.class, boolean.class).raise(synthetic);
        synthetic = false;
    }

    protected void startNewTick(Context ctx, int upcomingTick) {
        setTick(tick + 1);
        synthetic = tick != upcomingTick;
        ctx.createEvent(OnTickStart.class, boolean.class).raise(synthetic);
    }

    protected void setTick(int tick) {
        this.tick = tick;
    }

    @Override
    public int getTick() {
        return tick;
    }

    @Override
    public EngineType getEngineType() {
        return engineType;
    }

    @Override
    public Context getContext() {
        return context;
    }

}
