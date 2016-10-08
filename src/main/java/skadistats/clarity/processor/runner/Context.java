package skadistats.clarity.processor.runner;

import skadistats.clarity.event.Event;
import skadistats.clarity.model.EngineType;

import java.lang.annotation.Annotation;

public class Context {

    private final ExecutionModel executionModel;
    private int buildNumber = -1;

    public Context(ExecutionModel executionModel) {
        this.executionModel = executionModel;
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

    public int getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(int buildNumber) {
        this.buildNumber = buildNumber;
    }

    public <A extends Annotation> Event<A> createEvent(Class<A> eventType, Class... parameterTypes) {
        return executionModel.createEvent(eventType, parameterTypes);
    }

}
