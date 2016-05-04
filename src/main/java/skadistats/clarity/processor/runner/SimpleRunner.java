package skadistats.clarity.processor.runner;

import skadistats.clarity.source.Source;

import java.io.IOException;

public class SimpleRunner extends AbstractRunner<SimpleRunner> {

    private final LoopController.Func controllerFunc = new LoopController.Func() {
        @Override
        public LoopController.Command doLoopControl(Context ctx, int upcomingTick) {
            if (!loopController.isSyncTickSeen()) {
                if (tick == -1) {
                    startNewTick(ctx, 0);
                }
                return LoopController.Command.FALLTHROUGH;
            }
            if (upcomingTick != tick) {
                if (upcomingTick != Integer.MAX_VALUE) {
                    endTicksUntil(ctx, upcomingTick - 1);
                    startNewTick(ctx, upcomingTick);
                } else {
                    endTicksUntil(ctx, tick);
                }
            }
            return LoopController.Command.FALLTHROUGH;
        }
    };

    public SimpleRunner(Source s) throws IOException {
        super(s, s.readEngineType());
        this.loopController = new LoopController(controllerFunc);
    }

}
