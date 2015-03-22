package skadistats.clarity.processor.runner;

import java.io.InputStream;

public interface Runner {

    Context runWith(InputStream is, Object...processors);

}
