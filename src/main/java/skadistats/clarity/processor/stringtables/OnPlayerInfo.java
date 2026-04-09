package skadistats.clarity.processor.stringtables;

import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.model.csgo.PlayerInfoType;
import skadistats.clarity.processor.runner.Runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER, parameterClasses = { int.class, PlayerInfoType.class })
public @interface OnPlayerInfo {

    interface Listener {
        void invoke(int playerIndex, PlayerInfoType info);
    }

    final class Event extends skadistats.clarity.event.Event<OnPlayerInfo> {
        private final Listener[] typedListeners;

        public Event(Runner runner, Class<OnPlayerInfo> eventType, Set<EventListener<OnPlayerInfo>> listeners) {
            super(runner, eventType, listeners);
            var els = listeners();
            typedListeners = new Listener[els.length];
            for (int i = 0; i < els.length; i++) {
                typedListeners[i] = (Listener) els[i].getListenerSam();
            }
        }

        public void raise(int playerIndex, PlayerInfoType info) {
            for (int i = 0; i < typedListeners.length; i++) {
                try {
                    typedListeners[i].invoke(playerIndex, info);
                } catch (Throwable t) {
                    handleListenerException(i, t);
                }
            }
        }
    }
}
