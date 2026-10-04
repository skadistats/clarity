package skadistats.clarity.processor.reader;

import skadistats.clarity.protobuf.ByteString;
import skadistats.clarity.protobuf.GeneratedMessage;
import skadistats.clarity.event.EventBase;
import skadistats.clarity.event.GenerateEvent;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fires for a container message whose payload holds embedded messages: {@code CDemoPacket}, {@code CDemoSendTables}
 * (engines where send tables come in a container), and the packet of a {@code CDemoFullPacket} that is
 * applied after a reset.
 *
 * <p>Handler signature: {@code void onX([Context ctx,] Class clazz, ByteString bytes)}, where {@code clazz} is
 * the class of the container message (for example {@code Demo.CDemoPacket.class}) and {@code bytes} its payload.
 *
 * <p>{@code value()}: container class; a listener is called if {@code value()} is assignable from {@code clazz}.
 * The default {@code GeneratedMessage.class} matches all containers.
 *
 * @see Listener handler signature
 * @see Filter optional filter for handlers
 * @see Event event interface
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
@GenerateEvent
public @interface OnMessageContainer {
    /** Container class to listen for; see the class description. */
    Class<? extends GeneratedMessage> value() default GeneratedMessage.class;

    /** Handler signature for {@link OnMessageContainer}; implemented by the runtime. */
    interface Listener {
        void invoke(Class clazz, ByteString bytes);
    }

    /** Optional filter for {@link OnMessageContainer} handlers. */
    interface Filter {
        boolean test(Class clazz, ByteString bytes);
    }

    /** Event interface for {@link OnMessageContainer}; implemented by the runtime. */
    interface Event extends EventBase {
        void raise(Class clazz, ByteString bytes);
    }
}
