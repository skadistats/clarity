package skadistats.clarity.processor.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.event.Event;

import java.lang.annotation.Annotation;

public class Context {

    private static final Logger log = LoggerFactory.getLogger(Context.class);

    private final ExecutionModel executionModel;
    private int tick = 0;

    public Context(ExecutionModel executionModel) {
        this.executionModel = executionModel;
    }

    public ExecutionModel getExecutionModel() {
        return executionModel;
    }

    public <T> T getProcessor(Class<T> processorClass) {
        return executionModel.getProcessor(processorClass);
    }

    public int getTick() {
        return tick;
    }

    public void setTick(int tick) {
        this.tick = tick;
    }

    public <A extends Annotation> Event<A> createEvent(Class<A> eventType, Class... parameterTypes) {
        return executionModel.createEvent(eventType, parameterTypes);
    }

}
