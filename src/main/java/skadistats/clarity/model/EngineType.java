package skadistats.clarity.model;

import com.google.protobuf.GeneratedMessage;
import skadistats.clarity.decoder.FieldReader;
import skadistats.clarity.decoder.bitstream.BitStream;
import skadistats.clarity.source.Source;

import java.io.IOException;

public interface EngineType {

    EngineId getId();
    int getCompressedFlag();
    boolean isSendTablesContainer();

    int getIndexBits();
    int getSerialBits();
    int indexForHandle(int handle);
    int serialForHandle(int handle);
    int handleForIndexAndSerial(int index, int serial);

    Class<? extends GeneratedMessage> demoPacketClassForKind(int kind);
    Class<? extends GeneratedMessage> embeddedPacketClassForKind(int kind);
    Class<? extends GeneratedMessage> userMessagePacketClassForKind(int kind);
    boolean isUserMessage(Class<? extends GeneratedMessage> clazz);

    FieldReader getNewFieldReader();

    DemoHeader readHeader(Source source) throws IOException;

    int readKind(Source source) throws IOException;
    int readTick(Source source) throws IOException;
    int readPlayerSlot(Source source) throws IOException;
    int readSize(Source source) throws IOException;
    int readEmbeddedKind(BitStream bs);

    void readCommandInfo(Source source) throws IOException;

}
