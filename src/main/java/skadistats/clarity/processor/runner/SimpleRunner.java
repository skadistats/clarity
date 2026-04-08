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

    public SimpleRunner(Source s) throws IOException {
        super(s, s.determineEngineType());
        this.loopController = new LoopController(controllerFunc);
    }

    public SimpleRunner runWith(final Object... processors) throws IOException {
        initAndRunWith(processors);
        return this;
    }

}
