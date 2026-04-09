package skadistats.clarity.processor.stringtables;

import com.google.protobuf.ByteString;
import skadistats.clarity.event.EventBase;
import skadistats.clarity.event.GenerateEvent;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.model.StringTable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.TYPE, ElementType.METHOD })
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
@GenerateEvent
public @interface OnStringTableEntry {
    String value();

    interface Listener {
        void invoke(StringTable table, int index, String key, ByteString value);
    }

    interface Filter {
        boolean test(StringTable table, int index, String key, ByteString value);
    }

    interface Event extends EventBase {
        void raise(StringTable table, int index, String key, ByteString value);
    }
}
