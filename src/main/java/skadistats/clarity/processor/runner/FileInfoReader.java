package skadistats.clarity.processor.runner;

import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.proto.Demo;

import java.io.IOException;

public class FileInfoReader extends AbstractRunner<FileInfoReader> {

    private Demo.CDemoFileInfo fileInfo;

    public FileInfoReader(Source s) throws IOException {
        super(s);
        source.ensureDemoHeader();
        int offset = source.readFixedInt32();
        source.setPosition(offset);
        this.loopController = new LoopController() {
            @Override
            public boolean isTickBorder(int upcomingTick) {
                return processorTick != upcomingTick;
            }
            @Override
            public LoopController.Command doLoopControl(Context ctx, int nextTickWithData) {
                return LoopController.Command.FALLTHROUGH;
            }
        };
        super.runWith(this);
    }

    @OnMessage(Demo.CDemoFileInfo.class)
    public void onFileInfo(Context ctx, Demo.CDemoFileInfo message) throws IOException {
        this.fileInfo = message;
    }

    public Demo.CDemoFileInfo getFileInfo() {
        return fileInfo;
    }

}
