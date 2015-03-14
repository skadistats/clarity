package skadistats.clarity.two.processor.stringtables;

import skadistats.clarity.two.framework.annotation.UsagePointMarker;
import skadistats.clarity.two.framework.invocation.UsagePointType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.TYPE, ElementType.METHOD })
@UsagePointMarker(UsagePointType.FEATURE)
public @interface UseStringTable {
    String value();
}
