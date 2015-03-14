package skadistats.clarity.two.processor.runner;

import skadistats.clarity.two.framework.annotation.Provides;

import java.io.InputStream;

@Provides({OnInputStream.class})
public class Runner {

    public void runWith(InputStream is, Object... processors) {
        ExecutionModel executionModel = new ExecutionModel();
        executionModel.addProcessor(this);
        for (Object p : processors) {
            executionModel.addProcessor(p);
        }
        Context context = new Context(executionModel);
        executionModel.initialize(context);
        context.createEvent(OnInputStream.class, InputStream.class).raise(is);
    }

}
