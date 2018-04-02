package skadistats.clarity.model.engine;

import com.google.protobuf.GeneratedMessage;
import skadistats.clarity.decoder.FieldReader;
import skadistats.clarity.decoder.bitstream.BitStream;
import skadistats.clarity.decoder.s1.S1FieldReader;
import skadistats.clarity.model.DemoHeader;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.common.proto.Demo;
import skadistats.clarity.wire.s1.EmbeddedPackets;
import skadistats.clarity.wire.s1.UserMessagePackets;

import java.io.IOException;

public class DotaS1EngineType extends AbstractDotaEngineType {

    public DotaS1EngineType(EngineId identifier) {
        super(identifier,
                Demo.EDemoCommands.DEM_IsCompressed_S1_VALUE,
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
        return new S1FieldReader();
    }

    @Override
    public DemoHeader readHeader(Source source) throws IOException {
        source.skipBytes(4);
        return null;
    }

    @Override
    public int readEmbeddedKind(BitStream bs) {
        return bs.readVarUInt();
    }

}
