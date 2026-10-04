package skadistats.clarity.processor.reader;

import skadistats.clarity.event.EventBase;
import skadistats.clarity.event.GenerateEvent;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.wire.shared.demo.proto.Demo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fires for each {@code CDemoFullPacket} read from the replay.
 *
 * <p>Handler signature: {@code void onX([Context ctx,] Demo.CDemoFullPacket packet)}.
 *
 * @see Listener handler signature
 * @see Event event interface
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
@GenerateEvent
public @interface OnFullPacket {

    /** Handler signature for {@link OnFullPacket}; implemented by the runtime. */
    interface Listener {
        void invoke(Demo.CDemoFullPacket packet);
    }

    /** Event interface for {@link OnFullPacket}; implemented by the runtime. */
    interface Event extends EventBase {
        void raise(Demo.CDemoFullPacket packet);
    }
}
