package skadistats.clarity.processor.entities;

import skadistats.clarity.event.UsagePoint;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that a processor class or handler method depends on the {@link Entities} processor, so
 * the runtime registers it. Annotate the class (or method) that uses {@link Entities} or any
 * {@code @OnEntity*} event; obtain the instance with {@code ctx.getProcessor(Entities.class)} or an
 * {@code @Insert} field.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.TYPE, ElementType.METHOD })
@UsagePointMarker(value = UsagePointType.FEATURE)
public @interface UsesEntities {
}
