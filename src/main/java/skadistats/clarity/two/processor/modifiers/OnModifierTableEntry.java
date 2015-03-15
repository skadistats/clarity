package skadistats.clarity.two.processor.modifiers;

import com.dota2.proto.DotaModifiers;
import skadistats.clarity.two.framework.annotation.UsagePointMarker;
import skadistats.clarity.two.framework.invocation.UsagePointType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.TYPE, ElementType.METHOD })
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER, parameterClasses = { DotaModifiers.CDOTAModifierBuffTableEntry.class })
public @interface OnModifierTableEntry {
}
