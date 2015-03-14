package skadistats.clarity.two.processor.stringtables;

import com.google.protobuf.GeneratedMessage;
import skadistats.clarity.two.framework.EnlistmentMode;
import skadistats.clarity.two.framework.annotation.EventMarker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@EventMarker
public @interface UseStringtable {
    EnlistmentMode enlist() default EnlistmentMode.SPECIFIED;
    String value();
}