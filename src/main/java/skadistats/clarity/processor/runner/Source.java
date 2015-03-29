package skadistats.clarity.processor.runner;

import java.io.InputStream;

public interface Source {

    InputStream getInputStream();
    void setTick(int tick);

}
