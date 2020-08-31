package skadistats.clarity;

import java.lang.annotation.Annotation;

public interface ClarityExceptionHandler {

    void handleException(Class<? extends Annotation> eventType, Object[] parameters, Throwable throwable);

}
