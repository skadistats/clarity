package skadistats.clarity.processor.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.event.Event;

import java.lang.annotation.Annotation;

public abstract class AbstractContext implements Context {

    protected final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    protected final ExecutionModel executionModel;
    protected int tick = 0;

    public AbstractContext(ExecutionModel executionModel) {
        this.executionModel = executionModel;
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
