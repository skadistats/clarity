package skadistats.clarity.processor.stringtables;

import skadistats.clarity.protobuf.ByteString;
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
 * Fires for an entry of a string table: for each entry decoded from a create or update message, and for all
 * entries when a full {@code CDemoStringTables} message is applied (also at the end of a reset).
 *
 * <p>Handler signature: {@code void onX([Context ctx,] StringTable table, int index, String key, ByteString value)}.
 * {@code value} is {@code null} if the entry has no data.
 *
 * <p>{@code value()}: name of the string table (exact match), or {@code "*"} for all tables. Required. Only tables
 * that are requested this way (or by {@code @UsesStringTable}) are decoded.
 *
 * @see Listener handler signature
 * @see Filter optional filter for handlers
 * @see Event event interface
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.TYPE, ElementType.METHOD })
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
@GenerateEvent
public @interface OnStringTableEntry {
    /** Name of the string table, or {@code "*"} for all tables. */
    String value();

    /** Handler signature for {@link OnStringTableEntry}; implemented by the runtime. */
    interface Listener {
        void invoke(StringTable table, int index, String key, ByteString value);
    }

    /** Optional filter for {@link OnStringTableEntry} handlers. */
    interface Filter {
        boolean test(StringTable table, int index, String key, ByteString value);
    }

    /** Event interface for {@link OnStringTableEntry}; implemented by the runtime. */
    interface Event extends EventBase {
        void raise(StringTable table, int index, String key, ByteString value);
    }
}
