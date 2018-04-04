package skadistats.clarity.processor.packet;

import org.xerial.snappy.Snappy;
import skadistats.clarity.decoder.bitstream.BitStream;
import skadistats.clarity.event.Provides;
import skadistats.clarity.source.Source;

import java.io.IOException;

@Provides({ UsesPacketReader.class })
public class PacketReader {

    public byte[] readFromSource(Source source, int size, boolean isCompressed) throws IOException {
        byte[] buf = new byte[size];
        source.readBytes(buf, 0, size);
        if (isCompressed) {
            return Snappy.uncompress(buf);
        } else {
            return buf;
        }
    }

    public byte[] readFromBitStream(BitStream bs, int size) throws IOException {
        byte[] buf = new byte[(size + 7) / 8];
        bs.readBitsIntoByteArray(buf, size);
        return buf;
    }

}
