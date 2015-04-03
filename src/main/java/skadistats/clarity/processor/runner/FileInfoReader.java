package skadistats.clarity.processor.runner;

import skadistats.clarity.decoder.DemoInputStream;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.wire.proto.Demo;

import java.io.IOException;
import java.io.InputStream;

public class FileInfoReader extends AbstractRunner<FileInfoReader> {

    private Demo.CDemoFileInfo fileInfo;

    public FileInfoReader(InputStream inputStream) throws IOException {
        DemoInputStream dis = new DemoInputStream(inputStream);
        int offset = dis.ensureDemHeader();
        dis.skipBytes(offset - 12);
        cis = dis.newCodedInputStream();
        super.runWith(this);
    }

    @Override
    protected Source getSource() {
        return new AbstractSource() {
            @Override
            public LoopControlCommand doLoopControl(Context ctx, int nextTickWithData) {
                return LoopControlCommand.FALLTHROUGH;
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
