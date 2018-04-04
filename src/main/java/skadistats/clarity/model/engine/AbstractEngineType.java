package skadistats.clarity.model.engine;

import skadistats.clarity.event.Insert;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.processor.packet.PacketReader;
import skadistats.clarity.processor.packet.UsesPacketReader;

@UsesPacketReader
public abstract class AbstractEngineType implements EngineType {

    @Insert
    protected PacketReader packetReader = new PacketReader(); // HACK: create one if we do not get one injected

    private final EngineId id;
    private final boolean sendTablesContainer;
    private final int indexBits;
    private final int serialBits;
    private final int indexMask;

    AbstractEngineType(EngineId id, boolean sendTablesContainer, int indexBits, int serialBits) {
        this.id = id;
        this.sendTablesContainer = sendTablesContainer;
        this.indexBits = indexBits;
        this.serialBits = serialBits;
        this.indexMask = (1 << indexBits) - 1;
    }

    @Override
    public EngineId getId() {
        return id;
    }

    @Override
    public boolean isSendTablesContainer() {
        return sendTablesContainer;
    }

    @Override
    public boolean handleDeletions() {
        return true;
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

    @Override
    public String toString() {
        return id.toString();
    }

}
