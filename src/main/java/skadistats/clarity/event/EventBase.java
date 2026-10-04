package skadistats.clarity.event;

/**
 * Common supertype of {@link Event} and of the nested {@code Event} interfaces of event annotations.
 */
public interface EventBase {
    boolean isListenedTo();
}
