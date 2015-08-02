package skadistats.clarity.processor.runner;

import skadistats.clarity.engine.EngineType;

public interface Runner<T extends Runner> {

    Context getContext();
    int getTick();
    EngineType getEngineType();

    T runWith(Object... processors);


}
