package skadistats.clarity.source;

import org.slf4j.Logger;
import skadistats.clarity.ClarityException;
import skadistats.clarity.LogChannel;
import skadistats.clarity.logger.PrintfLoggerFactory;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.wire.common.proto.Demo;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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

    private final Path filePath;

    private SeekableByteChannel fileChannel;
    private boolean demoStopSeen;
    private boolean aborted;
    private boolean timeoutForced;
    private boolean blockingEnabled = true;

    private long currentSize;
    private long lastTickOffset;
    private long nextTickOffset;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition fileChanged = lock.newCondition();

    private final byte[] singleByteBuffer = new byte[1];
    private WatchKey watchKey;

    public LiveSource(String fileName, long timeout, TimeUnit timeUnit) {
        this.timeout = timeout;
        this.timeUnit = timeUnit;

        filePath = Paths.get(fileName).toAbsolutePath();
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
        if (fileChannel == null) {
            return 0;
        } else {
            try {
                return (int) fileChannel.position();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void setPosition(int position) throws IOException {
        if (fileChannel == null) {
            throw new IOException("file is not existing");
        }
        if (demoStopSeen && position < fileChannel.position()) {
            demoStopSeen = false;
        }
        fileChannel.position(position);
    }

    @Override
    public byte readByte() throws IOException {
        readBytes(singleByteBuffer, 0, 1);
        return singleByteBuffer[0];
    }

    @Override
    public void readBytes(byte[] dest, int offset, int length) throws IOException {
        blockUntilDataAvailable(length);
        fileChannel.read(ByteBuffer.wrap(dest, offset, length));
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
                watchService.take();
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
            if (nowExisting ^ (fileChannel != null)) {
                demoStopSeen = false;
                resetLastTick();
                if (nowExisting) {
                    fileChannel = Files.newByteChannel(filePath, StandardOpenOption.READ);
                } else {
                    fileChannel.close();
                    fileChannel = null;
                }
            }
            if (fileChannel != null) {
                currentSize = fileChannel.size();
                scanForLastTick();
            } else {
                currentSize = 0L;
            }
            log.info("file change  for %s, existing: %s, fileSize: %d", filePath, fileChannel != null, currentSize);
            fileChanged.signalAll();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    private void resetLastTick() {
        lastTickOffset = -1L;
        nextTickOffset = 0L;
        setLastTick(0);
    }

    private void scanForLastTick() {
        if (lastTickOffset >= currentSize) {
            // file size decreased
            resetLastTick();
        }
        if (nextTickOffset > currentSize) {
            // nothing to do
            return;
        }
        blockingEnabled = false;
        Long backupPosition = null;
        try {
            backupPosition = fileChannel.position();
            while (nextTickOffset <= currentSize) {
                if (nextTickOffset == 0L) {
                    fileChannel.position(0L);
                    EngineType engineType = readEngineType();
                    engineType.skipHeaderOffsets(this);
                    skipVarInt32(); // kind
                    nextTickOffset = fileChannel.position();
                } else {
                    fileChannel.position(nextTickOffset);
                    if (lastTickOffset < nextTickOffset) {
                        setLastTick(readVarInt32());
                        lastTickOffset = nextTickOffset;
                    } else {
                        skipVarInt32();
                    }
                    skipBytes(readVarInt32()); // size + packet
                    skipVarInt32(); // kind
                    nextTickOffset = fileChannel.position();
                }
            }
        } catch (IOException e) {
            //e.printStackTrace();
        } finally {
            try {
                log.warn("last tick determined to be %d", getLastTick());
            } catch (IOException e) {
                // should not happen
            }
            blockingEnabled = true;
            if (backupPosition != null) {
                try {
                    fileChannel.position(backupPosition);
                } catch (IOException e) {
                    throw new ClarityException("failed to restore position backup", e);
                }
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
                if (demoStopSeen) {
                    throw new EOFException();
                }
                if (fileChannel != null && fileChannel.position() + length < currentSize) {
                    dispose = false;
                    return;
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
