package skadistats.clarity.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.ANNOTATION_TYPE)
public @interface GenerateEvent {

    Strategy strategy() default Strategy.STANDARD;

    enum Strategy {
        STANDARD,
        BUCKETED
    }

}
