package skadistats.clarity.source;

import com.google.protobuf.GeneratedMessage;
import com.sun.management.UnixOperatingSystemMXBean;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.SimpleRunner;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class LiveSourceTest {

    @Test(enabled = false)
    @Parameters({"srcFile", "dstFile"})
    public void demoLiveSource(final String srcFile, final String dstFile) throws Exception {
        createWriterThread(srcFile, dstFile);

        LiveSource source = new LiveSource(dstFile, 5, TimeUnit.SECONDS);
        new SimpleRunner(source).runWith(new Object() {
            @OnMessage
            public void onMessage(GeneratedMessage msg) {
                System.out.println(msg.getClass().getSimpleName());
            }
        });
    }

    @Test(enabled = false)
    @Parameters({"srcFile", "dstFile"})
    public void testLiveSourceMMap(final String srcFile, final String dstFile) throws Exception {
        createWriterThread(srcFile, dstFile);
        while (true) {
            long t0 = System.currentTimeMillis();

            Path filePath = Paths.get(dstFile);
            FileChannel open = FileChannel.open(filePath);
            MappedByteBuffer buf = open.map(FileChannel.MapMode.READ_ONLY, 0L, Files.size(filePath));
            long t1 = System.currentTimeMillis();

            OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
            if(os instanceof UnixOperatingSystemMXBean){
                System.out.println("Number of open fd: " + ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount());
            }

            System.out.println(t1 - t0);
            System.out.println(buf.remaining());
            Thread.sleep(500);
        }
    }

    private void createWriterThread(final String srcFile, final String dstFile) {
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
                        Thread.sleep(25);
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

}
