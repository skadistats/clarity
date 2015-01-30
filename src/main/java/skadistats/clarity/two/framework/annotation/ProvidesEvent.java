package skadistats.clarity.two.framework.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
public @interface ProvidesEvent {
    Class<? extends Annotation>[] value();
}
