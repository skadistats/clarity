package skadistats.clarity.processor.reader;

import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.processor.runner.Runner;
import skadistats.clarity.wire.shared.demo.proto.Demo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER, parameterClasses = { Demo.CDemoFullPacket.class })
public @interface OnFullPacket {

    interface Listener {
        void invoke(Demo.CDemoFullPacket packet);
    }

    final class Event extends skadistats.clarity.event.Event<OnFullPacket> {
        private final Listener[] typedListeners;

        public Event(Runner runner, Class<OnFullPacket> eventType, Set<EventListener<OnFullPacket>> listeners) {
            super(runner, eventType, listeners);
            var els = listeners();
            typedListeners = new Listener[els.length];
            for (int i = 0; i < els.length; i++) {
                typedListeners[i] = (Listener) els[i].getListenerSam();
            }
        }

        public void raise(Demo.CDemoFullPacket packet) {
            for (int i = 0; i < typedListeners.length; i++) {
                try {
                    typedListeners[i].invoke(packet);
                } catch (Throwable t) {
                    handleListenerException(i, t);
                }
            }
        }
    }
}
