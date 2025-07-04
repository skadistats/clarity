package skadistats.clarity.processor.reader;

import com.google.protobuf.AbstractMessage;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER, parameterClasses = { GeneratedMessage.class })
public @interface OnMessage {
    Class<? extends AbstractMessage> value() default GeneratedMessage.class;
}
