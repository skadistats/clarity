package skadistats.clarity.model;

import com.google.protobuf.GeneratedMessage;
import skadistats.clarity.decoder.FieldReader;
import skadistats.clarity.decoder.bitstream.BitStream;
import skadistats.clarity.decoder.s1.S1FieldReader;
import skadistats.clarity.decoder.s2.S2FieldReader;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.common.DemoPackets;
import skadistats.clarity.wire.common.proto.Demo;

import java.io.IOException;

public enum EngineType {

    SOURCE1(
        "PBUFDEM\0",
        Demo.EDemoCommands.DEM_IsCompressed_S1_VALUE,
        true,   // CDemoSendTables is container
        11, 10
    ) {
        @Override
        public Class<? extends GeneratedMessage> embeddedPacketClassForKind(int kind) {
            return skadistats.clarity.wire.s1.EmbeddedPackets.classForKind(kind);
        }
        @Override
        public Class<? extends GeneratedMessage> userMessagePacketClassForKind(int kind) {
            return skadistats.clarity.wire.s1.UserMessagePackets.classForKind(kind);
        }
        @Override
        public boolean isUserMessage(Class<? extends GeneratedMessage> clazz) {
            return clazz.equals(GeneratedMessage.class)
                || skadistats.clarity.wire.s1.UserMessagePackets.isKnownClass(clazz);
        }
        @Override
        public int readEmbeddedKind(BitStream bs) {
            return bs.readVarUInt();
        }
        @Override
        public FieldReader getNewFieldReader() {
            return new S1FieldReader();
        }
        @Override
        public DemoHeader readHeader(Source source) throws IOException {
            source.skipBytes(4);
            return null;
        }
        @Override
        public int readKind(Source source) throws IOException {
            return source.readVarInt32();
        }
        @Override
        public int readTick(Source source) throws IOException {
            return source.readVarInt32();
        }
        @Override
        public int readPlayerSlot(Source source) throws IOException {
            return 0;
        }
        @Override
        public int readSize(Source source) throws IOException {
            return source.readVarInt32();
        }
    },

    SOURCE2(
        "PBDEMS2\0",
        Demo.EDemoCommands.DEM_IsCompressed_S2_VALUE,
        false, // CDemoSendTables is container
        14, 17
    ) {
        @Override
        public Class<? extends GeneratedMessage> embeddedPacketClassForKind(int kind) {
            return skadistats.clarity.wire.s2.EmbeddedPackets.classForKind(kind);
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
        public int readEmbeddedKind(BitStream bs) {
            return bs.readUBitVar();
        }
        @Override
        public FieldReader getNewFieldReader() {
            return new S2FieldReader();
        }
        @Override
        public DemoHeader readHeader(Source source) throws IOException {
            source.skipBytes(8);
            return null;
        }
        @Override
        public int readKind(Source source) throws IOException {
            return source.readVarInt32();
        }
        @Override
        public int readTick(Source source) throws IOException {
            return source.readVarInt32();
        }
        @Override
        public int readPlayerSlot(Source source) throws IOException {
            return 0;
        }
        @Override
        public int readSize(Source source) throws IOException {
            return source.readVarInt32();
        }
    },

    CSGO(
        "HL2DEMO\0",
        0x100,
        true,   // CDemoSendTables is container
        11, 10
    ) {
        @Override
        public Class<? extends GeneratedMessage> embeddedPacketClassForKind(int kind) {
            return skadistats.clarity.wire.s1.EmbeddedPackets.classForKind(kind);
        }
        @Override
        public Class<? extends GeneratedMessage> userMessagePacketClassForKind(int kind) {
            return skadistats.clarity.wire.csgo.UserMessagePackets.classForKind(kind);
        }
        @Override
        public boolean isUserMessage(Class<? extends GeneratedMessage> clazz) {
            return skadistats.clarity.wire.csgo.UserMessagePackets.isKnownClass(clazz);
        }
        @Override
        public int readEmbeddedKind(BitStream bs) {
            return bs.readVarUInt();
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
    };


    private final String magic;
    private final int compressedFlag;
    private final boolean sendTablesContainer;
    private final int indexBits;
    private final int serialBits;
    private final int indexMask;



    EngineType(String magic, int compressedFlag, boolean sendTablesContainer, int indexBits, int serialBits) {
        this.magic = magic;
        this.compressedFlag = compressedFlag;
        this.sendTablesContainer = sendTablesContainer;
        this.indexBits = indexBits;
        this.serialBits = serialBits;
        this.indexMask = (1 << indexBits) - 1;
    }

    public String getMagic() {
        return magic;
    }

    public int getCompressedFlag() {
        return compressedFlag;
    }

    public boolean isSendTablesContainer() {
        return sendTablesContainer;
    }

    public Class<? extends GeneratedMessage> demoPacketClassForKind(int kind) {
        return DemoPackets.classForKind(kind);
    }

    public abstract Class<? extends GeneratedMessage> embeddedPacketClassForKind(int kind);
    public abstract Class<? extends GeneratedMessage> userMessagePacketClassForKind(int kind);
    public abstract boolean isUserMessage(Class<? extends GeneratedMessage> clazz);
    public abstract int readEmbeddedKind(BitStream bs);
    public abstract FieldReader getNewFieldReader();
    public abstract DemoHeader readHeader(Source source) throws IOException;

    public abstract int readKind(Source source) throws IOException;
    public abstract int readTick(Source source) throws IOException;
    public abstract int readPlayerSlot(Source source) throws IOException;
    public abstract int readSize(Source source) throws IOException;

    public static EngineType forMagic(String magic) {
        for (EngineType et : values()) {
            if (et.magic.equals(magic)) {
                return et;
            }
        }
        return null;
    }

    public int getIndexBits() {
        return indexBits;
    }

    public int getSerialBits() {
        return serialBits;
    }

    public int indexForHandle(int handle) {
        return handle & indexMask;
    }

    public int serialForHandle(int handle) {
        return handle >> indexBits;
    }

    public int handleForIndexAndSerial(int index, int serial) {
        return serial << indexBits | index;
    }

}
