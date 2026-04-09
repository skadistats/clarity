package skadistats.clarity.processor.reader;

import com.google.protobuf.GeneratedMessage;
import skadistats.clarity.event.EventBase;
import skadistats.clarity.event.GenerateEvent;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.io.bitstream.BitStream;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER, dynamicParameters = true)
@GenerateEvent(strategy = GenerateEvent.Strategy.BUCKETED)
public @interface OnPostEmbeddedMessage {
    Class<? extends GeneratedMessage> value() default GeneratedMessage.class;

    interface Listener {
        void invoke(GeneratedMessage msg, BitStream bs);
    }

    interface Event extends EventBase {
        void raise(GeneratedMessage msg, BitStream bs);
        boolean isListenedTo(Class<? extends GeneratedMessage> messageClass);
    }
}
