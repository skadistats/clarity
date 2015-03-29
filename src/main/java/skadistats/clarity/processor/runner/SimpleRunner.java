package skadistats.clarity.processor.runner;

import com.google.protobuf.CodedInputStream;

import java.io.IOException;
import java.io.InputStream;

public class SimpleRunner extends AbstractRunner {

    private final CodedInputStream cis;

    public SimpleRunner(InputStream inputStream) throws IOException {
        ensureDemHeader(inputStream);
        this.cis = createCodedInputStream(inputStream);
    }

    @Override
    protected Source getSource() {
        return new AbstractSource() {
            @Override
            public CodedInputStream stream() {
                return cis;
            }
        };
    }

}
