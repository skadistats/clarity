package skadistats.clarity.processor.runner;

import com.google.protobuf.CodedInputStream;

public interface Source {

    enum LoopControlCommand {
        CONTINUE, BREAK, FALLTHROUGH
    }

    CodedInputStream stream();
    boolean isTickBorder(int upcomingTick);
    void markFullPacket(int tick, int size, boolean isCompressed);
    LoopControlCommand doLoopControl(Context ctx, int nextTickWithData);

}
