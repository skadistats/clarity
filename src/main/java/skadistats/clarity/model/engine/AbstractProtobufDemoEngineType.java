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
        if (header.hasBuildNum()) {
            contextData.setBuildNumber(header.getBuildNum());
        }
    }

    @OnMessage(DemoNetMessages.CSVCMsg_ServerInfo.class)
    protected void onServerInfo(DemoNetMessages.CSVCMsg_ServerInfo serverInfo) {
        contextData.setMillisPerTick(serverInfo.getTickInterval() * 1000.0f);
        var gameVersion = determineGameVersion(serverInfo);
        if (gameVersion != null) {
            contextData.setGameVersion(gameVersion);
        } else if (getId().canExtractGameVersion()) {
            log.warn("received CSVCMsg_ServerInfo, but could not read game version from it. (game dir '%s')", serverInfo.getGameDir());
        }
    }

    protected abstract Integer determineGameVersion(DemoNetMessages.CSVCMsg_ServerInfo serverInfo);

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
