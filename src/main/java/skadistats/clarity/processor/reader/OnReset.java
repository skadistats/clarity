package skadistats.clarity.processor.reader;

import skadistats.clarity.event.EventBase;
import skadistats.clarity.event.GenerateEvent;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.wire.shared.demo.proto.Demo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
@GenerateEvent
public @interface OnReset {

    interface Listener {
        void invoke(Demo.CDemoStringTables packet, ResetPhase phase);
    }

    interface Event extends EventBase {
        void raise(Demo.CDemoStringTables packet, ResetPhase phase);
    }
}
