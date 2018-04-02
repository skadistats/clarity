package skadistats.clarity.event;

import org.atteo.classindex.IndexAnnotated;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.processor.runner.Runner;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
@IndexAnnotated
public @interface Provides {

    Class<? extends Annotation>[] value();
    EngineId[] engine() default {};
    Class<? extends Runner>[] runnerClass() default {};
    int precedence() default 0;

}
