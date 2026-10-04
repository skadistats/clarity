package skadistats.clarity.processor.packet;

import org.xerial.snappy.Snappy;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.event.Provides;
import skadistats.clarity.source.Source;

import java.io.IOException;

/**
 * Helper for reading raw packet bytes from a {@link Source} or a {@link BitStream}.
 * <p>
 * Active when a processor declares {@link UsesPacketReader}.
 */
@Provides({ UsesPacketReader.class })
public class PacketReader {

    /** Creates a packet reader. */
    public PacketReader() {
    }

    /**
     * Reads a block of bytes from the source, optionally Snappy-decompressing it.
     *
     * @param source the source to read from
     * @param size number of bytes to read from the source
     * @param isCompressed whether the bytes are Snappy-compressed
     * @return the (decompressed) bytes
     * @throws IOException if reading or decompressing fails
     */
    public byte[] readFromSource(Source source, int size, boolean isCompressed) throws IOException {
        var buf = new byte[size];
        source.readBytes(buf, 0, size);
        if (isCompressed) {
            return Snappy.uncompress(buf);
        } else {
            return buf;
        }
    }

    /**
     * Reads bits from a bit stream into a byte array.
     *
     * @param bs the bit stream to read from
     * @param size number of <em>bits</em> to read
     * @return a byte array of {@code (size + 7) / 8} bytes holding the bits read
     * @throws IOException if reading fails
     */
    public byte[] readFromBitStream(BitStream bs, int size) throws IOException {
        var buf = new byte[(size + 7) / 8];
        bs.readBitsIntoByteArray(buf, size);
        return buf;
    }

}
