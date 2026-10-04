/**
 * Runners that drive the parsing of a replay and deliver events to your processors.
 *
 * <p>A <em>processor</em> is a plain object whose methods carry {@code @On*} annotations (see
 * {@link skadistats.clarity.processor.reader}, {@link skadistats.clarity.processor.entities} and the other
 * processor packages). A runner is created from a {@link skadistats.clarity.source.Source}, wires the processors
 * you pass to {@code runWith} together with the built-in processors they depend on, and reads the replay.
 * Handler methods may take a leading {@link skadistats.clarity.processor.runner.Context} parameter, which gives
 * access to the current tick, the engine type and other processors
 * ({@link skadistats.clarity.processor.runner.Context#getProcessor(Class)}). Dependencies on built-in processors
 * are declared with {@code @Uses*} annotations such as
 * {@link skadistats.clarity.processor.entities.UsesEntities}; the instance can then be obtained with
 * {@code @Insert} (see {@link skadistats.clarity.event.Insert}) or through the context.
 *
 * <pre>{@code
 * @UsesEntities
 * public class HeroLogger {
 *
 *     @OnEntityCreated(classPattern = "CDOTA_Unit_Hero_.*")
 *     public void onHeroCreated(Context ctx, Entity e) {
 *         System.out.println(ctx.getTick() + " " + e.getDtClass().getDtName());
 *     }
 *
 *     public static void main(String[] args) throws Exception {
 *         try (var source = new MappedFileSource(args[0])) {
 *             new SimpleRunner(source).runWith(new HeroLogger());
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p>The runners do not close the {@code Source}; close it yourself after the run.
 *
 * <ul>
 *   <li>{@link skadistats.clarity.processor.runner.SimpleRunner}: processes the replay from start to finish on the
 *       calling thread; {@code runWith} returns when the replay has been read.</li>
 *   <li>{@link skadistats.clarity.processor.runner.ControllableRunner}: processes on a separate thread and lets a
 *       controlling thread move between ticks with {@code seek(int)} and {@code tick()}; it stops at the end of the
 *       requested tick. Backwards seeking needs a seekable source such as
 *       {@link skadistats.clarity.source.MappedFileSource}.</li>
 *   <li>{@link skadistats.clarity.processor.runner.RealtimeRunner}: a {@code SimpleRunner} that sleeps so ticks
 *       are delivered at the replay's tick rate.</li>
 * </ul>
 *
 * <p>The {@code with*} methods of the file runners ({@code withEntityFilter}, {@code withS1EntityState},
 * {@code withS2EntityState}, {@code withS2FieldPath}) configure the run and must be called before {@code runWith}.
 * {@link skadistats.clarity.processor.runner.OnInit} is raised once after wiring, before any replay data is read.
 * {@code ExecutionModel}, {@code LoopController} and {@code OnInputSource} are internals.
 */
package skadistats.clarity.processor.runner;
