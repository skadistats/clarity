package skadistats.clarity.event;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class InitializerMethod extends AbstractInvocationPoint<Initializer> {

    public InitializerMethod(Initializer annotation, Class<?> processorClass, Method method, UsagePointMarker marker) {
        super(annotation, processorClass, method, marker);
    }

    @Override
    public Class<? extends Annotation> getUsagePointClass() {
        return annotation.value();
    }

}
