package skadistats.clarity.processor.runner;

import skadistats.clarity.model.EngineType;
import skadistats.clarity.source.Source;

public interface Runner<T extends Runner> {

    Context getContext();
    int getTick();
    Source getSource();
    EngineType getEngineType();

    T runWith(Object... processors);

}
