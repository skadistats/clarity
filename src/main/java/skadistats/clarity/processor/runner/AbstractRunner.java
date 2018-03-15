package skadistats.clarity.processor.runner;

import org.slf4j.Logger;
import skadistats.clarity.LogChannel;
import skadistats.clarity.event.Event;
import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.Provides;
import skadistats.clarity.logger.PrintfLoggerFactory;
import skadistats.clarity.model.EngineType;

@Provides({OnInit.class})
public abstract class AbstractRunner implements Runner {

    protected static final Logger log = PrintfLoggerFactory.getLogger(LogChannel.runner);

    @InsertEvent
    private Event<OnInit> evInitRun;

    protected final EngineType engineType;
    protected Context context;

    public AbstractRunner(EngineType engineType) {
        this.engineType = engineType;
    }

    private ExecutionModel createExecutionModel(Object... processors) {
        ExecutionModel executionModel = new ExecutionModel(this);
        addProcessorsToModel(executionModel, processors);
        return executionModel;
    }

    private void addProcessorsToModel(ExecutionModel executionModel, Object[] processors) {
        for (Object p : processors) {
            if (p instanceof Object[]) {
                addProcessorsToModel(executionModel, (Object[]) p);
            } else {
                executionModel.addProcessor(p);
            }
        }
    }

    protected void initWithProcessors(Object... processors) {
        ExecutionModel em = createExecutionModel(processors);
        context = new Context(em);
        em.initialize(context);
        if (evInitRun != null) {
            evInitRun.raise();
        }
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
