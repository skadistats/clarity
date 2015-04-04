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
            public LoopController.Command doLoopControl(Context ctx, int upcomingTick) {
                if (upcomingTick != Integer.MAX_VALUE) {
                    endTicksUntil(ctx, upcomingTick - 1);
                    startNewTick(ctx);
                } else {
                    endTicksUntil(ctx, tick);
                }
                return LoopController.Command.FALLTHROUGH;
            }
        };
    }

}
