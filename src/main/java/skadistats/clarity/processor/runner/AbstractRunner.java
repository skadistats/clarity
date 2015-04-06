package skadistats.clarity.processor.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.processor.reader.OnTickEnd;
import skadistats.clarity.processor.reader.OnTickStart;
import skadistats.clarity.source.Source;

public abstract class AbstractRunner<T extends Runner> implements Runner<AbstractRunner<T>> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final Source source;
    protected LoopController loopController;
    private Context context;

    /* tick the user is at the end of */
    protected int tick = -1;

    public AbstractRunner(Source source) {
        this.source = source;
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
            if (tick != -1) {
                ctx.createEvent(OnTickEnd.class).raise();
            }
            setTick(tick + 1);
            ctx.createEvent(OnTickStart.class).raise();
        }
        if (tick != -1) {
            ctx.createEvent(OnTickEnd.class).raise();
        }
    }

    protected void startNewTick(Context ctx) {
        setTick(tick + 1);
        ctx.createEvent(OnTickStart.class).raise();
    }

    protected void setTick(int tick) {
        this.tick = tick;
    }

    public int getTick() {
        return tick;
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

}
