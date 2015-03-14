package skadistats.clarity.two.framework.invocation;

import skadistats.clarity.two.framework.annotation.Initializer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public enum InvocationPointType {

    EVENT_LISTENER {
        @Override
        public InvocationPoint newInstance(Annotation annotation, Class<?> processorClass, Method method, int arity) {
            return new EventListener(annotation, processorClass, method, arity);
        }
    },

    INITIALIZER {
        @Override
        public InvocationPoint newInstance(Annotation annotation, Class<?> processorClass, Method method, int arity) {
            return new InitializerMethod((Initializer) annotation, processorClass, method, arity);
        }
    };

    public abstract InvocationPoint newInstance(Annotation annotation, Class<?> processorClass, Method method, int arity);

}
