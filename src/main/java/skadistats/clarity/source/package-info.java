/**
 * Sources of raw replay data for the runners.
 *
 * <p>Use {@link skadistats.clarity.source.MappedFileSource} for replay files on disk; it supports seeking in both
 * directions, which {@link skadistats.clarity.processor.runner.ControllableRunner} needs.
 * {@link skadistats.clarity.source.InputStreamSource} reads any {@link java.io.InputStream} but only moves forward.
 * {@link skadistats.clarity.source.LiveSource} follows a replay file that is still being written and blocks until
 * more data arrives.
 *
 * <p>Sources are {@link java.io.Closeable}; the runners do not close them.
 * {@link skadistats.clarity.source.PacketPosition} and {@link skadistats.clarity.source.ResetRelevantKind} are used
 * by the runners to plan seeks.
 */
package skadistats.clarity.source;
