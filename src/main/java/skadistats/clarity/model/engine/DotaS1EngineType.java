package skadistats.clarity.model.engine;

import com.google.protobuf.GeneratedMessage;
import skadistats.clarity.decoder.FieldReader;
import skadistats.clarity.decoder.bitstream.BitStream;
import skadistats.clarity.decoder.s1.DotaS1FieldReader;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.common.proto.Demo;
import skadistats.clarity.wire.s1.EmbeddedPackets;
import skadistats.clarity.wire.s1.UserMessagePackets;

import java.io.IOException;

public class DotaS1EngineType extends AbstractDotaEngineType {

    public DotaS1EngineType(EngineId identifier) {
        super(identifier,
                true,   // CDemoSendTables is container
                11,
                10
        );
    }

    @Override
    protected int getCompressedFlag() {
        return Demo.EDemoCommands.DEM_IsCompressed_S1_VALUE;
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
    public void readHeader(Source source) throws IOException {
        source.skipBytes(4);
    }

    @Override
    public void skipHeader(Source source) throws IOException {
        source.skipBytes(4);
    }

    @Override
    public void emitHeader() {
    }

    @Override
    public int readEmbeddedKind(BitStream bs) {
        return bs.readVarUInt();
    }

}
