package skadistats.clarity.two.framework.annotation;

import skadistats.clarity.two.framework.invocation.InvocationPointType;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@InvocationPointMarker(InvocationPointType.INITIALIZER)
public @interface Initializer {
    Class<? extends Annotation> value();
}
