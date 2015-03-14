package skadistats.clarity.two.runner;

import skadistats.clarity.two.framework.annotation.Provides;

import java.io.InputStream;

@Provides({OnInputStream.class})
public class Runner {

    public void runWith(InputStream is, Object processor) {
        Context c = new Context();
        c.addProcessor(this);
        c.addProcessor(processor);
        c.initialize();
        c.createEvent(OnInputStream.class, InputStream.class).raise(is);
    }

}
