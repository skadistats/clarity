package skadistats.clarity.processor.runner;

import java.io.InputStream;

public class SimpleRunner extends AbstractRunner<InputStream> {

    @Override
    protected Source getSource(final InputStream input) {
        return new AbstractSource<InputStream>() {
            @Override
            public InputStream getInputStream() {
                return input;
            }
        };
    }

}
