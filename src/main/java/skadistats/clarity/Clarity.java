package skadistats.clarity;

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessage;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.processor.reader.PacketInstance;
import skadistats.clarity.source.InputStreamSource;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.Packet;
import skadistats.clarity.wire.common.proto.Demo;
import skadistats.clarity.wire.s2.proto.S2DotaMatchMetadata;

import java.io.FileInputStream;
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
        PacketInstance<GeneratedMessage> pi = engineType.getNextPacketInstance(source);
        return (Demo.CDemoFileInfo) pi.parse();
    }

    /**
     * reads a metadata file and returns it's contents
     *
     * @param fileName path and name of the file on disk
     * @return the {@code CDOTAMatchMetadataFile} protobuf message
     * @throws IOException if the given file is non-existing or is no valid metadata-file
     */
    public static S2DotaMatchMetadata.CDOTAMatchMetadataFile metadataForFile(String fileName) throws IOException {
        return metadataForStream(new FileInputStream(fileName));
    }

    /**
     * reads a metadata stream and returns it's contents
     *
     * @param stream an {@code InputStream}, containing replay data, positioned at the beginning
     * @return the {@code CDOTAMatchMetadataFile} protobuf message
     * @throws IOException if the given stream is invalid
     */
    private static S2DotaMatchMetadata.CDOTAMatchMetadataFile metadataForStream(InputStream stream) throws IOException {
        return Packet.parse(S2DotaMatchMetadata.CDOTAMatchMetadataFile.class, ByteString.readFrom(stream));
    }

}
