package skadistats.clarity.processor.runner;

import skadistats.clarity.engine.EngineType;
import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.s2.S2FieldPathType;
import skadistats.clarity.processor.reader.OnTickEnd;
import skadistats.clarity.processor.reader.OnTickStart;
import skadistats.clarity.source.Source;
import skadistats.clarity.state.s1.S1EntityStateType;
import skadistats.clarity.state.s2.S2EntityStateType;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

/**
 * Base class of the runners that read a replay from a {@link Source}.
 *
 * <p>Provides the {@code with*} configuration methods. They return the runner for chaining and must be
 * called before {@code runWith}. Raises {@link OnInputSource}, {@link skadistats.clarity.processor.reader.OnTickStart}
 * and {@link skadistats.clarity.processor.reader.OnTickEnd}.
 */
@Provides(value = {OnInputSource.class, OnTickStart.class, OnTickEnd.class}, runnerClass = { AbstractFileRunner.class })
public abstract class AbstractFileRunner extends AbstractRunner implements FileRunner {

    @InsertEvent
    private OnTickStart.Event evTickStart;
    @InsertEvent
    private OnTickEnd.Event evTickEnd;

    protected final Source source;
    protected LoopController loopController;
    protected S1EntityStateType s1EntityStateType = S1EntityStateType.FLAT;
    protected S2EntityStateType s2EntityStateType = S2EntityStateType.FLAT;
    protected S2FieldPathType s2FieldPathType = S2FieldPathType.LONG;
    protected Predicate<DTClass> entityFilter;
    private volatile boolean started;

    /* tick the user is at the end of */
    protected int tick;
    /* tick is synthetic (does not contain replay data) */
    protected boolean synthetic = true;

    /**
     * @param source the source to read from
     * @param engineType the engine type determined from {@code source}
     * @throws IOException if reading from the source fails
     */
    public AbstractFileRunner(Source source, EngineType engineType) throws IOException {
        super(engineType);
        this.source = source;
        this.tick = -1;
    }

    @Override
    protected List<Object> infraProcessors() {
        return List.of(this, engineType, engineType.getPacketReader(), source);
    }

    @Override
    protected Context createContext(ExecutionModel em) {
        return new Context(em, s1EntityStateType, s2EntityStateType, s2FieldPathType, entityFilter);
    }

    /**
     * Initializes the processors, emits the replay header and raises {@link OnInputSource}, which starts the read loop.
     *
     * @param processors the processor instances
     * @throws IOException if reading from the source fails
     */
    protected void initAndRunWith(Object... processors) throws IOException {
        markStarted();
        initWithProcessors(processors);
        engineType.emitHeader();
        OnInputSource.Event ev = context.createEvent(OnInputSource.class);
        ev.raise(source, loopController);
    }

    /**
     * Marks the run as started; afterwards the {@code with*} configuration methods throw. Called from
     * {@code runWith} before processing begins, on the calling thread.
     */
    protected void markStarted() {
        started = true;
    }

    private void checkNotStarted() {
        if (started) {
            throw new IllegalStateException("runner configuration cannot be changed after the run has started");
        }
    }

    /**
     * Ends the current tick and, if {@code untilTick} is later, runs through synthetic ticks up to and including it.
     *
     * @param untilTick the last tick to end
     */
    protected void endTicksUntil(int untilTick) {
        while (tick < untilTick) {
            evTickEnd.raise(synthetic);
            setTick(tick + 1);
            synthetic = true;
            evTickStart.raise(synthetic);
        }
        evTickEnd.raise(synthetic);
        synthetic = false;
    }

    /**
     * Advances to the next tick and raises {@code OnTickStart}. The tick is synthetic if it is not {@code upcomingTick}.
     *
     * @param upcomingTick the next tick that carries replay data
     */
    protected void startNewTick(int upcomingTick) {
        setTick(tick + 1);
        synthetic = tick != upcomingTick;
        evTickStart.raise(synthetic);
    }

    /**
     * @param tick the new current tick
     */
    protected void setTick(int tick) {
        this.tick = tick;
    }

    /**
     * @return the current tick; {@code -1} before processing has started
     */
    @Override
    public int getTick() {
        return tick;
    }

    /**
     * @return the source this runner reads from
     */
    @Override
    public Source getSource() {
        return source;
    }

    /**
     * Selects the entity state implementation for Source 1 replays. Default is {@link S1EntityStateType#FLAT}.
     *
     * @param type the implementation
     * @return this runner
     * @throws IllegalStateException if the run has already been started
     */
    public AbstractFileRunner withS1EntityState(S1EntityStateType type) {
        checkNotStarted();
        this.s1EntityStateType = type;
        return this;
    }

    /**
     * Selects the entity state implementation for Source 2 replays. Default is {@link S2EntityStateType#FLAT}.
     *
     * @param type the implementation
     * @return this runner
     * @throws IllegalStateException if the run has already been started
     */
    public AbstractFileRunner withS2EntityState(S2EntityStateType type) {
        checkNotStarted();
        this.s2EntityStateType = type;
        return this;
    }

    /**
     * Selects the field path implementation for Source 2 replays. Default is {@link S2FieldPathType#LONG}.
     *
     * @param type the implementation
     * @return this runner
     * @throws IllegalStateException if the run has already been started
     */
    public AbstractFileRunner withS2FieldPath(S2FieldPathType type) {
        checkNotStarted();
        this.s2FieldPathType = type;
        return this;
    }

    /**
     * Restricts which entities are created. Entities whose {@link DTClass} does not satisfy the filter are
     * skipped while parsing and never appear in {@link skadistats.clarity.processor.entities.Entities}.
     * Default is no filter.
     *
     * @param filter predicate on the entity's class; {@code true} keeps the entity
     * @return this runner
     * @throws IllegalStateException if the run has already been started
     */
    public AbstractFileRunner withEntityFilter(Predicate<DTClass> filter) {
        checkNotStarted();
        this.entityFilter = filter;
        return this;
    }

    /**
     * Returns the last tick of the replay. This calls {@link Source#getLastTick()}, which may reposition the source.
     *
     * @return the last tick
     * @throws IOException if the last tick cannot be determined
     */
    public int getLastTick() throws IOException {
        return source.getLastTick();
    }

}
