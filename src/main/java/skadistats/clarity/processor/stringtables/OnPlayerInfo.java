package skadistats.clarity.processor.stringtables;

import skadistats.clarity.event.EventBase;
import skadistats.clarity.event.GenerateEvent;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.model.cs.PlayerInfoType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fires when a player info entry in the {@code "userinfo"} string table is added, changed or removed.
 * Only provided for CSGO and CS2.
 *
 * <p>Handler signature: {@code void onX([Context ctx,] int playerIndex, PlayerInfoType info)}.
 * {@code playerIndex} is the string table index plus one, which is the player's entity index. {@code info} is
 * {@code null} if the entry was removed or has no data. Not raised if the entry's info is equal to the previous one.
 *
 * @see Listener handler signature
 * @see Event event interface
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
@GenerateEvent
public @interface OnPlayerInfo {

    /** Handler signature for {@link OnPlayerInfo}; implemented by the runtime. */
    interface Listener {
        void invoke(int playerIndex, PlayerInfoType info);
    }

    /** Event interface for {@link OnPlayerInfo}; implemented by the runtime. */
    interface Event extends EventBase {
        void raise(int playerIndex, PlayerInfoType info);
    }
}
