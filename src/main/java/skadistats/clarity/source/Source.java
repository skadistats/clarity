package skadistats.clarity.source;

import skadistats.clarity.model.EngineType;
import skadistats.clarity.wire.common.proto.Demo;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.TreeSet;

/**
 * A Source provides clarity with raw replay data.
 *
 * <p> At the moment, clarity supplies the following implementations:
 *
 * <ul>
 *     <li>{@link InputStreamSource}
 *         <p> This will allow clarity to read from an InputStream, for example from System.in or from sockets.
 *         It will not allow seeking backwards, so it is limited when using it with a {@link skadistats.clarity.processor.runner.ControllableRunner}
 *     </li>
 *     <li>{@link MappedFileSource}
 *         <p> This uses the operating systems memory mapping functions to map a file into memory.
 *         If the replay you are processing is local, it is strongly advised to use this implementation.
 *     </li>
 * </ul>
 */
public abstract class Source {

    protected Integer lastTick;


    /**
     * returns the current position
     *
     * @return the position
     */
    public abstract int getPosition();

    /**
     * sets the current position
     *
     * <p> depending on implementation, this might fail when setting a position lower than the current one.
     *
     * @param position the new position
     * @throws IOException if the supplied positions is invalid
     */
    public abstract void setPosition(int position) throws IOException;

    /**
     * reads a byte from the current position
     *
     * @throws IOException if the data cannot be read
     */
    public abstract byte readByte() throws IOException;

    /**
     * reads {@code len} bytes and puts them into {@code dst} at the specified {@code offset}
     *
     * @param dest the byte array to write the data to
     * @param offset the offset in the byte array where the first byte will be written
     * @param length the number of bytes to read
     * @throws IOException if the data cannot be read
     */
    public abstract void readBytes(byte[] dest, int offset, int length) throws IOException;

    /**
     * allocates a new byte array with the specified {@code length} bytes and fills it with data
     *
     * @param length the number of bytes to read
     * @return the filled byte array
     * @throws IOException if the data cannot be read
     */
    public byte[] readBytes(int length) throws IOException {
        byte[] dst = new byte[length];
        readBytes(dst, 0, length);
        return dst;
    }

    /**
     * reads a variable int32 from the current position
     *
     * @return the int
     * @throws IOException if the data cannot be read, or is not a valid variable int32
     */
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
                        throw new IOException("malformed varint detected");
                    }
                }
            }
        }
        return result;
    }

    /**
     * reads a fixed int32 from the current position
     *
     * @return the int
     * @throws IOException if the data cannot be read
     */
    public int readFixedInt32() throws IOException {
        return ByteBuffer.wrap(readBytes(4)).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get();
    }

    /**
     * skips over a number of bytes
     *
     * @param num number of bytes to skip
     * @throws IOException if there is not enough data left
     */
    public void skipBytes(int num) throws IOException {
        setPosition(getPosition() + num);
    }

    /**
     * skips over a variable int32
     *
     * @throws IOException if there is not enough data left, or the data is malformed
     */
    public void skipVarInt32() throws IOException {
        if (readByte() >= 0) return;
        if (readByte() >= 0) return;
        if (readByte() >= 0) return;
        if (readByte() >= 0) return;
        if (readByte() >= 0) return;
        throw new IOException("malformed varint detected");
    }

    /**
     * reads the magic of a demo file, identifying the engine type
     *
     * @throws IOException if there is not enough data or the if no valid magic was found
     */
    public EngineType readEngineType() throws IOException {
        try {
            EngineType et = EngineType.forMagic(new String(readBytes(8)));
            if (et == null) {
                throw new IOException();
            }
            return et;
        } catch (IOException e) {
            throw new IOException("given stream does not seem to contain a valid replay");
        }
    }

    /**
     * gets the positions of all packets containing full string tables up to a given tick
     *
     * @param engineType
     * @param wantedTick the tick
     * @param packetPositions a set of positions found in previous runs
     * @throws IOException if the underlying data is invalid
     */
    public TreeSet<PacketPosition> getResetPacketsBeforeTick(EngineType engineType, int wantedTick, TreeSet<PacketPosition> packetPositions) throws IOException {
        int backup = getPosition();
        PacketPosition wanted = PacketPosition.createPacketPosition(wantedTick, Demo.EDemoCommands.DEM_FullPacket_VALUE, 0);
        if (packetPositions.tailSet(wanted, true).size() == 0) {
            PacketPosition basePos = packetPositions.floor(wanted);
            setPosition(basePos.getOffset());
            try {
                while (true) {
                    int at = getPosition();
                    int kind = readVarInt32() & ~engineType.getCompressedFlag();
                    int tick = readVarInt32();
                    int size = readVarInt32();
                    PacketPosition pp = PacketPosition.createPacketPosition(tick, kind, at);
                    if (pp != null) {
                        packetPositions.add(pp);
                    }
                    if (tick >= wantedTick) {
                        break;
                    }
                    skipBytes(size);
                }
            } catch (EOFException e) {
            }
        }
        setPosition(backup);
        return new TreeSet<>(packetPositions.headSet(wanted, true));
    }

    /**
     * gets the number of the last tick
     *
     * <p> Caution: this will set the position to the end of the data. If the implementation does not support
     * setting the position to a lower value, you will not be able to use this source for further processing.
     *
     * @return the last tick
     * @throws IOException if the position cannot be adjusted, or the data is invalid
     */
    public int getLastTick() throws IOException {
        if (lastTick == null) {
            setPosition(8);
            setPosition(readFixedInt32());
            skipVarInt32();
            lastTick = readVarInt32();
        }
        return lastTick.intValue();
    }

    /**
     * closes the source
     *
     * @throws IOException if closing the source fails
     */
    public void close() throws IOException {
    }

}
