package skadistats.clarity.processor.runner;

import skadistats.clarity.ClarityExceptionHandler;
import skadistats.clarity.model.EngineType;

public interface Runner {

    Context getContext();
    int getTick();
    EngineType getEngineType();
    ClarityExceptionHandler getExceptionHandler();

}
