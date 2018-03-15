package skadistats.clarity.source;

import com.google.protobuf.GeneratedMessage;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.SimpleRunner;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.concurrent.TimeUnit;

public class LiveSourceTest {

    @Test
    @Parameters({"srcFile", "dstFile"})
    public void demoLiveSource(final String srcFile, final String dstFile) throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    FileInputStream src = new FileInputStream(srcFile);
                    FileOutputStream dst = new FileOutputStream(dstFile);
                    byte[] buf = new byte[8192];
                    int n = buf.length;
                    while (n == buf.length) {
                        n = src.read(buf);
                        dst.write(buf, 0, n);
                        Thread.sleep(100);
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        LiveSource source = new LiveSource(dstFile, 5, TimeUnit.SECONDS);
        new SimpleRunner(source).runWith(new Object() {
            @OnMessage
            public void onMessage(GeneratedMessage msg) {
                System.out.println(msg.getClass().getSimpleName());
            }
        });
    }

}