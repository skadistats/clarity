package skadistats.clarity.model.engine;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.ZeroCopy;
import skadistats.clarity.processor.reader.PacketInstance;
import skadistats.clarity.source.ResetRelevantKind;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.Packet;
import skadistats.clarity.wire.shared.demo.DemoPackets;
import skadistats.clarity.wire.shared.demo.proto.Demo;

import java.io.IOException;

public class PacketInstanceReaderProtobufDemo extends PacketInstanceReader<Demo.CDemoFileHeader> {

    private final int compressedFlag;

    public PacketInstanceReaderProtobufDemo(int compressedFlag) {
        this.compressedFlag = compressedFlag;
    }

    @Override
    public Demo.CDemoFileHeader readHeader(Source source) throws IOException {
        PacketInstance<Demo.CDemoFileHeader> nextPacketInstance = getNextPacketInstance(source);
        return nextPacketInstance.parse();
    }

    @Override
    public <T extends GeneratedMessage> PacketInstance<T> getNextPacketInstance(Source source) throws IOException {
        var rawKind = source.readVarInt32();
        final var isCompressed = (rawKind & compressedFlag) == compressedFlag;
        final var kind = rawKind &~ compressedFlag;
        final var tick = source.readVarInt32();
        final var size = source.readVarInt32();
        final var messageClass = (Class<T>) DemoPackets.classForKind(kind);
        return new PacketInstance<>() {

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
                switch (kind) {
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
