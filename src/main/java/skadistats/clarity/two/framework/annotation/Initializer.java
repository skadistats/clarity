package skadistats.clarity.two.framework.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface Initializer {
    Class<? extends Annotation> value();
}
