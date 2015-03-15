package skadistats.clarity;

import com.dota2.proto.Demo.CDemoFileInfo;
import skadistats.clarity.processor.reader.InputStreamProcessor;
import skadistats.clarity.processor.reader.OnFileInfoOffset;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.Runner;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Clarity {

    public static class InfoRetriever {
        private CDemoFileInfo fileInfo;
        @OnFileInfoOffset
        public void onFileInfoOffset(Context ctx, int offset) throws IOException {
            ctx.getProcessor(InputStreamProcessor.class).skipBytes(offset - 12);
        }
        @OnMessage(CDemoFileInfo.class)
        public void onFileInfo(Context ctx, CDemoFileInfo message) throws IOException {
            this.fileInfo = message;
        }
        public CDemoFileInfo retrieve(InputStream stream) {
            new Runner().runWith(stream, this);
            return fileInfo;
        }
    }

    public static CDemoFileInfo infoForFile(String fileName) throws IOException {
    	return infoForStream(new FileInputStream(fileName));
    }

    public static CDemoFileInfo infoForStream(final InputStream stream) throws IOException {
        return new InfoRetriever().retrieve(stream);
    }

}
