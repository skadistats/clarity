package skadistats.clarity.processor.runner;

import skadistats.clarity.event.Event;

import java.lang.annotation.Annotation;

public interface Context {

    void setExecutionModel(ExecutionModel executionModel);

    public <T> T getProcessor(Class<T> processorClass);
    public int getTick();
    public void setTick(int tick);
    public <A extends Annotation> Event<A> createEvent(Class<A> eventType, Class... parameterTypes);

}
