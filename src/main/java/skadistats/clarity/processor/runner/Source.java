package skadistats.clarity.processor.runner;

import com.google.protobuf.CodedInputStream;
import skadistats.clarity.processor.reader.ResetPhase;

import java.io.IOException;
import java.util.Iterator;

public interface Source {

    enum LoopControlCommand {
        CONTINUE, BREAK, FALLTHROUGH
    }

    CodedInputStream stream();
    boolean isTickBorder(int upcomingTick);
    Iterator<ResetPhase> evaluateResetPhases(int tick, int cisOffset) throws IOException;
    LoopControlCommand doLoopControl(Context ctx, int nextTickWithData);

}
