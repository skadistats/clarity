package skadistats.clarity.processor.runner;

import skadistats.clarity.event.Event;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.model.engine.ContextData;

import java.lang.annotation.Annotation;

public class Context {

    private final ExecutionModel executionModel;
    private final ContextData contextData;

    public Context(ExecutionModel executionModel, ContextData contextData) {
        this.executionModel = executionModel;
        this.contextData = contextData;
    }

    public <T> T getProcessor(Class<T> processorClass) {
        return executionModel.getProcessor(processorClass);
    }

    public int getTick() {
        return executionModel.getRunner().getTick();
    }

    public EngineType getEngineType() {
        return executionModel.getRunner().getEngineType();
    }

    public float getMillisPerTick() {
        return contextData.getMillisPerTick();
    }

    public int getBuildNumber() {
        return contextData.getBuildNumber();
    }

    public int getGameVersion() {
        return contextData.getGameVersion();
    }

    public <A extends Annotation> Event<A> createEvent(Class<A> eventType, Class... parameterTypes) {
        return executionModel.createEvent(eventType, parameterTypes);
    }

}
