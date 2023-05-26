package skadistats.clarity.model.engine;

import com.google.protobuf.GeneratedMessage;
import skadistats.clarity.io.FieldReader;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.s1.CsGoFieldReader;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.reader.OnPostEmbeddedMessage;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.shared.common.proto.Demo;
import skadistats.clarity.wire.csgo.s1.EmbeddedPackets;
import skadistats.clarity.wire.csgo.s1.UserMessagePackets;
import skadistats.clarity.wire.csgo.s1.proto.CsGoClarityMessages;
import skadistats.clarity.wire.csgo.s1.proto.CsGoNetMessages;
import skadistats.clarity.wire.dota.s1.proto.S1NetMessages;

import java.io.IOException;

public class CsGoS1EngineType extends AbstractEngineType<CsGoClarityMessages.CsGoDemoHeader> {

    public CsGoS1EngineType(EngineId id, PacketInstanceReader<CsGoClarityMessages.CsGoDemoHeader> packetInstanceReader, CsGoClarityMessages.CsGoDemoHeader header) {
        super(
                id,
                packetInstanceReader,
                header,
                true,   // CDemoSendTables is container
                11,
                10
        );
    }

    @OnMessage(CsGoNetMessages.CSVCMsg_ServerInfo.class)
    protected void onServerInfo(CsGoNetMessages.CSVCMsg_ServerInfo serverInfo) {
        this.millisPerTick = serverInfo.getTickInterval() * 1000.0f;
    }

    @Override
    public int getInfoOffset() {
        throw new UnsupportedOperationException("CSGO Source 1 replays do not have CDemoFileInfo");
    }

    @Override
    public boolean isFullPacketSeekAllowed() {
        return false;
    }

    @Override
    public Integer getExpectedFullPacketInterval() {
        return null;
    }

    @Override
    public boolean handleDeletions() {
        return false;
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
        return UserMessagePackets.isKnownClass(clazz);
    }

    @Override
    public FieldReader getNewFieldReader() {
        return new CsGoFieldReader();
    }

    @Override
    public int determineLastTick(Source source) throws IOException {
        return header.getPlaybackTicks();
    }

    @Override
    public int readEmbeddedKind(BitStream bs) {
        return bs.readVarUInt();
    }

    @OnPostEmbeddedMessage(S1NetMessages.CSVCMsg_SendTable.class)
    public void onPostSendTable(S1NetMessages.CSVCMsg_SendTable message, BitStream bs) {
        if (message.getIsEnd()) {
            Demo.CDemoClassInfo.Builder b = Demo.CDemoClassInfo.newBuilder();
            int n = bs.readSBitInt(16);
            for (int i = 0; i < n; i++) {
                b.addClassesBuilder()
                        .setClassId(bs.readSBitInt(16))
                        .setNetworkName(bs.readString(255))
                        .setTableName(bs.readString(255));
            }
            ctx.createEvent(OnMessage.class, Demo.CDemoClassInfo.class).raise(b.build());
        }
    }


}
