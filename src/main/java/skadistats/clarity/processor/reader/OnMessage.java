package skadistats.clarity.processor.reader;

import com.google.protobuf.GeneratedMessage;
import skadistats.clarity.event.EventBase;
import skadistats.clarity.event.GenerateEvent;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER, dynamicParameters = true)
@GenerateEvent(strategy = GenerateEvent.Strategy.BUCKETED)
public @interface OnMessage {
    Class<? extends GeneratedMessage> value() default GeneratedMessage.class;

    interface Listener {
        void invoke(GeneratedMessage msg);
    }

    interface Event extends EventBase {
        void raise(GeneratedMessage msg);
        boolean isListenedTo(Class<? extends GeneratedMessage> messageClass);
    }
}
