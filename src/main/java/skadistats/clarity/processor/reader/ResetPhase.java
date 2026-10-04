package skadistats.clarity.processor.reader;

/**
 * Phases of a reset, which happens when seeking to another tick. Raised through {@link OnReset} in this order.
 */
public enum ResetPhase {
    /** The reset begins. */
    START,
    /** Discard all state that is rebuilt from reset-relevant packets (e.g. string table contents). */
    CLEAR,
    /**
     * A single reset-relevant packet (a string tables packet or the string tables of a full packet) is handed over
     * in the {@code packet} argument; raised once per such packet, in order.
     */
    ACCUMULATE,
    /** Apply the accumulated state; the entities of the last full packet are applied right after this phase. */
    APPLY,
    /** The reset is finished and normal processing resumes. */
    COMPLETE
}
