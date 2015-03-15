package skadistats.clarity.processor.gameevents;

import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.model.GameEventDescriptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER, parameterClasses = { GameEventDescriptor.class })
public @interface OnGameEventDescriptor {
    String value() default "";
}
