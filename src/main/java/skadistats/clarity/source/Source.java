package skadistats.clarity.source;

import skadistats.clarity.wire.proto.Demo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.TreeSet;

public abstract class Source {

    public abstract int getPosition();
    public abstract void setPosition(int position) throws IOException;

    public abstract byte readByte() throws IOException;
    public abstract void readBytes(byte[] dst, int offset, int len) throws IOException;

    public byte[] readBytes(int size) throws IOException {
        byte[] dst = new byte[size];
        readBytes(dst, 0, size);
        return dst;
    }

    public int readVarInt32() throws IOException {
        byte tmp = readByte();
        if (tmp >= 0) {
            return tmp;
        }
        int result = tmp & 0x7f;
        if ((tmp = readByte()) >= 0) {
            result |= tmp << 7;
        } else {
            result |= (tmp & 0x7f) << 7;
            if ((tmp = readByte()) >= 0) {
                result |= tmp << 14;
            } else {
                result |= (tmp & 0x7f) << 14;
                if ((tmp = readByte()) >= 0) {
                    result |= tmp << 21;
                } else {
                    result |= (tmp & 0x7f) << 21;
                    result |= (tmp = readByte()) << 28;
                    if (tmp < 0) {
                        throw new IOException("malformed varint");
                    }
                }
            }
        }
        return result;
    }

    public int readFixedInt32() throws IOException {
        return ByteBuffer.wrap(readBytes(4)).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get();
    }

    public void skipBytes(int num) throws IOException {
        setPosition(getPosition() + num);
    }

    public void ensureDemoHeader() throws IOException {
        if (!"PBUFDEM\0".equals(new String(readBytes(8)))) {
            throw new IOException("given stream does not seem to contain a valid replay");
        }
    }

    public TreeSet<PacketPosition> getFullPacketsBeforeTick(int wantedTick, TreeSet<PacketPosition> fullPacketPositions) throws IOException {
        int backup = getPosition();
        PacketPosition wanted = new PacketPosition(wantedTick, 0);
        if (fullPacketPositions.tailSet(wanted, true).size() == 0) {
            setPosition(fullPacketPositions.floor(wanted).getOffset());
            while (true) {
                int at = getPosition();
                int kind = readVarInt32() & ~Demo.EDemoCommands.DEM_IsCompressed_VALUE;
                int tick = readVarInt32();
                int size = readVarInt32();
                if (kind == Demo.EDemoCommands.DEM_FullPacket_VALUE) {
                    fullPacketPositions.add(new PacketPosition(tick, at));
                }
                if (tick >= wantedTick) {
                    break;
                }
                skipBytes(size);
            }
        }
        setPosition(backup);
        return new TreeSet<>(fullPacketPositions.headSet(wanted, true));
    }

    public int getLastTick() throws IOException {
        int backup = getPosition();
        setPosition(8);
        setPosition(readFixedInt32());
        readVarInt32();
        int lastTick = readVarInt32();
        setPosition(backup);
        return lastTick;
    }

}
