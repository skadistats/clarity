package skadistats.clarity.event;

import org.atteo.classindex.IndexAnnotated;
import skadistats.clarity.engine.EngineType;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
@IndexAnnotated
public @interface Provides {

    Class<? extends Annotation>[] value();
    EngineType[] engine() default {};

}
