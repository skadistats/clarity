package skadistats.clarity.two.framework.invocation;

import skadistats.clarity.two.framework.annotation.Initializer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public enum InvocationPointType {

    EVENT_LISTENER {
        @Override
        public InvocationPoint newInstance(Annotation annotation, Class<?> processorClass, Method method) {
            return new EventListener(annotation, processorClass, method);
        }
    },

    INITIALIZER {
        @Override
        public InvocationPoint newInstance(Annotation annotation, Class<?> processorClass, Method method) {
            return new InitializerMethod((Initializer) annotation, processorClass, method);
        }
    };

    public abstract InvocationPoint newInstance(Annotation annotation, Class<?> processorClass, Method method);

}
