package skadistats.clarity.model;

import skadistats.clarity.ClarityException;
import skadistats.clarity.model.engine.CsGoS1EngineType;
import skadistats.clarity.model.engine.CsgoS2EngineType;
import skadistats.clarity.model.engine.DotaS1EngineType;
import skadistats.clarity.model.engine.DotaS2EngineType;
import skadistats.clarity.model.engine.PacketInstanceReaderCsGoS1;
import skadistats.clarity.model.engine.PacketInstanceReaderProtobufDemo;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.shared.demo.proto.Demo;

import java.io.IOException;
import java.util.regex.Pattern;

public enum EngineMagic {

    DOTA_S1("PBUFDEM\0") {
        @Override
        public EngineType determineEngineType(Source source) throws IOException {
            var infoOffset = source.readFixedInt32();
            var packetInstanceReader = new PacketInstanceReaderProtobufDemo(Demo.EDemoCommands.DEM_IsCompressed_S1_VALUE);
            var header = packetInstanceReader.readHeader(source);
            return new DotaS1EngineType(EngineId.DOTA_S1, packetInstanceReader, header, infoOffset);
        }
    },
    CSGO_S1("HL2DEMO\0") {
        @Override
        public EngineType determineEngineType(Source source) throws IOException {
            var packetInstanceReader = new PacketInstanceReaderCsGoS1();
            var header = packetInstanceReader.readHeader(source);
            return new CsGoS1EngineType(EngineId.CSGO_S1, packetInstanceReader, header);
        }
    },
    S2("PBDEMS2\0") {
        private final Pattern GAMEDIR_MATCH = Pattern.compile(".*[/\\\\](\\w+)$");
        @Override
        public EngineType determineEngineType(Source source) throws IOException {
            var infoOffset = source.readFixedInt32();
            source.skipBytes(4);
            var packetInstanceReader = new PacketInstanceReaderProtobufDemo(Demo.EDemoCommands.DEM_IsCompressed_S2_VALUE);
            var header = packetInstanceReader.readHeader(source);

            String gameId = null;

            if (header.hasGame()) {
                gameId = header.getGame();
            } else if (header.hasGameDirectory()) {
                var m = GAMEDIR_MATCH.matcher(header.getGameDirectory());
                if (m.matches()) {
                    gameId = m.group(1);
                }
            }
            switch(gameId) {
                case "csgo":
                    return new CsgoS2EngineType(EngineId.CSGO_S2, packetInstanceReader, header, infoOffset);
                case "dota":
                    return new DotaS2EngineType(EngineId.DOTA_S2, packetInstanceReader, header, infoOffset);
                default:
                    throw new ClarityException("Unable to determine engine type");
            }
        }
    };

    public static EngineMagic magicForString(String magic) {
        for (var em : values()) {
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
