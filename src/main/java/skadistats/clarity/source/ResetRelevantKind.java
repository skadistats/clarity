package skadistats.clarity.source;

/**
 * Kinds of packets that carry state a seek must restore.
 */
public enum ResetRelevantKind {
    /** A string table snapshot. */
    STRINGTABLE,
    /** A full entity snapshot. */
    FULL_PACKET,
    /** The sync packet that starts the tick numbering. */
    SYNC
}
