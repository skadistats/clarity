package skadistats.clarity.two.processor.reader;

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
public @interface OnMessage {
    EnlistmentMode enlist() default EnlistmentMode.SPECIFIED;
    Class<? extends GeneratedMessage> value() default GeneratedMessage.class;
}
