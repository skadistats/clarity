package skadistats.clarity.processor.runner;

import com.google.protobuf.CodedInputStream;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.wire.proto.Demo;

import java.io.IOException;
import java.io.InputStream;

public class FileInfoReader extends AbstractRunner {

    private final CodedInputStream cis;
    private Demo.CDemoFileInfo fileInfo;

    public FileInfoReader(InputStream inputStream) throws IOException {
        int offset = ensureDemHeader(inputStream);
        inputStream.skip(offset - 12);
        this.cis = createCodedInputStream(inputStream);
        super.runWith(this);
    }

    @Override
    protected Source getSource() {
        return new AbstractSource() {
            @Override
            public CodedInputStream stream() {
                return cis;
            }
        };
    }

    @OnMessage(Demo.CDemoFileInfo.class)
    public void onFileInfo(Context ctx, Demo.CDemoFileInfo message) throws IOException {
        this.fileInfo = message;
    }

    public Demo.CDemoFileInfo getFileInfo() {
        return fileInfo;
    }

}
