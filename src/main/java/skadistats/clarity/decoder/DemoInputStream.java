package skadistats.clarity.decoder;

import com.google.protobuf.CodedInputStream;
import skadistats.clarity.wire.proto.Demo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.TreeSet;

public class DemoInputStream {

    public static class PacketPosition implements Comparable<PacketPosition> {
        private int tick;
        private int offset;

        public PacketPosition(int tick, int offset) {
            this.tick = tick;
            this.offset = offset;
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

        public int getOffset() {
            return offset;
        }

        public int getTick() {
            return tick;
        }
    }

    private final InputStream stream;
    private int offset = 0;

    public DemoInputStream(InputStream stream) {
        this.stream = stream;
    }

    public void close() throws IOException {
        stream.close();
    }

    public CodedInputStream newCodedInputStream() {
        CodedInputStream cis = CodedInputStream.newInstance(stream);
        cis.setSizeLimit(Integer.MAX_VALUE);
        return cis;
    }

    public int getOffset() {
        return offset;
    }

    public byte readByte() throws IOException {
        int i = stream.read();
        if (i == -1) {
            throw new IOException("unexpected end of file!");
        }
        offset++;
        return (byte) i;
    }

    public byte[] readBytes(int size) throws IOException {
        byte[] result = new byte[size];
        int n = size;
        while (n > 0) {
            int r = stream.read(result, size - n, n);
            if (r == -1) {
                throw new IOException("unexpected end of file!");
            }
            offset += r;
            n -= r;
        }
        return result;
    }

    public void skipBytes(int num) throws IOException {
        int n = num;
        while (n > 0) {
            long r = stream.skip(n);
            if (r == 0) {
                throw new IOException("unexpected end of file!");
            }
            offset += r;
            n -= r;
        }
    }

    public int readRawVarint32() throws IOException {
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
                        // Discard upper 32 bits.
                        for (int i = 0; i < 5; i++) {
                            if (readByte() >= 0) {
                                return result;
                            }
                        }
                        throw new IOException("malformed varint");
                    }
                }
            }
        }
        return result;
    }

    public int ensureDemHeader() throws IOException {
        byte[] header = readBytes(12);
        if (!"PBUFDEM\0".equals(new String(Arrays.copyOfRange(header, 0, 8)))) {
            throw new IOException("given stream does not seem to contain a valid replay");
        }
        return ByteBuffer.wrap(header, 8, 4).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get();
    }

    public PacketPosition getFullPacketBeforeTick(int wantedTick, TreeSet<PacketPosition> fullPacketPositions) throws IOException {
        if (offset != 0) {
            throw new IOException("must be at the beginning of the stream");
        }
        PacketPosition cur = fullPacketPositions.floor(new PacketPosition(wantedTick, 0));
        if (cur == null) {
            throw new IOException("never seen a full packet below tick " + wantedTick);
        }
        skipBytes(cur.offset);
        while (true) {
            int at = offset;
            int kind = readRawVarint32() & ~Demo.EDemoCommands.DEM_IsCompressed_VALUE;
            int tick = readRawVarint32();
            int size = readRawVarint32();
            if (tick >= wantedTick) {
                return cur;
            }
            if (kind == Demo.EDemoCommands.DEM_FullPacket_VALUE) {
                cur = new PacketPosition(tick, at);
                fullPacketPositions.add(cur);
            }
            skipBytes(size);
        }
    }

}
