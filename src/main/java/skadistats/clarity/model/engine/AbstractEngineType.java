package skadistats.clarity.model.engine;

import com.google.protobuf.GeneratedMessage;
import org.slf4j.Logger;
import skadistats.clarity.LogChannel;
import skadistats.clarity.event.Insert;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.logger.PrintfLoggerFactory;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.reader.PacketInstance;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.source.Source;

import java.io.IOException;

public abstract class AbstractEngineType<H extends GeneratedMessage> implements EngineType {

    protected static final Logger log = PrintfLoggerFactory.getLogger(LogChannel.runner);

    @Insert
    protected Context ctx;

    private final EngineId id;
    protected final PacketInstanceReader<?> packetInstanceReader;
    protected final H header;
    private final boolean sendTablesContainer;
    private final int indexBits;
    private final int serialBits;
    private final int indexMask;
    private final int emptyHandle;

    protected final ContextData contextData = new ContextData();

    AbstractEngineType(EngineId id, PacketInstanceReader<H> packetInstanceReader, H header, boolean sendTablesContainer, int indexBits, int serialBits) {
        this.id = id;
        this.packetInstanceReader = packetInstanceReader;
        this.header = header;
        this.sendTablesContainer = sendTablesContainer;
        this.indexBits = indexBits;
        this.serialBits = serialBits;
        this.indexMask = (1 << indexBits) - 1;
        this.emptyHandle = (1 << (indexBits + 10)) - 1;
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
    public boolean shouldHandleDeletions(BitStream bs) {
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
    public int emptyHandle() {
        return emptyHandle;
    }

    @Override
    public <T extends GeneratedMessage> PacketInstance<T> getNextPacketInstance(Source source) throws IOException {
        return packetInstanceReader.getNextPacketInstance(source);
    }

    @Override
    public Object[] getRegisteredProcessors() {
        return new Object[] {
                this,
                packetInstanceReader.getPacketReader()
        };
    }

    @Override
    public void emitHeader() {
        if (header != null) {
            ctx.createEvent(OnMessage.class, header.getClass()).raise(header);
        }
    }

    @Override
    public String toString() {
        return id.toString();
    }

    @Override
    public ContextData getContextData() {
        return contextData;
    }

}
