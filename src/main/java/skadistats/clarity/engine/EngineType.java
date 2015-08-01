package skadistats.clarity.engine;

import com.google.protobuf.GeneratedMessage;

public enum EngineType {

    SOURCE1(
        "PBUFDEM\0",
        skadistats.clarity.wire.s1.proto.Demo.EDemoCommands.DEM_IsCompressed_VALUE,
        skadistats.clarity.wire.s1.proto.Demo.CDemoFileInfo.class
    ),

    SOURCE2(
        "PBDEMS2\0",
        skadistats.clarity.wire.s2.proto.Demo.EDemoCommands.DEM_IsCompressed_VALUE,
        skadistats.clarity.wire.s2.proto.Demo.CDemoFileInfo.class
    );

    private final String magic;
    private final int compressedFlag;
    private final Class<? extends GeneratedMessage> fileInfoClass;

    EngineType(String magic, int compressedFlag, Class<? extends GeneratedMessage> fileInfoClass) {
        this.magic = magic;
        this.compressedFlag = compressedFlag;
        this.fileInfoClass = fileInfoClass;
    }

    public String getMagic() {
        return magic;
    }

    public int getCompressedFlag() {
        return compressedFlag;
    }

    public Class<? extends GeneratedMessage> getFileInfoClass() {
        return fileInfoClass;
    }

    public static EngineType forMagic(String magic) {
        for (EngineType et : values()) {
            if (et.magic.equals(magic)) {
                return et;
            }
        }
        return null;
    }

}
