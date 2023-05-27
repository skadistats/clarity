package skadistats.clarity.model.engine;

import com.google.protobuf.GeneratedMessage;
import skadistats.clarity.io.FieldReader;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.s1.DotaS1FieldReader;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.wire.dota.s1.EmbeddedPackets;
import skadistats.clarity.wire.dota.s1.UserMessagePackets;
import skadistats.clarity.wire.shared.demo.proto.Demo;

public class DotaS1EngineType extends AbstractProtobufDemoEngineType {

    public DotaS1EngineType(EngineId id, PacketInstanceReader<Demo.CDemoFileHeader> packetInstanceReader, Demo.CDemoFileHeader header, int infoOffset) {
        super(
                id,
                packetInstanceReader,
                header,
                infoOffset,
                true,   // CDemoSendTables is container
                11,
                10
        );
    }

    @Override
    public Class<? extends GeneratedMessage> embeddedPacketClassForKind(int kind) {
        return EmbeddedPackets.classForKind(kind);
    }

    @Override
    public Class<? extends GeneratedMessage> userMessagePacketClassForKind(int kind) {
        return UserMessagePackets.classForKind(kind);
    }

    @Override
    public boolean isUserMessage(Class<? extends GeneratedMessage> clazz) {
        return clazz.equals(GeneratedMessage.class) || UserMessagePackets.isKnownClass(clazz);
    }

    @Override
    public FieldReader getNewFieldReader() {
        return new DotaS1FieldReader();
    }

    @Override
    public int readEmbeddedKind(BitStream bs) {
        return bs.readVarUInt();
    }

}
