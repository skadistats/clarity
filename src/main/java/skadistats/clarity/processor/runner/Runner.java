package skadistats.clarity.processor.runner;

import skadistats.clarity.ClarityExceptionHandler;
import skadistats.clarity.engine.EngineType;

/**
 * Drives the processing of a replay and gives processors access to its state.
 *
 * <p>Implementations are {@link SimpleRunner}, {@link ControllableRunner} and {@link RealtimeRunner}.
 */
public interface Runner {

    /**
     * @return the {@link Context} of this run; {@code null} before the processors have been initialized by {@code runWith}
     */
    Context getContext();
    /**
     * @return the current tick; {@code -1} before processing has started
     */
    int getTick();
    /**
     * @return the engine type, determined from the magic at the start of the replay
     */
    EngineType getEngineType();
    /**
     * @return the handler that receives exceptions thrown by event listeners
     */
    ClarityExceptionHandler getExceptionHandler();

}
