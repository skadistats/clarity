package skadistats.clarity.source;

import com.google.protobuf.CodedInputStream;
import skadistats.clarity.processor.reader.ResetPhase;
import skadistats.clarity.processor.runner.Context;

import java.io.IOException;
import java.util.Iterator;

public interface Source {

    CodedInputStream stream();
    boolean isTickBorder(int upcomingTick);
    Iterator<ResetPhase> evaluateResetPhases(int tick, int cisOffset) throws IOException;
    LoopControlCommand doLoopControl(Context ctx, int nextTickWithData);

}
