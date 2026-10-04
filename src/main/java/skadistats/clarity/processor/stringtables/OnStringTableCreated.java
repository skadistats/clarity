package skadistats.clarity.processor.stringtables;

import skadistats.clarity.event.EventBase;
import skadistats.clarity.event.GenerateEvent;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.model.StringTable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fires when a string table is created from a {@code CreateStringTable} message. Only raised for tables
 * that are requested by an {@link OnStringTableEntry} listener or {@code @UsesStringTable}.
 *
 * <p>Handler signature: {@code void onX([Context ctx,] int numTables, StringTable table)}. {@code numTables} is the
 * number of tables created before this one, counting tables that were not raised; {@code table} is the new
 * table, with its initial entries already decoded. {@link OnStringTableEntry} events for those entries follow.
 *
 * @see Listener handler signature
 * @see Event event interface
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
@GenerateEvent
public @interface OnStringTableCreated {

    /** Handler signature for {@link OnStringTableCreated}; implemented by the runtime. */
    interface Listener {
        void invoke(int numTables, StringTable table);
    }

    /** Event interface for {@link OnStringTableCreated}; implemented by the runtime. */
    interface Event extends EventBase {
        void raise(int numTables, StringTable table);
    }
}
