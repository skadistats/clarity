package skadistats.clarity.processor.entities;

import skadistats.clarity.event.EventBase;
import skadistats.clarity.event.GenerateEvent;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
@GenerateEvent
public @interface OnEntityUpdated {
    String classPattern() default ".*";

    interface Listener {
        void invoke(Entity e, FieldPath[] fps, int n);
    }

    interface Filter {
        boolean test(Entity e, FieldPath[] fps, int n);
    }

    interface Event extends EventBase {
        void raise(Entity e, FieldPath[] fps, int n);
    }
}
