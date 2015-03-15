package skadistats.clarity.two.processor.tempentities;

import skadistats.clarity.model.Entity;
import skadistats.clarity.two.framework.annotation.UsagePointMarker;
import skadistats.clarity.two.framework.invocation.UsagePointType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER, parameterClasses = { Entity.class })
public @interface OnTempEntity {
}
