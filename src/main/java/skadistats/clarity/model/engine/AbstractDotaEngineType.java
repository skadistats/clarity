package skadistats.clarity.model.engine;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.ZeroCopy;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.processor.reader.PacketInstance;
import skadistats.clarity.source.ResetRelevantKind;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.Packet;
import skadistats.clarity.wire.common.DemoPackets;
import skadistats.clarity.wire.common.proto.Demo;

import java.io.IOException;

public abstract class AbstractDotaEngineType extends AbstractEngineType {

    public AbstractDotaEngineType(EngineId identifier, boolean sendTablesContainer, int indexBits, int serialBits) {
        super(identifier, sendTablesContainer, indexBits, serialBits);
    }

    protected abstract int getCompressedFlag();

    @Override
    public float getMillisPerTick() {
        return 1000.0f / 30.0f;
    }

    @Override
    public boolean isFullPacketSeekAllowed() {
        return true;
    }

    public int determineLastTick(Source source) throws IOException {
        int backup = source.getPosition();
        source.setPosition(source.readFixedInt32());
        source.skipVarInt32();
        int lastTick = source.readVarInt32();
        source.setPosition(backup);
        return lastTick;
    }

    @Override
    public <T extends GeneratedMessage> PacketInstance<T> getNextPacketInstance(final Source source) throws IOException {
        int rawKind = source.readVarInt32();
        final boolean isCompressed = (rawKind & getCompressedFlag()) == getCompressedFlag();
        final int kind = rawKind &~ getCompressedFlag();
        final int tick = source.readVarInt32();
        final int size = source.readVarInt32();
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
            public ResetRelevantKind getResetRelevantKind() {
                switch(kind) {
                    case Demo.EDemoCommands.DEM_SyncTick_VALUE:
                        return ResetRelevantKind.SYNC;
                    case Demo.EDemoCommands.DEM_StringTables_VALUE:
                        return ResetRelevantKind.STRINGTABLE;
                    case Demo.EDemoCommands.DEM_FullPacket_VALUE:
                        return ResetRelevantKind.FULL_PACKET;
                    default:
                        return null;
                }
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
