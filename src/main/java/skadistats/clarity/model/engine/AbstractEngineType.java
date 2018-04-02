package skadistats.clarity.model.engine;

import com.google.protobuf.GeneratedMessage;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.wire.common.DemoPackets;

public abstract class AbstractEngineType implements EngineType {

    private final EngineId identifier;
    private final int compressedFlag;
    private final boolean sendTablesContainer;
    private final int indexBits;
    private final int serialBits;
    private final int indexMask;

    AbstractEngineType(EngineId identifier, int compressedFlag, boolean sendTablesContainer, int indexBits, int serialBits) {
        this.identifier = identifier;
        this.compressedFlag = compressedFlag;
        this.sendTablesContainer = sendTablesContainer;
        this.indexBits = indexBits;
        this.serialBits = serialBits;
        this.indexMask = (1 << indexBits) - 1;
    }

    @Override
    public EngineId getId() {
        return identifier;
    }

    @Override
    public int getCompressedFlag() {
        return compressedFlag;
    }

    @Override
    public boolean isSendTablesContainer() {
        return sendTablesContainer;
    }

    @Override
    public Class<? extends GeneratedMessage> demoPacketClassForKind(int kind) {
        return DemoPackets.classForKind(kind);
    }

    @Override
    public int getIndexBits() {
        return indexBits;
    }

    @Override
    public int getSerialBits() {
        return serialBits;
    }

    @Override
    public int indexForHandle(int handle) {
        return handle & indexMask;
    }

    @Override
    public int serialForHandle(int handle) {
        return handle >> indexBits;
    }

    @Override
    public int handleForIndexAndSerial(int index, int serial) {
        return serial << indexBits | index;
    }

}
