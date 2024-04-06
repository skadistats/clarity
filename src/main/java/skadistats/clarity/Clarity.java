package skadistats.clarity;

import com.google.protobuf.ByteString;
import skadistats.clarity.source.InputStreamSource;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.Packet;
import skadistats.clarity.wire.dota.s2.proto.DOTAS2MatchMetadata;
import skadistats.clarity.wire.shared.demo.proto.Demo;

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
        var engineType = source.determineEngineType();
        source.setPosition(engineType.getInfoOffset());
        var pi = engineType.getNextPacketInstance(source);
        return (Demo.CDemoFileInfo) pi.parse();
    }

    /**
     * reads a metadata file and returns it's contents
     *
     * @param fileName path and name of the file on disk
     * @return the {@code CDOTAMatchMetadataFile} protobuf message
     * @throws IOException if the given file is non-existing or is no valid metadata-file
     */
    public static DOTAS2MatchMetadata.CDOTAMatchMetadataFile metadataForFile(String fileName) throws IOException {
        return metadataForStream(new FileInputStream(fileName));
    }

    /**
     * reads a metadata stream and returns it's contents
     *
     * @param stream an {@code InputStream}, containing replay data, positioned at the beginning
     * @return the {@code CDOTAMatchMetadataFile} protobuf message
     * @throws IOException if the given stream is invalid
     */
    private static DOTAS2MatchMetadata.CDOTAMatchMetadataFile metadataForStream(InputStream stream) throws IOException {
        return Packet.parse(DOTAS2MatchMetadata.CDOTAMatchMetadataFile.class, ByteString.readFrom(stream));
    }

}
