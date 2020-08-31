package skadistats.clarity.processor.runner;

import skadistats.clarity.ClarityExceptionHandler;
import skadistats.clarity.event.Event;
import skadistats.clarity.model.EngineType;

import java.lang.annotation.Annotation;

public class Context {

    private final ExecutionModel executionModel;
    private final ClarityExceptionHandler exceptionHandler;
    private int buildNumber = -1;

    public Context(ExecutionModel executionModel, ClarityExceptionHandler exceptionHandler) {
        this.executionModel = executionModel;
        this.exceptionHandler = exceptionHandler;
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

    public ClarityExceptionHandler getExceptionHandler() {
        return exceptionHandler;
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
