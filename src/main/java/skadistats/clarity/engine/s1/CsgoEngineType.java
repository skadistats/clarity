package skadistats.clarity.engine.s1;

import skadistats.clarity.protobuf.GeneratedMessage;
import skadistats.clarity.engine.AbstractEngineType;
import skadistats.clarity.engine.PacketInstanceReader;
import skadistats.clarity.io.FieldReader;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.s1.CsgoFieldReader;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.reader.OnPostEmbeddedMessage;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.cs.csgo.EmbeddedPackets;
import skadistats.clarity.wire.cs.csgo.UserMessagePackets;
import skadistats.clarity.wire.cs.csgo.proto.CsgoClarityMessages;
import skadistats.clarity.wire.cs.csgo.proto.CsgoNetMessages;
import skadistats.clarity.wire.shared.demo.proto.Demo;
import skadistats.clarity.wire.shared.s1.proto.S1NetMessages;

import java.io.IOException;

public class CsgoEngineType extends AbstractEngineType<CsgoClarityMessages.CsgoDemoHeader> {

    public CsgoEngineType(EngineId id, PacketInstanceReader<CsgoClarityMessages.CsgoDemoHeader> packetInstanceReader, CsgoClarityMessages.CsgoDemoHeader header) {
        super(
                id,
                packetInstanceReader,
                header,
                true,   // CDemoSendTables is container
                11,
                10
        );
    }

    @OnMessage(CsgoNetMessages.CSVCMsg_ServerInfo.class)
    protected void onServerInfo(CsgoNetMessages.CSVCMsg_ServerInfo serverInfo) {
        ctx.setMillisPerTick(serverInfo.getTickInterval() * 1000.0f);
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
    public boolean shouldHandleDeletions(BitStream bs) {
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
        return new CsgoFieldReader();
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
            var b = Demo.CDemoClassInfo.newBuilder();
            var n = bs.readSBitInt(16);
            for (var i = 0; i < n; i++) {
                b.addClassesBuilder()
                        .setClassId(bs.readSBitInt(16))
                        .setNetworkName(bs.readString(255))
                        .setTableName(bs.readString(255));
            }
            OnMessage.Event ev = ctx.createEvent(OnMessage.class);
            ev.raise(b.build());
        }
    }


}
