package skadistats.clarity.processor.runner;

import skadistats.clarity.source.Source;

import java.io.IOException;

/**
 * Synchronous runner that processes a replay from start to finish in a single pass.
 *
 * <p>The runner does <b>not</b> own the {@link Source}: the caller is responsible
 * for closing it. The recommended pattern is try-with-resources:
 *
 * <pre>{@code
 * try (var source = new MappedFileSource(path)) {
 *     new SimpleRunner(source).runWith(processor);
 * }
 * }</pre>
 *
 * <p>Failing to close the source will leak file descriptors and memory mappings
 * until the JVM Cleaner releases them — see issue #289.
 *
 * <p>Everything runs on the calling thread; {@code runWith} blocks until the replay has been processed.
 */
public class SimpleRunner extends AbstractFileRunner {

    private final LoopController.Func controllerFunc = upcomingTick -> {
        if (!loopController.isSyncTickSeen()) {
            if (tick == -1) {
                startNewTick(0);
            }
            return LoopController.Command.FALLTHROUGH;
        }
        if (upcomingTick != tick) {
            if (upcomingTick != Integer.MAX_VALUE) {
                endTicksUntil(upcomingTick - 1);
                startNewTick(upcomingTick);
            } else {
                endTicksUntil(tick);
            }
        }
        return LoopController.Command.FALLTHROUGH;
    };

    /**
     * @param s the source to read from; the engine type is determined by reading its magic
     * @throws IOException if the source does not contain a valid replay
     */
    public SimpleRunner(Source s) throws IOException {
        super(s, s.determineEngineType());
        this.loopController = new LoopController(controllerFunc);
    }

    /**
     * Processes the whole replay on the calling thread and returns when the end is reached.
     *
     * <p>Ticks are delivered in order. Ticks without replay data are delivered as synthetic ticks
     * ({@code OnTickStart}/{@code OnTickEnd} with {@code synthetic == true}).
     *
     * <p>An exception thrown by an event listener is passed to the {@linkplain #getExceptionHandler() exception
     * handler}; the default handler rethrows it, ending the run.
     *
     * @param processors processor instances whose {@code @On*} methods are registered; arrays are flattened
     * @return this runner
     * @throws IOException if reading from the source fails
     */
    public SimpleRunner runWith(final Object... processors) throws IOException {
        initAndRunWith(processors);
        return this;
    }

}
