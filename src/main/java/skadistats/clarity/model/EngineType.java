package skadistats.clarity.model;

import com.google.protobuf.GeneratedMessage;
import skadistats.clarity.io.FieldReader;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.engine.ContextData;
import skadistats.clarity.processor.reader.PacketInstance;
import skadistats.clarity.source.Source;

import java.io.IOException;

public interface EngineType {

    EngineId getId();
    boolean isSendTablesContainer();
    boolean handleDeletions();

    int getIndexBits();
    int getSerialBits();
    int indexForHandle(int handle);
    int serialForHandle(int handle);
    int handleForIndexAndSerial(int index, int serial);
    int emptyHandle();

    int getInfoOffset();

    boolean isFullPacketSeekAllowed();
    Integer getExpectedFullPacketInterval();

    Class<? extends GeneratedMessage> embeddedPacketClassForKind(int kind);
    Class<? extends GeneratedMessage> userMessagePacketClassForKind(int kind);
    boolean isUserMessage(Class<? extends GeneratedMessage> clazz);

    FieldReader getNewFieldReader();

    void emitHeader();

    int determineLastTick(Source source) throws IOException;
    int readEmbeddedKind(BitStream bs);

    <T extends GeneratedMessage> PacketInstance<T> getNextPacketInstance(Source source) throws IOException;

    Object[] getRegisteredProcessors();

    ContextData getContextData();
}
