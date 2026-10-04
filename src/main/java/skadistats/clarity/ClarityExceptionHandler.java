package skadistats.clarity;

import java.lang.annotation.Annotation;

/**
 * Receives exceptions thrown by event listeners (processor {@code @On*} methods). Set via
 * {@link skadistats.clarity.processor.runner.AbstractRunner#setExceptionHandler(ClarityExceptionHandler)}.
 * The default handler rethrows the exception, which aborts the run.
 */
public interface ClarityExceptionHandler {

    /**
     * Called when a listener throws.
     *
     * @param eventType the {@code @On*} annotation type of the event
     * @param parameters context of the failure; currently a single element, the index of the listener within the event
     * @param throwable the exception thrown by the listener
     */
    void handleException(Class<? extends Annotation> eventType, Object[] parameters, Throwable throwable);

}
