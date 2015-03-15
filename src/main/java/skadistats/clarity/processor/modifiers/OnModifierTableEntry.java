package skadistats.clarity.processor.modifiers;

import com.dota2.proto.DotaModifiers;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.TYPE, ElementType.METHOD })
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER, parameterClasses = { DotaModifiers.CDOTAModifierBuffTableEntry.class })
public @interface OnModifierTableEntry {
}
