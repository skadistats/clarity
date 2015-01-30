package skadistats.clarity.two.runner;

import skadistats.clarity.two.framework.EventProviders;
import skadistats.clarity.two.framework.annotation.ProvidesEvent;
import skadistats.clarity.two.processor.reader.event.OnInputStream;

import java.io.InputStream;

@ProvidesEvent({OnInputStream.class})
public class Runner {

    public void runWith(InputStream is, Object processor) {
        EventProviders.scan("skadistats.clarity.two.processor");
        Context c = new Context();
        c.addProcessor(this);
        c.addProcessor(processor);
        c.initialize();
        c.raise(OnInputStream.class, is);
    }

}
