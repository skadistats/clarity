package skadistats.clarity.processor.reader;

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessage;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER, parameterClasses = { Class.class, ByteString.class })
public @interface OnMessageContainer {
    Class<? extends GeneratedMessage> value() default GeneratedMessage.class;
}
