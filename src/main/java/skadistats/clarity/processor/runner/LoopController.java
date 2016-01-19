package skadistats.clarity.processor.runner;

import java.io.IOException;

public class LoopController {

    public enum Command {
        CONTINUE,
        BREAK,
        FALLTHROUGH,
        AGAIN,

        RESET_CLEAR,
        RESET_ACCUMULATE,
        RESET_APPLY,
        RESET_FORWARD, RESET_COMPLETE
    }

    public interface Func {
        Command doLoopControl(Context ctx, int nextTickWithData);
    }

    public LoopController(Func controllerFunc) {
        this.controllerFunc = controllerFunc;
    }

    protected Func controllerFunc;

    public Command doLoopControl(Context ctx, int nextTickWithData) {
        return controllerFunc.doLoopControl(ctx, nextTickWithData);
    }

    public void markResetRelevantPacket(int tick, int kind, int offset) throws IOException {}

    public void markSyncTickSeen() {}

}
