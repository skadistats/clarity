package skadistats.clarity.source;

import com.google.protobuf.GeneratedMessage;
import org.slf4j.Logger;
import skadistats.clarity.ClarityException;
import skadistats.clarity.LogChannel;
import skadistats.clarity.logger.PrintfLoggerFactory;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.reader.PacketInstance;
import skadistats.clarity.wire.common.proto.Demo;
import sun.nio.ch.DirectBuffer;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class LiveSource extends Source {

    protected static final Logger log = PrintfLoggerFactory.getLogger(LogChannel.runner);

    private final long timeout;
    private final TimeUnit timeUnit;

    private WatchService watchService = null;
    private WatchKey watchKey;

    private final Path filePath;

    private FileChannel channel;
    private MappedByteBuffer file;

    private boolean demoStopSeen;
    private boolean aborted;
    private boolean timeoutForced;
    private boolean blockingEnabled = true;

    private int lastTickOffset;
    private int nextTickOffset;
    private EngineType engineType;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition fileChanged = lock.newCondition();


    public LiveSource(String fileName, long timeout, TimeUnit timeUnit) {
        this(Paths.get(fileName), timeout, timeUnit);
    }

    public LiveSource(File file, long timeout, TimeUnit timeUnit) {
        this(file.toPath(), timeout, timeUnit);
    }

    public LiveSource(Path filePath, long timeout, TimeUnit timeUnit) {
        this.timeout = timeout;
        this.timeUnit = timeUnit;

        this.filePath = filePath.toAbsolutePath();
        resetLastTick();
        handleFileChange();

        final Thread watcherThread = new Thread(new Runnable() {
            @Override
            public void run() {
                watcherThread();
            }
        });
        watcherThread.setName("clarity-livesource-watcher");
        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    @Override
    public int getPosition() {
        return file == null ? 0 : file.position();
    }

    @Override
    public void setPosition(int position) throws IOException {
        if (file == null) {
            throw new IOException("file is not existing");
        }
        if (demoStopSeen && position < file.position()) {
            demoStopSeen = false;
        }
        blockUntilDataAvailable(position - file.position());
        file.position(position);
    }

    @Override
    public byte readByte() throws IOException {
        blockUntilDataAvailable(1);
        return file.get();
    }

    @Override
    public void readBytes(byte[] dest, int offset, int length) throws IOException {
        blockUntilDataAvailable(length);
        file.get(dest, offset, length);
    }

    @Override
    public int getLastTick() throws IOException {
        lock.lock();
        try {
            return super.getLastTick();
        } finally {
            lock.unlock();
        }
    }

    private void open() throws IOException {
        close();
        channel = FileChannel.open(filePath);
        file = channel.map(FileChannel.MapMode.READ_ONLY, 0L, Files.size(filePath));
    }

    @Override
    public void close() throws IOException {
        if (channel != null) {
            channel.close();
            channel = null;
        }
        if (file != null) {
            ((DirectBuffer) file).cleaner().clean();
            file = null;
        }
    }

    public void stop() {
        lock.lock();
        try {
            aborted = true;
            fileChanged.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void forceTimeout() {
        lock.lock();
        try {
            timeoutForced = true;
            fileChanged.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private void watcherThread() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            log.debug("starting watcher for directory %s", filePath.getParent());
            watchKey = filePath.getParent().register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY
            );
            boolean stillValid = true;
            while (stillValid) {
                if (watchService.poll(250, TimeUnit.MILLISECONDS) == null) {
                    try {
                        // workaround Windows not detecting modified events
                        filePath.toFile().length();
                    } catch (Exception e) {
                        // ignore any errors
                    }
                    continue;
                }
                for (WatchEvent<?> event : watchKey.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (Path.class.isAssignableFrom(kind.type())) {
                        Path affectedPath = (Path) event.context();
                        if (filePath.getParent().resolve(affectedPath).equals(filePath)) {
                            handleFileChange();
                        }
                    }
                }
                stillValid = watchKey.reset();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            disposeWatchService();
        }
    }

    private void disposeWatchService() {
        if (watchKey.isValid()) {
            watchKey.cancel();
        }
    }

    private void handleFileChange() {
        lock.lock();
        try {
            boolean nowExisting = Files.isReadable(filePath);
            if (nowExisting ^ (file != null)) {
                demoStopSeen = false;
                resetLastTick();
                if (!nowExisting) {
                    close();
                }
            }
            if (nowExisting) {
                int pos = getPosition();
                open();
                setPosition(Math.min(pos, file.capacity() - 1));
                scanForLastTick();
            }
            log.debug("file change  for %s, existing: %s, fileSize: %d", filePath, file != null, file.capacity());
            fileChanged.signalAll();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    private void resetLastTick() {
        lastTickOffset = -1;
        nextTickOffset = 0;
        setLastTick(0);
    }

    private void scanForLastTick() {
        if (lastTickOffset >= file.capacity()) {
            // file size decreased
            resetLastTick();
        }
        if (nextTickOffset > file.capacity()) {
            // nothing to do
            return;
        }
        blockingEnabled = false;
        Integer backupPosition = null;
        try {
            backupPosition = file.position();
            while (nextTickOffset <= file.capacity()) {
                if (nextTickOffset == 0) {
                    file.position(0);
                    engineType = readEngineType();
                    engineType.skipHeader(this);
                    nextTickOffset = file.position();
                } else {
                    file.position(nextTickOffset);
                }
                PacketInstance<GeneratedMessage> pi = engineType.getNextPacketInstance(this);
                if (lastTickOffset < nextTickOffset) {
                    setLastTick(pi.getTick());
                    lastTickOffset = nextTickOffset;
                }
                pi.skip();
                nextTickOffset = file.position();
            }
        } catch (IOException e) {
            //e.printStackTrace();
        } finally {
            try {
                log.debug("last tick determined to be %d", getLastTick());
            } catch (IOException e) {
                // should not happen
            }
            blockingEnabled = true;
            if (backupPosition != null) {
                file.position(backupPosition);
            }
        }
    }

    private void blockUntilDataAvailable(int length) throws IOException {
        lock.lock();
        boolean dispose = true;
        try {
            while (true) {
                if (aborted) {
                    throw new AbortedException("aborted");
                }
                if (timeoutForced) {
                    throw new TimeoutException("forced timeout");
                }
                if (file != null && file.remaining() >= length) {
                    dispose = false;
                    return;
                }
                if (demoStopSeen) {
                    throw new EOFException();
                }
                if (blockingEnabled) {
                    if (!fileChanged.await(timeout, timeUnit)) {
                        throw new TimeoutException("timeout while waiting for data");
                    }
                } else {
                    dispose = false;
                    throw new EOFException();
                }
            }
        } catch (InterruptedException e) {
            throw new IOException("interrupted while waiting for available data", e);
        } finally {
            if (dispose) {
                disposeWatchService();
            }
            lock.unlock();
        }
    }

    @OnMessage(Demo.CDemoStop.class)
    public void onDemoStop(Demo.CDemoStop msg) {
        lock.lock();
        try {
            demoStopSeen = true;
            fileChanged.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public static class TimeoutException extends ClarityException {
        public TimeoutException(String format, Object... parameters) {
            super(format, parameters);
        }
    }

    public static class AbortedException extends ClarityException {
        public AbortedException(String format, Object... parameters) {
            super(format, parameters);
        }
    }

}
