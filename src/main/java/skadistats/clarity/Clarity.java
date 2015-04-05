package skadistats.clarity;

import com.google.protobuf.ZeroCopy;
import org.xerial.snappy.Snappy;
import skadistats.clarity.source.InputStreamSource;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.PacketTypes;
import skadistats.clarity.wire.proto.Demo;

import java.io.IOException;
import java.io.InputStream;

public class Clarity {

    public static Demo.CDemoFileInfo infoForFile(String fileName) throws IOException {
    	return infoForSource(new MappedFileSource(fileName));
    }

    public static Demo.CDemoFileInfo infoForStream(final InputStream stream) throws IOException {
        return infoForSource(new InputStreamSource(stream));
    }

    public static Demo.CDemoFileInfo infoForSource(final Source src) throws IOException {
        src.ensureDemoHeader();
        src.setPosition(src.readFixedInt32());
        boolean isCompressed = (src.readVarInt32() & Demo.EDemoCommands.DEM_IsCompressed_VALUE) == Demo.EDemoCommands.DEM_IsCompressed_VALUE;
        src.readVarInt32();
        int size = src.readVarInt32();
        byte[] data = src.readBytes(size);
        if (isCompressed) {
            data = Snappy.uncompress(data);
        }
        return PacketTypes.parse(Demo.CDemoFileInfo.class, ZeroCopy.wrap(data));
    }


}
