package skadistats.clarity.processor.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.processor.reader.OnTickEnd;
import skadistats.clarity.processor.reader.OnTickStart;
import skadistats.clarity.source.Source;

import java.io.IOException;

public abstract class AbstractRunner<T extends Runner> implements Runner<AbstractRunner<T>> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final Source source;
    protected final EngineType engineType;

    protected LoopController loopController;
    private Context context;

    /* tick the user is at the end of */
    protected int tick;
    /* tick is synthetic (does not contain replay data) */
    protected boolean synthetic = true;

    public AbstractRunner(Source source, EngineType engineType) throws IOException {
        this.source = source;
        this.engineType = engineType;
        this.tick = -1;
        engineType.skipHeaderOffsets(source);
    }

    protected ExecutionModel createExecutionModel(Object... processors) {
        ExecutionModel executionModel = new ExecutionModel(this);
        for (Object p : processors) {
            executionModel.addProcessor(p);
        }
        return executionModel;
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
    public AbstractRunner<T> runWith(Object... processors) {
        ExecutionModel em = createExecutionModel(processors);
        context = new Context(em);
        em.initialize(context);
        context.createEvent(OnInputSource.class, Source.class, LoopController.class).raise(source, loopController);
        return this;
    }

    public Context getContext() {
        return context;
    }

    public Source getSource() {
        return source;
    }

}
