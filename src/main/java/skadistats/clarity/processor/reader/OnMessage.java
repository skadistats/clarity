package skadistats.clarity.processor.reader;

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
 * Fires for a decoded protobuf message: top-level messages other than the packet containers (for example
 * string tables or sync tick), messages unpacked from {@link OnMessageContainer} payloads, and user messages
 * unpacked from {@code CSVCMsg_UserMessage}.
 *
 * <p>Handler signature: {@code void onX([Context ctx,] M msg)}, where {@code M} is {@code GeneratedMessage}
 * or the message class selected by {@code value()}. A handler parameter of a subclass of
 * {@code GeneratedMessage} is allowed (only the parameter count is checked at compile time).
 *
 * <p>{@code value()}: message class to listen for; matched against the exact class of the message, not its
 * superclasses. The default {@code GeneratedMessage.class} matches every message. If no listener
 * selects a top-level message class, the message is skipped without being parsed.
 *
 * @see Listener handler signature
 * @see Event event interface
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER, dynamicParameters = true)
@GenerateEvent(strategy = GenerateEvent.Strategy.BUCKETED)
public @interface OnMessage {
    /** Message class to listen for; see the class description. */
    Class<? extends GeneratedMessage> value() default GeneratedMessage.class;

    /** Handler signature for {@link OnMessage}; implemented by the runtime. */
    interface Listener {
        void invoke(GeneratedMessage msg);
    }

    /** Event interface for {@link OnMessage}; implemented by the runtime. */
    interface Event extends EventBase {
        void raise(GeneratedMessage msg);
        boolean isListenedTo(Class<? extends GeneratedMessage> messageClass);
    }
}
