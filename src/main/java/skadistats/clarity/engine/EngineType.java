package skadistats.clarity.engine;

import skadistats.clarity.protobuf.GeneratedMessage;
import skadistats.clarity.io.FieldReader;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.model.s2.S2FieldPathType;
import skadistats.clarity.processor.packet.PacketReader;
import skadistats.clarity.processor.reader.PacketInstance;
import skadistats.clarity.source.Source;

import java.io.IOException;

/**
 * Engine-specific behaviour of a replay: demo file layout, entity handle
 * encoding, packet class lookup and field reader creation. One instance is
 * created per replay by {@link skadistats.clarity.model.EngineMagic}; the
 * implementations cover Dota 2 (S1, S2), CS:GO and CS2 and Deadlock.
 */
public interface EngineType {

    /**
     * @return the game/engine this replay was recorded with
     */
    EngineId getId();
    /**
     * @return true if the send tables are wrapped in a {@code CDemoSendTables} container message (Source 1)
     */
    boolean isSendTablesContainer();
    /**
     * Whether the entity deletion section of a packet entities message is to be read.
     * Always true for Dota S1, always false for CS:GO; for the Source 2 engines true
     * if more than 12 bits remain in {@code bs}.
     */
    boolean shouldHandleDeletions(BitStream bs);

    /**
     * @return the number of bits of an entity handle holding the entity index
     */
    int getIndexBits();
    /**
     * @return the number of bits used to transmit an entity serial
     */
    int getSerialBits();
    /**
     * @return the entity index encoded in {@code handle}
     */
    int indexForHandle(int handle);
    /**
     * @return the entity serial encoded in {@code handle}
     */
    int serialForHandle(int handle);
    /**
     * @return the handle for the given entity index and serial
     */
    int handleForIndexAndSerial(int index, int serial);
    /**
     * @return the handle value that denotes no entity
     */
    int emptyHandle();

    /**
     * @return the file offset of the {@code CDemoFileInfo} message
     * @throws UnsupportedOperationException for CS:GO, whose replays have none
     */
    int getInfoOffset();

    /**
     * @return the demo file header message ({@code CDemoFileHeader}, or the CS:GO demo header)
     */
    GeneratedMessage getHeader();

    /**
     * @return true if seeking backwards can use full packets (all engines except CS:GO)
     */
    boolean isFullPacketSeekAllowed();
    /**
     * @return the expected tick distance between full packets, or {@code null} if the engine has none (CS:GO)
     */
    Integer getExpectedFullPacketInterval();

    /**
     * @return the message class for an embedded packet kind
     */
    Class<? extends GeneratedMessage> embeddedPacketClassForKind(int kind);
    /**
     * @return the message class for a user message kind
     * @throws UnsupportedOperationException for the Source 2 engines
     */
    Class<? extends GeneratedMessage> userMessagePacketClassForKind(int kind);
    /**
     * @return true if {@code clazz} is a user message class of this engine (always false for the Source 2 engines)
     */
    boolean isUserMessage(Class<? extends GeneratedMessage> clazz);

    /**
     * Creates a new field reader for this engine. For the Source 2 engines it uses {@link S2FieldPathType#LONG}.
     */
    FieldReader getNewFieldReader();

    /**
     * Creates a new field reader using the given field path implementation. The Source 2
     * engines honour {@code pathType}; the default implementation, used by the Source 1
     * engines, ignores it.
     */
    default FieldReader getNewFieldReader(S2FieldPathType pathType) {
        return getNewFieldReader();
    }

    /** Internal to the parser: raises the header message as an {@code OnMessage} event, if there is one. */
    void emitHeader();

    /**
     * @return the last tick of the replay, read from the file info (CS:GO: from the header)
     */
    int determineLastTick(Source source) throws IOException;
    /**
     * @return the embedded packet kind read from {@code bs}
     */
    int readEmbeddedKind(BitStream bs);

    /**
     * @return the next packet instance from {@code source}
     */
    <T extends GeneratedMessage> PacketInstance<T> getNextPacketInstance(Source source) throws IOException;

    /** Internal to the parser. */
    PacketReader getPacketReader();
}
