package skadistats.clarity.source;


/**
 * Position of a reset-relevant packet in a {@link Source}: the tick it belongs to, its kind and its byte offset.
 * Used by {@link skadistats.clarity.processor.runner.ControllableRunner} to plan seeks. Ordered by tick, then kind;
 * {@code equals} ignores the offset.
 */
public class PacketPosition implements Comparable<PacketPosition> {

    private final int tick;
    private final ResetRelevantKind kind;
    private final int offset;

    /**
     * @param tick the packet's tick; {@code -1} if before the sync tick
     * @param kind the kind of packet
     * @param offset the byte offset in the source
     * @return the position, or {@code null} if {@code kind} is {@code null}
     */
    public static PacketPosition createPacketPosition(int tick, ResetRelevantKind kind, int offset) {
        if (kind != null) {
            return new PacketPosition(tick, kind, offset);
        }
        else {
            return null;
        }
    }

    private PacketPosition(int tick, ResetRelevantKind kind, int offset) {
        this.tick = tick;
        this.kind = kind;
        this.offset = offset;
    }

    /**
     * @return the tick
     */
    public int getTick() {
        return tick;
    }

    /**
     * @return the kind of packet
     */
    public ResetRelevantKind getKind() {
        return kind;
    }

    /**
     * @return the byte offset in the source
     */
    public int getOffset() {
        return offset;
    }

    @Override
    public int compareTo(PacketPosition o) {
        var r = Integer.compare(tick, o.tick);
        return r != 0 ? r : kind.compareTo(o.kind);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (PacketPosition) o;
        return tick == that.tick && kind == that.kind;
    }


    @Override
    public int hashCode() {
        var result = tick;
        result = 31 * result + kind.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final var sb = new StringBuilder("PacketPosition{");
        sb.append("tick=").append(tick);
        sb.append(", kind=").append(kind);
        sb.append(", offset=").append(offset);
        sb.append('}');
        return sb.toString();
    }
}
