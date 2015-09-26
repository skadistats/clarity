package skadistats.clarity.processor.runner;

import skadistats.clarity.processor.reader.ResetPhase;
import skadistats.clarity.util.Iterators;

import java.io.IOException;
import java.util.Iterator;

public abstract class LoopController {

    public enum Command {
        CONTINUE, BREAK, FALLTHROUGH, RESET_COMPLETE
    }

    abstract public Command doLoopControl(Context ctx, int nextTickWithData);

    public void markCDemoStringTables(int tick, int offset) throws IOException {
    }

    public Iterator<ResetPhase> evaluateResetPhases() throws IOException {
        return Iterators.emptyIterator();
    }

}
