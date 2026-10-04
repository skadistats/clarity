package skadistats.clarity.processor.reader;

import skadistats.clarity.protobuf.GeneratedMessage;
import skadistats.clarity.ClarityException;
import skadistats.clarity.source.ResetRelevantKind;

import java.io.IOException;

/**
 * A top-level packet of the input source whose payload has not been read yet. It can either be parsed or skipped.
 *
 * @param <T> the message type of the packet
 */
public interface PacketInstance<T extends GeneratedMessage> {

    /** Sentinel for the end of the source: tick {@link Integer#MAX_VALUE}, kind -1; parsing or skipping it throws. */
    PacketInstance<GeneratedMessage> EOF = new PacketInstance<>() {
        @Override
        public int getKind() {
            return -1;
        }

        @Override
        public int getTick() {
            return Integer.MAX_VALUE;
        }

        @Override
        public Class<GeneratedMessage> getMessageClass() {
            return null;
        }

        @Override
        public ResetRelevantKind getResetRelevantKind() {
            return null;
        }

        @Override
        public GeneratedMessage parse() throws IOException {
            throw new ClarityException("cannot parse EOF");
        }

        @Override
        public void skip() {
            throw new ClarityException("cannot skip EOF");
        }
    };

    /**
     * @return the engine specific message kind of the packet
     */
    int getKind();
    /**
     * @return the tick this packet belongs to
     */
    int getTick();
    /**
     * @return the message class of the packet, or {@code null} if the kind is unknown
     */
    Class<T> getMessageClass();
    /**
     * @return how this packet matters when seeking, or {@code null} if it does not
     */
    ResetRelevantKind getResetRelevantKind();
    /**
     * Reads and parses the payload.
     *
     * @return the message
     * @throws IOException if reading or parsing fails
     */
    T parse() throws IOException;
    /**
     * Skips the payload without parsing it.
     *
     * @throws IOException if skipping fails
     */
    void skip() throws IOException;

}
