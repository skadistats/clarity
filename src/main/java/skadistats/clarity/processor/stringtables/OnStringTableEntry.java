package skadistats.clarity.processor.stringtables;

import com.google.protobuf.ByteString;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.model.StringTable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.TYPE, ElementType.METHOD })
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER, parameterClasses = { StringTable.class, int.class, String.class, ByteString.class })
public @interface OnStringTableEntry {
    String value();
}
