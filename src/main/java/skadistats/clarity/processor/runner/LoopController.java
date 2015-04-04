package skadistats.clarity.processor.runner;

import com.google.common.collect.Iterators;
import skadistats.clarity.processor.reader.ResetPhase;

import java.io.IOException;
import java.util.Iterator;

public abstract class LoopController {

    public enum Command {
        CONTINUE, BREAK, FALLTHROUGH
    }

    abstract public boolean isTickBorder(int upcomingTick);
    abstract public Command doLoopControl(Context ctx, int nextTickWithData);

    public Iterator<ResetPhase> evaluateResetPhases(int tick, int offset) throws IOException {
        return Iterators.emptyIterator();
    }

}
