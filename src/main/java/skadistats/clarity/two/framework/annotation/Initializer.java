package skadistats.clarity.two.framework.annotation;

import skadistats.clarity.two.framework.invocation.UsagePointType;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(UsagePointType.INITIALIZER)
public @interface Initializer {
    Class<? extends Annotation> value();
}
