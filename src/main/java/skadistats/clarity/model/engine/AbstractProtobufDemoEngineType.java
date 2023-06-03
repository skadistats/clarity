package skadistats.clarity.model.engine;

import skadistats.clarity.model.EngineId;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.shared.demo.proto.Demo;
import skadistats.clarity.wire.shared.demo.proto.DemoNetMessages;

import java.io.IOException;

public abstract class AbstractProtobufDemoEngineType extends AbstractEngineType<Demo.CDemoFileHeader> {

    private final int infoOffset;

    public AbstractProtobufDemoEngineType(EngineId id, PacketInstanceReader<Demo.CDemoFileHeader> packetInstanceReader, Demo.CDemoFileHeader header, int infoOffset, boolean sendTablesContainer, int indexBits, int serialBits) {
        super(id, packetInstanceReader, header, sendTablesContainer, indexBits, serialBits);
        this.infoOffset = infoOffset;
    }

    @OnMessage(DemoNetMessages.CSVCMsg_ServerInfo.class)
    protected void onServerInfo(DemoNetMessages.CSVCMsg_ServerInfo serverInfo) {
        this.millisPerTick = serverInfo.getTickInterval() * 1000.0f;
    }

    @Override
    public int getInfoOffset() {
        return infoOffset;
    }

    @Override
    public boolean isFullPacketSeekAllowed() {
        return true;
    }

    @Override
    public Integer getExpectedFullPacketInterval() {
        return 1800;
    }

    public int determineLastTick(Source source) throws IOException {
        var backup = source.getPosition();
        source.setPosition(8);
        source.setPosition(source.readFixedInt32());
        source.skipVarInt32();
        var lastTick = source.readVarInt32();
        source.setPosition(backup);
        return lastTick;
    }

}
