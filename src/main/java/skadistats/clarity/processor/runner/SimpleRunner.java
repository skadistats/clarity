package skadistats.clarity.processor.runner;

import skadistats.clarity.source.Source;

import java.io.IOException;

public class SimpleRunner extends AbstractRunner<SimpleRunner> {

    public SimpleRunner(Source s) throws IOException {
        super(s);
        source.ensureDemoHeader();
        source.skipBytes(4);
        this.loopController = new LoopController() {
            @Override
            public boolean isTickBorder(int upcomingTick) {
                return processorTick != upcomingTick;
            }
            @Override
            public LoopController.Command doLoopControl(Context ctx, int upcomingTick) {
                processorTick = upcomingTick;
                if (processorTick != Integer.MAX_VALUE) {
                    endTicksUntil(ctx, processorTick - 1);
                    startNewTick(ctx);
                } else {
                    endTicksUntil(ctx, tick);
                }
                return LoopController.Command.FALLTHROUGH;
            }
        };
    }

}
