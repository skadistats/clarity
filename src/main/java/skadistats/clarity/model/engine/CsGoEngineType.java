package skadistats.clarity.model.engine;

import com.google.protobuf.GeneratedMessage;
import skadistats.clarity.decoder.FieldReader;
import skadistats.clarity.decoder.bitstream.BitStream;
import skadistats.clarity.decoder.s1.S1FieldReader;
import skadistats.clarity.model.DemoHeader;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.csgo.UserMessagePackets;
import skadistats.clarity.wire.s1.EmbeddedPackets;

import java.io.IOException;

public class CsGoEngineType extends AbstractEngineType {
    public CsGoEngineType(EngineId identifier) {
        super(identifier,
                0x100,
                true,   // CDemoSendTables is container
                11, 10
        );
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
        return new S1FieldReader();
    }

    @Override
    public DemoHeader readHeader(Source source) throws IOException {
//            int32	demoprotocol;					// Should be DEMO_PROTOCOL
        source.skipBytes(4);
//            int32	networkprotocol;				// Should be PROTOCOL_VERSION
        source.skipBytes(4);
//            char	servername[ MAX_OSPATH ];		// Name of server
        source.skipBytes(260);
//            char	clientname[ MAX_OSPATH ];		// Name of client who recorded the game
        source.skipBytes(260);
//            char	mapname[ MAX_OSPATH ];			// Name of map
        source.skipBytes(260);
//            char	gamedirectory[ MAX_OSPATH ];	// Name of game directory (com_gamedir)
        source.skipBytes(260);
//            float	playback_time;					// Time of track
        source.skipBytes(4);
//            int32   playback_ticks;					// # of ticks in track
        source.skipBytes(4);
//            int32   playback_frames;				// # of frames in track
        source.skipBytes(4);
//            int32	signonlength;					// length of sigondata in bytes
        source.skipBytes(4);
        return null;
    }

    @Override
    public int readKind(Source source) throws IOException {
        return source.readByte() & 0xFF;
    }

    @Override
    public int readTick(Source source) throws IOException {
        return source.readFixedInt32();
    }

    @Override
    public int readPlayerSlot(Source source) throws IOException {
        return source.readByte() & 0xFF;
    }

    @Override
    public int readSize(Source source) throws IOException {
        return source.readFixedInt32();
    }

    @Override
    public int readEmbeddedKind(BitStream bs) {
        return bs.readVarUInt();
    }

    @Override
    public void readCommandInfo(Source source) throws IOException {
        source.skipBytes(152);
    }

}
