package skadistats.clarity.source;


public class PacketPosition implements Comparable<PacketPosition> {

    private final int tick;
    private final ResetRelevantKind kind;
    private final int offset;

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

    public int getTick() {
        return tick;
    }

    public ResetRelevantKind getKind() {
        return kind;
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public int compareTo(PacketPosition o) {
        int r = Integer.compare(tick, o.tick);
        return r != 0 ? r : kind.compareTo(o.kind);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PacketPosition that = (PacketPosition) o;
        return tick == that.tick && kind == that.kind;
    }


    @Override
    public int hashCode() {
        int result = tick;
        result = 31 * result + kind.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("PacketPosition{");
        sb.append("tick=").append(tick);
        sb.append(", kind=").append(kind);
        sb.append(", offset=").append(offset);
        sb.append('}');
        return sb.toString();
    }
}
