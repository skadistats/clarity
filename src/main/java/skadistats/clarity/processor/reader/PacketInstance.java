package skadistats.clarity.processor.reader;

import com.google.protobuf.GeneratedMessage;

import java.io.IOException;

public interface PacketInstance<T extends GeneratedMessage> {

    PacketInstance<GeneratedMessage> EOF = new PacketInstance<GeneratedMessage>() {
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
        public GeneratedMessage parse() throws IOException {
            return null;
        }

        @Override
        public void skip() {
        }
    };

    int getKind();
    int getTick();
    Class<T> getMessageClass();
    T parse() throws IOException;
    void skip() throws IOException;

}
