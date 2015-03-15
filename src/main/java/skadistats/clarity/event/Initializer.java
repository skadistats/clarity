package skadistats.clarity.event;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.INITIALIZER, parameterClasses = { UsagePoint.class })
public @interface Initializer {
    Class<? extends Annotation> value();
}
