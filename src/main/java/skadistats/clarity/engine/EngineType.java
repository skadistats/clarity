package skadistats.clarity.engine;

import com.google.protobuf.GeneratedMessage;
import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.common.DemoPackets;
import skadistats.clarity.wire.common.proto.Demo;

import java.io.IOException;

public enum EngineType {

    SOURCE1(
        "PBUFDEM\0",
        Demo.EDemoCommands.DEM_IsCompressed_S1_VALUE,
        false, // has 4 extra header bytes
        true   // CDemoSendTables is container
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
            return skadistats.clarity.wire.s1.UserMessagePackets.isKnownClass(clazz);
        }
        @Override
        public int readEmbeddedKind(BitStream bs) {
            return bs.readVarInt();
        }
    },

    SOURCE2(
        "PBDEMS2\0",
        Demo.EDemoCommands.DEM_IsCompressed_S2_VALUE,
        true, // has 4 extra header bytes
        false // CDemoSendTables is container
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
            return bs.readEntityIndex(-1);
        }
    };

    private final String magic;
    private final int compressedFlag;
    private final boolean extraHeaderInt32;
    private final boolean sendTablesContainer;


    EngineType(String magic, int compressedFlag, boolean extraHeaderInt32, boolean sendTablesContainer) {
        this.magic = magic;
        this.compressedFlag = compressedFlag;
        this.extraHeaderInt32 = extraHeaderInt32;
        this.sendTablesContainer = sendTablesContainer;
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

    public void skipHeaderOffsets(Source source) throws IOException {
        source.skipBytes(extraHeaderInt32 ? 8 : 4);
    }

    public Class<? extends GeneratedMessage> demoPacketClassForKind(int kind) {
        return DemoPackets.classForKind(kind);
    }

    public abstract Class<? extends GeneratedMessage> embeddedPacketClassForKind(int kind);
    public abstract Class<? extends GeneratedMessage> userMessagePacketClassForKind(int kind);
    public abstract boolean isUserMessage(Class<? extends GeneratedMessage> clazz);
    public abstract int readEmbeddedKind(BitStream bs);

    public static EngineType forMagic(String magic) {
        for (EngineType et : values()) {
            if (et.magic.equals(magic)) {
                return et;
            }
        }
        return null;
    }

}
