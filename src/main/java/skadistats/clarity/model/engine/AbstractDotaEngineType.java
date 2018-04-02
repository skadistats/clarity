package skadistats.clarity.model.engine;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.ZeroCopy;
import org.xerial.snappy.Snappy;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.processor.reader.PacketInstance;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.Packet;
import skadistats.clarity.wire.common.DemoPackets;

import java.io.IOException;

public abstract class AbstractDotaEngineType extends AbstractEngineType {

    public AbstractDotaEngineType(EngineId identifier, int compressedFlag, boolean sendTablesContainer, int indexBits, int serialBits) {
        super(identifier, compressedFlag, sendTablesContainer, indexBits, serialBits);
    }

    @Override
    public int readKind(Source source) throws IOException {
        return source.readVarInt32();
    }

    @Override
    public int readTick(Source source) throws IOException {
        return source.readVarInt32();
    }

    @Override
    public int readPlayerSlot(Source source) throws IOException {
        return 0;
    }

    @Override
    public int readSize(Source source) throws IOException {
        return source.readVarInt32();
    }

    @Override
    public void readCommandInfo(Source source) throws IOException {
        // do nothing
    }

    @Override
    public <T extends GeneratedMessage> PacketInstance<T> getNextPacketInstance(final Source source) throws IOException {
        int k = readKind(source);
        final boolean isCompressed = (k & getCompressedFlag()) == getCompressedFlag();
        k &= ~getCompressedFlag();
        final int tick = readTick(source);
        final int size = readSize(source);
        final int kind = k;
        final Class<T> messageClass = (Class<T>) DemoPackets.classForKind(kind);
        return new PacketInstance<T>() {
            @Override
            public int getKind() {
                return kind;
            }

            @Override
            public int getTick() {
                return tick;
            }

            @Override
            public Class<T> getMessageClass() {
                return messageClass;
            }

            @Override
            public T parse() throws IOException {
                byte[] buf = new byte[size];
                source.readBytes(buf, 0, size);
                if (isCompressed) {
                    buf = Snappy.uncompress(buf);
                }
                return Packet.parse(messageClass, ZeroCopy.wrap(buf));
            }

            @Override
            public void skip() throws IOException {
                source.skipBytes(size);
            }
        };
    }

}
