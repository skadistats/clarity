package skadistats.clarity.processor.runner;

import java.io.InputStream;

public class SimpleRunner extends AbstractRunner {

    private final InputStream inputStream;

    public SimpleRunner(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    protected Source getSource() {
        return new AbstractSource() {
            @Override
            public InputStream getInputStream() {
                return inputStream;
            }
        };
    }

}
