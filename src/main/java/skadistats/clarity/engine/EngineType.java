package skadistats.clarity.engine;

import com.google.protobuf.GeneratedMessage;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.common.DemoPackets;
import skadistats.clarity.wire.common.proto.Demo;

import java.io.IOException;

public enum EngineType {

    SOURCE1("PBUFDEM\0", Demo.EDemoCommands.DEM_IsCompressed_S1_VALUE, false) {
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
    },

    SOURCE2("PBDEMS2\0", Demo.EDemoCommands.DEM_IsCompressed_S2_VALUE, true) {
        @Override
        public Class<? extends GeneratedMessage> embeddedPacketClassForKind(int kind) {
            return skadistats.clarity.wire.s2.EmbeddedPackets.classForKind(kind);
        }

        @Override
        public Class<? extends GeneratedMessage> userMessagePacketClassForKind(int kind) {
            return skadistats.clarity.wire.s2.UserMessagePackets.classForKind(kind);
        }
        @Override
        public boolean isUserMessage(Class<? extends GeneratedMessage> clazz) {
            return skadistats.clarity.wire.s2.UserMessagePackets.isKnownClass(clazz);
        }
    };

    private final String magic;
    private final int compressedFlag;
    private final boolean extraHeaderInt32;

    EngineType(String magic, int compressedFlag, boolean extraHeaderInt32) {
        this.magic = magic;
        this.compressedFlag = compressedFlag;
        this.extraHeaderInt32 = extraHeaderInt32;
    }

    public String getMagic() {
        return magic;
    }

    public int getCompressedFlag() {
        return compressedFlag;
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

    public static EngineType forMagic(String magic) {
        for (EngineType et : values()) {
            if (et.magic.equals(magic)) {
                return et;
            }
        }
        return null;
    }

}
