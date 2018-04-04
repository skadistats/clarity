package skadistats.clarity.processor.runner;

import skadistats.clarity.source.Source;

import java.io.IOException;

public class SimpleRunner extends AbstractFileRunner {

    private final LoopController.Func controllerFunc = new LoopController.Func() {
        @Override
        public LoopController.Command doLoopControl(int upcomingTick) {
            if (!loopController.isSyncTickSeen()) {
                if (tick == -1) {
                    startNewTick(0);
                }
                return LoopController.Command.FALLTHROUGH;
            }
            if (upcomingTick != tick) {
                if (upcomingTick != Integer.MAX_VALUE) {
                    endTicksUntil(upcomingTick - 1);
                    startNewTick(upcomingTick);
                } else {
                    endTicksUntil(tick);
                }
            }
            return LoopController.Command.FALLTHROUGH;
        }
    };

    public SimpleRunner(Source s) throws IOException {
        super(s, s.readEngineType());
        this.loopController = new LoopController(controllerFunc);
    }

    public SimpleRunner runWith(final Object... processors) throws IOException {
        initAndRunWith(processors);
        return this;
    }

}
