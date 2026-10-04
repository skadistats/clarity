package skadistats.clarity.processor.sendtables;

import skadistats.clarity.event.UsagePoint;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that the annotated processor class or method needs {@link DTClasses}. This activates the DT class
 * emitter of the replay's engine.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.TYPE, ElementType.METHOD })
@UsagePointMarker(value = UsagePointType.FEATURE)
public @interface UsesDTClasses {
}
