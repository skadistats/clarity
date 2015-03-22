package skadistats.clarity.processor.runner;

import java.io.InputStream;

public abstract class AbstractRunner<R extends Runner, T extends Context> implements Runner<AbstractRunner<R,T>,T> {

    private final Class<T> contextClass;
    protected T context;

    public AbstractRunner(Class<T> contextClass) {
        this.contextClass = contextClass;
    }

    protected T instantiateContext() {
        try {
            return contextClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createContext(Object... processors) {
        ExecutionModel executionModel = new ExecutionModel();
        executionModel.addProcessor(this);
        for (Object p : processors) {
            executionModel.addProcessor(p);
        }
        context = instantiateContext();
        context.setExecutionModel(executionModel);
        executionModel.initialize(context);
    }

    @Override
    public AbstractRunner<R, T> runWith(InputStream is, Object... processors) {
        createContext(processors);
        context.createEvent(OnInputStream.class, InputStream.class).raise(is);
        return this;
    }

    public T getContext() {
        return context;
    }

}
