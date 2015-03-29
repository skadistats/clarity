package skadistats.clarity;

import skadistats.clarity.processor.runner.FileInfoReader;
import skadistats.clarity.wire.proto.Demo;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Clarity {

    public static Demo.CDemoFileInfo infoForFile(String fileName) throws IOException {
    	return infoForStream(new FileInputStream(fileName));
    }

    public static Demo.CDemoFileInfo infoForStream(final InputStream stream) throws IOException {
        return new FileInfoReader(stream).getFileInfo();
    }

}
