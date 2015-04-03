package skadistats.clarity.processor.runner;

import com.google.protobuf.CodedInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.processor.reader.OnTickEnd;
import skadistats.clarity.processor.reader.OnTickStart;

public abstract class AbstractRunner<T extends Runner> implements Runner<AbstractRunner<T>> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private Context context;

    /* tick the user is at the end of */
    protected int tick = -1;
    /* tick the processor has last processed */
    protected int processorTick = -1;

    protected CodedInputStream cis;

    protected abstract class AbstractSource implements Source {
        @Override
        public CodedInputStream stream() {
            return cis;
        }
        @Override
        public boolean isTickBorder(int upcomingTick) {
            return processorTick != upcomingTick;
        }
        @Override
        public void markFullPacket(int tick, int size, boolean isCompressed) {
            log.debug("mark full packet: tick: {}, offset: {}, size: {}, compressed: {}", tick, cis.getTotalBytesRead(), size, isCompressed);
        }
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
            tick++;
            ctx.createEvent(OnTickStart.class).raise();
        }
        if (tick != -1) {
            ctx.createEvent(OnTickEnd.class).raise();
        }
    }

    protected void startNewTick(Context ctx) {
        tick++;
        ctx.createEvent(OnTickStart.class).raise();
    }

    public int getTick() {
        return tick;
    }

    abstract protected Source getSource();

    @Override
    public AbstractRunner<T> runWith(Object... processors) {
        ExecutionModel em = createExecutionModel(processors);
        context = new Context(em);
        em.initialize(context);
        context.createEvent(OnInputSource.class, Source.class).raise(getSource());
        return this;
    }

    public Context getContext() {
        return context;
    }

}
