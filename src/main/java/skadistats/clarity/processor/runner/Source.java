package skadistats.clarity.processor.runner;

import com.google.protobuf.CodedInputStream;

public interface Source {

    CodedInputStream stream();
    void setTick(int tick);

}
