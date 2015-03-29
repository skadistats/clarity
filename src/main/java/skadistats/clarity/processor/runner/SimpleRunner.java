package skadistats.clarity.processor.runner;

import com.google.protobuf.CodedInputStream;
import skadistats.clarity.processor.reader.OnTickEnd;
import skadistats.clarity.processor.reader.OnTickStart;

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
            public LoopControlCommand doLoopControl(Context ctx, int upcomingTick) {
                if (tick != upcomingTick) {
                    if (tick != -1) {
                        ctx.createEvent(OnTickEnd.class).raise();
                    }
                    tick = upcomingTick;
                    if (tick != Integer.MAX_VALUE){
                        ctx.createEvent(OnTickStart.class).raise();
                    }
                }
                return LoopControlCommand.FALLTHROUGH;
            }

        };
    }

}
