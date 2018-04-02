package skadistats.clarity.model.engine;

import skadistats.clarity.model.EngineId;
import skadistats.clarity.source.Source;

import java.io.IOException;

public abstract class AbstractDotaEngineType extends AbstractEngineType {

    public AbstractDotaEngineType(EngineId identifier, int compressedFlag, boolean sendTablesContainer, int indexBits, int serialBits) {
        super(identifier, compressedFlag, sendTablesContainer, indexBits, serialBits);
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

    @Override
    public void readCommandInfo(Source source) throws IOException {
        // do nothing
    }
}
