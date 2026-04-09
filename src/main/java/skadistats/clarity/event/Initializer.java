package skadistats.clarity.event;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.INITIALIZER)
public @interface Initializer {
    Class<? extends Annotation> value();
}
