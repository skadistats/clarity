package skadistats.clarity.model.engine;

import com.google.protobuf.GeneratedMessage;
import skadistats.clarity.decoder.FieldReader;
import skadistats.clarity.decoder.bitstream.BitStream;
import skadistats.clarity.decoder.s2.S2FieldReader;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.common.proto.Demo;
import skadistats.clarity.wire.s2.EmbeddedPackets;

import java.io.IOException;

public class DotaS2EngineType extends AbstractDotaEngineType {

    public DotaS2EngineType(EngineId identifier) {
        super(identifier,
                false, // CDemoSendTables is container
                14,
                17
        );
    }

    @Override
    protected int getCompressedFlag() {
        return Demo.EDemoCommands.DEM_IsCompressed_S2_VALUE;
    }

    @Override
    public Class<? extends GeneratedMessage> embeddedPacketClassForKind(int kind) {
        return EmbeddedPackets.classForKind(kind);
    }

    @Override
    public Class<? extends GeneratedMessage> userMessagePacketClassForKind(int kind) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isUserMessage(Class<? extends GeneratedMessage> clazz) {
        return false;
    }

    @Override
    public FieldReader getNewFieldReader() {
        return new S2FieldReader();
    }

    @Override
    public void readHeader(Source source) throws IOException {
        source.skipBytes(8);
    }

    @Override
    public void skipHeader(Source source) throws IOException {
        source.skipBytes(8);
    }

    @Override
    public void emitHeader() {
    }

    @Override
    public int readEmbeddedKind(BitStream bs) {
        return bs.readUBitVar();
    }

}
