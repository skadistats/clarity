package skadistats.clarity.model.engine;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.ZeroCopy;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.processor.reader.PacketInstance;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.Packet;
import skadistats.clarity.wire.common.DemoPackets;

import java.io.IOException;

public abstract class AbstractDotaEngineType extends AbstractEngineType {

    public AbstractDotaEngineType(EngineId identifier, boolean sendTablesContainer, int indexBits, int serialBits) {
        super(identifier, sendTablesContainer, indexBits, serialBits);
    }

    protected abstract int getCompressedFlag();

    @Override
    public <T extends GeneratedMessage> PacketInstance<T> getNextPacketInstance(final Source source) throws IOException {
        int k = source.readVarInt32();
        final boolean isCompressed = (k & getCompressedFlag()) == getCompressedFlag();
        k &= ~getCompressedFlag();
        final int tick = source.readVarInt32();
        final int size = source.readVarInt32();
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
                return Packet.parse(
                        messageClass,
                        ZeroCopy.wrap(packetReader.readFromSource(source, size, isCompressed))
                );
            }

            @Override
            public void skip() throws IOException {
                source.skipBytes(size);
            }
        };
    }

}
