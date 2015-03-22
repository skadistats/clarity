package skadistats.clarity.processor.runner;

import java.io.InputStream;

public abstract class AbstractRunner<T extends Context> implements Runner {

    private final Class<T> contextClass;

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

    private T createContext(Object... processors) {
        ExecutionModel executionModel = new ExecutionModel();
        executionModel.addProcessor(this);
        for (Object p : processors) {
            executionModel.addProcessor(p);
        }
        T context = instantiateContext();
        context.setExecutionModel(executionModel);
        executionModel.initialize(context);
        return context;
    }

    @Override
    public Context runWith(InputStream is, Object... processors) {
        Context ctx = createContext(processors);
        ctx.createEvent(OnInputStream.class, InputStream.class).raise(is);
        return ctx;
    }

}
