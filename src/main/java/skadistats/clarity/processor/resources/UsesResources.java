package skadistats.clarity.processor.resources;

import skadistats.clarity.event.UsagePoint;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that the annotated processor class or method needs {@link Resources}. Only applies to Source 2 engines.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.TYPE, ElementType.METHOD })
@UsagePointMarker(value = UsagePointType.FEATURE)
public @interface UsesResources {
}
