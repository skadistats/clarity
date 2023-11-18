package skadistats.clarity.model.engine;

import com.google.protobuf.GeneratedMessage;
import skadistats.clarity.processor.packet.PacketReader;
import skadistats.clarity.processor.reader.PacketInstance;
import skadistats.clarity.source.Source;

import java.io.IOException;

public abstract class PacketInstanceReader<H extends GeneratedMessage> {

    protected final PacketReader packetReader = new PacketReader();

    public abstract H readHeader(Source source) throws IOException;

    public abstract <T extends GeneratedMessage> PacketInstance<T> getNextPacketInstance(Source source) throws IOException;

    public PacketReader getPacketReader() {
        return packetReader;
    }
}
