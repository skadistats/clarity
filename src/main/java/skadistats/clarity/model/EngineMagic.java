package skadistats.clarity.model;

import skadistats.clarity.model.engine.*;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.common.proto.Demo;
import skadistats.clarity.wire.csgo.s1.proto.CsGoClarityMessages;

import java.io.IOException;

public enum EngineMagic {

    DOTA_S1("PBUFDEM\0") {
        @Override
        public EngineType determineEngineType(Source source) throws IOException {
            int infoOffset = source.readFixedInt32();
            PacketInstanceReaderProtobufDemo packetInstanceReader = new PacketInstanceReaderProtobufDemo(Demo.EDemoCommands.DEM_IsCompressed_S1_VALUE);
            Demo.CDemoFileHeader header = packetInstanceReader.readHeader(source);
            return new DotaS1EngineType(EngineId.DOTA_S1, packetInstanceReader, header, infoOffset);
        }
    },
    CSGO_S1("HL2DEMO\0") {
        @Override
        public EngineType determineEngineType(Source source) throws IOException {
            PacketInstanceReaderCsGoS1 packetInstanceReader = new PacketInstanceReaderCsGoS1();
            CsGoClarityMessages.CsGoDemoHeader header = packetInstanceReader.readHeader(source);
            return new CsGoS1EngineType(EngineId.CSGO_S1, packetInstanceReader, header);
        }
    },
    S2("PBDEMS2\0") {
        @Override
        public EngineType determineEngineType(Source source) throws IOException {
            int infoOffset = source.readFixedInt32();
            source.skipBytes(4);
            PacketInstanceReaderProtobufDemo packetInstanceReader = new PacketInstanceReaderProtobufDemo(Demo.EDemoCommands.DEM_IsCompressed_S2_VALUE);
            Demo.CDemoFileHeader header = packetInstanceReader.readHeader(source);
            if (header.hasGame() && "csgo".equals(header.getGame())) {
                return new CsgoS2EngineType(EngineId.CSGO_S2, packetInstanceReader, header, infoOffset);
            } else {
                return new DotaS2EngineType(EngineId.DOTA_S2, packetInstanceReader, header, infoOffset);
            }
        }
    };

    public static EngineMagic magicForString(String magic) {
        for (EngineMagic em : values()) {
            if (em.magic.equals(magic)) {
                return em;
            }
        }
        return null;
    }

    private final String magic;

    EngineMagic(String magic) {
        this.magic = magic;
    }

    public abstract EngineType determineEngineType(Source source) throws IOException;
}
