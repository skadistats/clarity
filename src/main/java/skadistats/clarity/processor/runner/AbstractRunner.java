package skadistats.clarity.processor.runner;

import org.slf4j.Logger;
import skadistats.clarity.ClarityExceptionHandler;
import skadistats.clarity.LogChannel;
import skadistats.clarity.engine.EngineType;
import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.Provides;
import skadistats.clarity.io.Util;
import skadistats.clarity.logger.PrintfLoggerFactory;

import java.util.List;

/**
 * Base class of all runners: builds the processor set, creates the {@link Context} and raises {@link OnInit}.
 *
 * <p>Processors passed to {@code runWith} may be nested in arrays; arrays are flattened. Only one processor
 * instance per processor class is kept; a later one replaces an earlier one of the same class.
 */
@Provides({OnInit.class})
public abstract class AbstractRunner implements Runner {

    protected static final Logger log = PrintfLoggerFactory.getLogger(LogChannel.runner);

    @InsertEvent
    private OnInit.Event evInitRun;

    protected final EngineType engineType;
    protected Context context;
    protected ClarityExceptionHandler exceptionHandler = (eventType, parameters, throwable) -> Util.uncheckedThrow(throwable);

    /**
     * @param engineType the engine type of the replay
     */
    public AbstractRunner(EngineType engineType) {
        this.engineType = engineType;
    }

    /**
     * @return the processors the runner itself contributes in addition to the user supplied ones
     */
    protected List<Object> infraProcessors() {
        return List.of(this);
    }

    /**
     * Creates the {@link Context} for this run.
     *
     * @param em the execution model holding all processors
     * @return the new context
     */
    protected abstract Context createContext(ExecutionModel em);

    private ExecutionModel createExecutionModel(List<Object> infra, Object[] userProcessors) {
        var executionModel = new ExecutionModel(this);
        for (var p : infra) {
            executionModel.addProcessor(p);
        }
        addProcessorsToModel(executionModel, userProcessors);
        return executionModel;
    }

    private void addProcessorsToModel(ExecutionModel executionModel, Object[] processors) {
        for (var p : processors) {
            if (p instanceof Object[]) {
                addProcessorsToModel(executionModel, (Object[]) p);
            } else {
                executionModel.addProcessor(p);
            }
        }
    }

    /**
     * Registers the infrastructure and user processors, creates the {@link Context}, wires event
     * listeners and raises {@link OnInit}.
     *
     * @param userProcessors the processor instances (arrays are flattened)
     */
    protected void initWithProcessors(Object... userProcessors) {
        var em = createExecutionModel(infraProcessors(), userProcessors);
        context = createContext(em);
        em.initialize(context);
        if (evInitRun != null) {
            evInitRun.raise();
        }
    }

    /**
     * @return the engine type of the replay
     */
    @Override
    public EngineType getEngineType() {
        return engineType;
    }

    /**
     * @return the context of this run; {@code null} before {@code runWith} has initialized the processors
     */
    @Override
    public Context getContext() {
        return context;
    }

    /**
     * @return the current exception handler
     */
    @Override
    public ClarityExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    /**
     * Sets the handler that receives exceptions thrown by event listeners. The default handler rethrows
     * the exception, which aborts the run.
     *
     * @param exceptionHandler the new handler
     */
    public void setExceptionHandler(ClarityExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

}
