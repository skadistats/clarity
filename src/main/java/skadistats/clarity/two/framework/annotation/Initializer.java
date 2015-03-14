package skadistats.clarity.two.framework.annotation;

import skadistats.clarity.two.framework.invocation.UsagePoint;
import skadistats.clarity.two.framework.invocation.UsagePointType;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.INITIALIZER, parameterClasses = { UsagePoint.class })
public @interface Initializer {
    Class<? extends Annotation> value();
}
