package skadistats.clarity;

import com.google.protobuf.ZeroCopy;
import org.xerial.snappy.Snappy;
import skadistats.clarity.engine.EngineType;
import skadistats.clarity.source.InputStreamSource;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.s1.PacketTypes;
import skadistats.clarity.wire.s1.proto.Demo;

import java.io.IOException;
import java.io.InputStream;

public class Clarity {

    /**
     * Retrieves summary-data from the given demo-file
     *
     * @param fileName path and name of the file on disk
     * @return the {@code CDemoFileInfo} protobuf message
     * @throws IOException if the given file is non-existing or is no valid demo-file
     */
    public static Demo.CDemoFileInfo infoForFile(String fileName) throws IOException {
        return infoForSource(new MappedFileSource(fileName));
    }

    /**
     * Retrieves summary-data from the given input stream
     *
     * @param stream an {@code InputStream}, containing replay data, positioned at the beginning
     * @return the {@code CDemoFileInfo} protobuf message
     * @throws IOException if the given stream is invalid
     */
    public static Demo.CDemoFileInfo infoForStream(final InputStream stream) throws IOException {
        return infoForSource(new InputStreamSource(stream));
    }

    /**
     * Retrieves summary-data from the given input source
     *
     * @param source the {@code Source} providing the replay data
     * @return the {@code CDemoFileInfo} protobuf message
     * @throws IOException if the given source is invalid
     * @see Source
     */
    public static Demo.CDemoFileInfo infoForSource(final Source source) throws IOException {
        EngineType engineType = source.readEngineType();
        source.setPosition(source.readFixedInt32());
        int kind = source.readVarInt32();
        boolean isCompressed = (kind & engineType.getCompressedFlag()) == engineType.getCompressedFlag();
        source.skipVarInt32();
        int size = source.readVarInt32();
        byte[] data = source.readBytes(size);
        if (isCompressed) {
            data = Snappy.uncompress(data);
        }
        return PacketTypes.parse(Demo.CDemoFileInfo.class, ZeroCopy.wrap(data));
    }

}
