package skadistats.clarity.two.framework.invocation;

import skadistats.clarity.two.framework.annotation.Initializer;
import skadistats.clarity.two.framework.annotation.UsagePointMarker;

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
