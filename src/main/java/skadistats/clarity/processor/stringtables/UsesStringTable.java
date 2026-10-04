package skadistats.clarity.processor.stringtables;

import skadistats.clarity.event.UsagePoint;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that the annotated processor class or method needs the string table named by {@link #value()}.
 * <p>
 * This activates the string table processing and makes the table available through
 * {@link StringTables#forName(String)}; without it, tables that nobody requested are not tracked.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.TYPE, ElementType.METHOD })
@UsagePointMarker(value = UsagePointType.FEATURE)
public @interface UsesStringTable {
    /**
     * @return the name of the required string table, e.g. {@code "userinfo"}; {@code "*"} requests all tables
     */
    String value();
}
