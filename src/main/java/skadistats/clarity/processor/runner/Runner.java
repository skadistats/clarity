package skadistats.clarity.processor.runner;

import skadistats.clarity.ClarityExceptionHandler;
import skadistats.clarity.engine.EngineType;

public interface Runner {

    Context getContext();
    int getTick();
    EngineType getEngineType();
    ClarityExceptionHandler getExceptionHandler();

}
