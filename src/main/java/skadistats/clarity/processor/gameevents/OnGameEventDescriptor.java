package skadistats.clarity.processor.gameevents;

import skadistats.clarity.event.EventBase;
import skadistats.clarity.event.GenerateEvent;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.model.GameEventDescriptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
@GenerateEvent
public @interface OnGameEventDescriptor {
    String value() default "";

    interface Listener {
        void invoke(GameEventDescriptor e);
    }

    interface Filter {
        boolean test(GameEventDescriptor e);
    }

    interface Event extends EventBase {
        void raise(GameEventDescriptor e);
    }
}
