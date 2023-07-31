package skadistats.clarity.model.engine;

import com.google.protobuf.GeneratedMessage;
import skadistats.clarity.io.FieldReader;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.s2.S2FieldReader;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.wire.csgo.s2.EmbeddedPackets;
import skadistats.clarity.wire.shared.demo.proto.Demo;
import skadistats.clarity.wire.shared.demo.proto.DemoNetMessages;

import java.util.regex.Pattern;

public class CsgoS2EngineType extends AbstractProtobufDemoEngineType {

    public CsgoS2EngineType(EngineId id, PacketInstanceReader<Demo.CDemoFileHeader> packetInstanceReader, Demo.CDemoFileHeader header, int infoOffset) {
        super(
                id,
                packetInstanceReader,
                header,
                infoOffset,
                false, // CDemoSendTables is container
                14,
                17
        );
    }

    @Override
    protected Integer determineGameVersion(DemoNetMessages.CSVCMsg_ServerInfo serverInfo) {
        var matcher = Pattern.compile("csgo_v20*(\\d+)").matcher(serverInfo.getGameDir());
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
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
    public int readEmbeddedKind(BitStream bs) {
        return bs.readUBitVar();
    }

}
