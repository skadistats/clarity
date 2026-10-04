package skadistats.clarity.processor.reader;

import skadistats.clarity.protobuf.GeneratedMessage;
import skadistats.clarity.event.EventBase;
import skadistats.clarity.event.GenerateEvent;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.io.bitstream.BitStream;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fires for an embedded message, after the {@link OnMessage} listeners for it have run.
 *
 * <p>Handler signature: {@code void onX([Context ctx,] M msg, BitStream bs)}, where {@code msg} is the embedded
 * message and {@code bs} is the bit stream of the enclosing container payload. A handler parameter of a subclass
 * of {@code GeneratedMessage} is allowed (only the parameter count is checked at compile time).
 *
 * <p>{@code value()}: embedded message class; matched against the exact class of the message. The default
 * {@code GeneratedMessage.class} matches all embedded messages. User messages unpacked from
 * {@code CSVCMsg_UserMessage} are raised through {@link OnMessage} only.
 *
 * @see Listener handler signature
 * @see Event event interface
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER, dynamicParameters = true)
@GenerateEvent(strategy = GenerateEvent.Strategy.BUCKETED)
public @interface OnPostEmbeddedMessage {
    /** Message class to listen for; see the class description. */
    Class<? extends GeneratedMessage> value() default GeneratedMessage.class;

    /** Handler signature for {@link OnPostEmbeddedMessage}; implemented by the runtime. */
    interface Listener {
        void invoke(GeneratedMessage msg, BitStream bs);
    }

    /** Event interface for {@link OnPostEmbeddedMessage}; implemented by the runtime. */
    interface Event extends EventBase {
        void raise(GeneratedMessage msg, BitStream bs);
        boolean isListenedTo(Class<? extends GeneratedMessage> messageClass);
    }
}
