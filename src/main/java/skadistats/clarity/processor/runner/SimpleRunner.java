package skadistats.clarity.processor.runner;

public class SimpleRunner extends AbstractRunner {

    public static class SimpleContext extends AbstractContext {}

    public SimpleRunner() {
        super(SimpleContext.class);
    }

}
