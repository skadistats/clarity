package skadistats.clarity.processor.runner;

import com.google.protobuf.CodedInputStream;

import java.io.IOException;
import java.io.InputStream;

public class SimpleRunner extends AbstractRunner<SimpleRunner> {

    private final CodedInputStream cis;

    public SimpleRunner(InputStream inputStream) throws IOException {
        ensureDemHeader(inputStream);
        this.cis = createCodedInputStream(inputStream);
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
            public LoopControlCommand doLoopControl(Context ctx, int upcomingTick) {
                if (upcomingTick != Integer.MAX_VALUE) {
                    endTicksUntil(ctx, upcomingTick - 1);
                    startNewTick(ctx);
                } else {
                    endTicksUntil(ctx, tick);
                }
                return LoopControlCommand.FALLTHROUGH;
            }

        };
    }

}
