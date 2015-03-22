package skadistats.clarity.processor.runner;

public class SimpleRunner extends AbstractRunner<SimpleRunner, SimpleRunner.SimpleContext> {

    public static class SimpleContext extends AbstractContext {}

    public SimpleRunner() {
        super(SimpleContext.class);
    }

}
