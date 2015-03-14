package skadistats.clarity.two.framework.annotation;

import skadistats.clarity.two.framework.invocation.InvocationPointType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.ANNOTATION_TYPE})
public @interface InvocationPointMarker {
    InvocationPointType value();
}
