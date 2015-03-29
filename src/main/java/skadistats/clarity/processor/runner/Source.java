package skadistats.clarity.processor.runner;

import com.google.protobuf.CodedInputStream;

public interface Source {

    enum LoopControlCommand {
        CONTINUE, BREAK, FALLTHROUGH
    }

    CodedInputStream stream();
    boolean isTickBorder(int upcomingTick);
    LoopControlCommand doLoopControl(int nextTickWithData);

}
