package skadistats.clarity.two.runner;

import skadistats.clarity.two.framework.annotation.InvocationPointMarker;
import skadistats.clarity.two.framework.invocation.InvocationPointType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@InvocationPointMarker(InvocationPointType.EVENT_LISTENER)
public @interface OnInputStream {
}
