package skadistats.clarity.processor.runner;

import skadistats.clarity.event.Provides;

import java.io.InputStream;

@Provides({OnInputStream.class})
public class SimpleRunner {

    public class Context extends AbstractContext {
        public Context(ExecutionModel executionModel) {
            super(executionModel);
        }
    }

    public Context runWith(InputStream is, Object... processors) {
        ExecutionModel executionModel = new ExecutionModel();
        executionModel.addProcessor(this);
        for (Object p : processors) {
            executionModel.addProcessor(p);
        }
        Context context = new Context(executionModel);
        executionModel.initialize(context);
        context.createEvent(OnInputStream.class, InputStream.class).raise(is);
        return context;
    }

}
