package skadistats.clarity.two.processor.stringtables;

import skadistats.clarity.two.framework.EnlistmentMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface UseStringtable {
    EnlistmentMode enlist() default EnlistmentMode.SPECIFIED;
    String value();
}
