package skadistats.clarity.processor.runner;

import com.google.protobuf.CodedInputStream;

import java.io.IOException;
import java.io.InputStream;

public class SimpleRunner extends AbstractRunner<SimpleRunner> {

    private final CodedInputStream cis;
    private int tick = -1;

    public SimpleRunner(InputStream inputStream) throws IOException {
        ensureDemHeader(inputStream);
        this.cis = createCodedInputStream(inputStream);
    }

    @Override
    public int getTick() {
        return tick;
    }

    @Override
    protected Source getSource() {
        return new Source() {

            @Override
            public CodedInputStream stream() {
                return cis;
            }

            @Override
            public boolean isTickBorder(int upcomingTick) {
                return tick != upcomingTick;
            }

            @Override
            public LoopControlCommand doLoopControl(int upcomingTick) {
                tick = upcomingTick;
                return LoopControlCommand.FALLTHROUGH;
            }

        };
    }

}
