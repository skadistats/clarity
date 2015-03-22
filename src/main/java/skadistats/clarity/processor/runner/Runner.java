package skadistats.clarity.processor.runner;

import java.io.InputStream;

public interface Runner<R extends Runner, T extends Context> {

    T getContext();
    R runWith(InputStream is, Object...processors);

}
