package skadistats.clarity.source;


public class PacketPosition implements Comparable<PacketPosition> {

    private int tick;
    private int offset;

    public PacketPosition(int tick, int offset) {
        this.tick = tick;
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }

    public int getTick() {
        return tick;
    }

    @Override
    public int compareTo(PacketPosition o) {
        return Integer.compare(tick, o.tick);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PacketPosition that = (PacketPosition) o;
        return tick == that.tick;
    }

    @Override
    public int hashCode() {
        return tick;
    }

}
