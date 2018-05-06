package skadistats.clarity.model;

import com.google.protobuf.GeneratedMessage;
import skadistats.clarity.decoder.FieldReader;
import skadistats.clarity.decoder.bitstream.BitStream;
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

    float getMillisPerTick();

    boolean isFullPacketSeekAllowed();

    Class<? extends GeneratedMessage> embeddedPacketClassForKind(int kind);
    Class<? extends GeneratedMessage> userMessagePacketClassForKind(int kind);
    boolean isUserMessage(Class<? extends GeneratedMessage> clazz);

    FieldReader getNewFieldReader();

    void readHeader(Source source) throws IOException;
    void skipHeader(Source source) throws IOException;
    void emitHeader();

    int determineLastTick(Source source) throws IOException;
    int readEmbeddedKind(BitStream bs);

    <T extends GeneratedMessage> PacketInstance<T> getNextPacketInstance(Source source) throws IOException;
}
