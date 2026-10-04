package skadistats.clarity.model;

import skadistats.clarity.ClarityException;
import skadistats.clarity.engine.EngineType;
import skadistats.clarity.engine.PacketInstanceReaderProtobufDemo;
import skadistats.clarity.engine.s1.CsgoEngineType;
import skadistats.clarity.engine.s1.DotaS1EngineType;
import skadistats.clarity.engine.s1.PacketInstanceReaderCsgo;
import skadistats.clarity.engine.s2.Cs2EngineType;
import skadistats.clarity.engine.s2.DeadlockEngineType;
import skadistats.clarity.engine.s2.DotaS2EngineType;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.shared.demo.proto.Demo;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * The magic header string at the start of a demo file, mapped to the logic
 * that builds the matching {@link EngineType}.
 */
public enum EngineMagic {

    /** {@code PBUFDEM\0}: Dota 2 Source 1 demo. */
    DOTA_S1("PBUFDEM\0") {
        @Override
        public EngineType determineEngineType(Source source) throws IOException {
            var infoOffset = source.readFixedInt32();
            var packetInstanceReader = new PacketInstanceReaderProtobufDemo(Demo.EDemoCommands.DEM_IsCompressed_S1_VALUE);
            var header = packetInstanceReader.readHeader(source);
            return new DotaS1EngineType(EngineId.DOTA_S1, packetInstanceReader, header, infoOffset);
        }
    },
    /** {@code HL2DEMO\0}: CS:GO Source 1 demo. */
    CSGO("HL2DEMO\0") {
        @Override
        public EngineType determineEngineType(Source source) throws IOException {
            var packetInstanceReader = new PacketInstanceReaderCsgo();
            var header = packetInstanceReader.readHeader(source);
            return new CsgoEngineType(EngineId.CSGO, packetInstanceReader, header);
        }
    },
    /**
     * {@code PBDEMS2\0}: Source 2 demo. The game (Dota 2, CS2 or Deadlock) is
     * determined from the file header's game or game directory.
     */
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
            if (gameId == null) {
                throw new ClarityException("Unable to extract game id");
            }
            switch(gameId) {
                case "csgo":
                    return new Cs2EngineType(EngineId.CS2, packetInstanceReader, header, infoOffset);
                case "dota":
                    return new DotaS2EngineType(EngineId.DOTA_S2, packetInstanceReader, header, infoOffset);
                case "citadel":
                    return new DeadlockEngineType(EngineId.DEADLOCK, packetInstanceReader, header, infoOffset);
                default:
                    throw new ClarityException("Unable to determine engine type");
            }
        }
    };

    /**
     * @return the constant whose magic equals {@code magic}, or {@code null} if none does
     */
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

    /**
     * Reads the remainder of the demo header from {@code source} (positioned
     * just after the magic string) and builds the engine type.
     *
     * @throws skadistats.clarity.ClarityException for {@link #S2} if the game cannot be determined
     */
    public abstract EngineType determineEngineType(Source source) throws IOException;
}
