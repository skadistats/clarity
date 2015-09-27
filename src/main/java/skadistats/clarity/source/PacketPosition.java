package skadistats.clarity.source;


import skadistats.clarity.wire.common.proto.Demo;

public class PacketPosition implements Comparable<PacketPosition> {

    public enum Kind {
        SYNC,
        STRINGTABLE,
        FULL_PACKET
    }

    private final int tick;
    private final Kind kind;
    private final int offset;

    public static PacketPosition createPacketPosition(int tick, int kind, int offset) {
        switch(kind) {
            case Demo.EDemoCommands.DEM_SyncTick_VALUE:
                return new PacketPosition(tick, Kind.SYNC, offset);
            case Demo.EDemoCommands.DEM_StringTables_VALUE:
                return new PacketPosition(tick, Kind.STRINGTABLE, offset);
            case Demo.EDemoCommands.DEM_FullPacket_VALUE:
                return new PacketPosition(tick, Kind.FULL_PACKET, offset);
            default:
                return null;
        }
    }

    private PacketPosition(int tick, Kind kind, int offset) {
        this.tick = tick;
        this.kind = kind;
        this.offset = offset;
    }

    public int getTick() {
        return tick;
    }

    public Kind getKind() {
        return kind;
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public int compareTo(PacketPosition o) {
        int r = Integer.compare(tick, o.tick);
        return r != 0 ? r : o.kind.compareTo(kind);
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

}
