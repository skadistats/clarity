package skadistats.clarity;

import skadistats.clarity.processor.reader.InputStreamProcessor;
import skadistats.clarity.processor.reader.OnFileInfoOffset;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.wire.proto.Demo;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Clarity {

    public static class InfoRetriever {
        private Demo.CDemoFileInfo fileInfo;
        @OnFileInfoOffset
        public void onFileInfoOffset(Context ctx, int offset) throws IOException {
            ctx.getProcessor(InputStreamProcessor.class).skipBytes(offset - 12);
        }
        @OnMessage(Demo.CDemoFileInfo.class)
        public void onFileInfo(Context ctx, Demo.CDemoFileInfo message) throws IOException {
            this.fileInfo = message;
        }
        public Demo.CDemoFileInfo retrieve(InputStream stream) {
            new SimpleRunner().runWith(stream, this);
            return fileInfo;
        }
    }

    public static Demo.CDemoFileInfo infoForFile(String fileName) throws IOException {
    	return infoForStream(new FileInputStream(fileName));
    }

    public static Demo.CDemoFileInfo infoForStream(final InputStream stream) throws IOException {
        return new InfoRetriever().retrieve(stream);
    }

}
