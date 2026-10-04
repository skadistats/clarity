package skadistats.clarity.processor.runner;

import skadistats.clarity.engine.EngineType;
import skadistats.clarity.event.Event;
import skadistats.clarity.io.FieldReader;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.s1.S1DTClass;
import skadistats.clarity.model.s2.S2DTClass;
import skadistats.clarity.model.s2.S2FieldPathType;
import skadistats.clarity.state.EntityState;
import skadistats.clarity.state.s1.S1EntityStateType;
import skadistats.clarity.state.s2.FieldLayoutBuilder;
import skadistats.clarity.state.s2.S2EntityStateType;

import java.lang.annotation.Annotation;
import java.util.function.Predicate;

/**
 * Per-run state shared by all processors of a runner: access to other processors, the current tick, engine type
 * and values learned from the replay. A processor receives it by declaring a leading {@code Context} parameter on
 * an {@code @On*} handler method.
 *
 * <p>The replay-derived values ({@link #getBuildNumber()}, {@link #getGameVersion()}, {@link #getMillisPerTick()},
 * {@link #getPointerCount()}) are set once while the replay header and the first messages are processed and have
 * sentinel values before that.
 */
public class Context {

    private final ExecutionModel executionModel;

    // structural (set at construction, immutable)
    private final S1EntityStateType s1EntityStateType;
    private final S2EntityStateType s2EntityStateType;
    private final S2FieldPathType s2FieldPathType;
    private final FieldLayoutBuilder layoutBuilder;
    private final Predicate<DTClass> entityFilter;

    // parse-time initialized (set-once)
    private int buildNumber = -1;
    private boolean buildNumberSet;
    private float millisPerTick = Float.NaN;
    private boolean millisPerTickSet;
    private int gameVersion = -1;
    private boolean gameVersionSet;
    private int pointerCount = 0;
    private boolean pointerCountSet;

    /**
     * Created by the runner.
     *
     * @param executionModel the execution model of the run
     * @param s1EntityStateType entity state implementation for Source 1
     * @param s2EntityStateType entity state implementation for Source 2
     * @param s2FieldPathType field path implementation for Source 2
     * @param entityFilter filter on entity classes; may be {@code null}
     */
    public Context(ExecutionModel executionModel, S1EntityStateType s1EntityStateType, S2EntityStateType s2EntityStateType, S2FieldPathType s2FieldPathType, Predicate<DTClass> entityFilter) {
        this.executionModel = executionModel;
        this.s1EntityStateType = s1EntityStateType;
        this.s2EntityStateType = s2EntityStateType;
        this.s2FieldPathType = s2FieldPathType;
        this.layoutBuilder = new FieldLayoutBuilder();
        this.entityFilter = entityFilter;
    }

    // --- construction API ---

    /**
     * Creates an empty entity state for the given class, using the configured entity state implementation.
     *
     * @param cls the entity's class
     * @return a new entity state
     */
    public EntityState newEntityState(DTClass cls) {
        return switch (cls) {
            case S2DTClass s2 -> s2EntityStateType.createState(s2.getField(), pointerCount, layoutBuilder);
            case S1DTClass s1 -> s1EntityStateType.createState(s1);
        };
    }

    /**
     * @return a new field reader for the current engine type and configured field path implementation
     */
    public FieldReader newFieldReader() {
        return getEngineType().getNewFieldReader(s2FieldPathType);
    }

    // --- set-once parse-time initializers ---

    /**
     * Used by the parser when the replay header is read.
     *
     * @param buildNumber the build number
     * @throws IllegalStateException if already set
     */
    public void setBuildNumber(int buildNumber) {
        if (buildNumberSet) throw new IllegalStateException("buildNumber already set");
        this.buildNumber = buildNumber;
        this.buildNumberSet = true;
    }

    /**
     * Used by the parser when the server info is read.
     *
     * @param millisPerTick milliseconds per tick
     * @throws IllegalStateException if already set
     */
    public void setMillisPerTick(float millisPerTick) {
        if (millisPerTickSet) throw new IllegalStateException("millisPerTick already set");
        this.millisPerTick = millisPerTick;
        this.millisPerTickSet = true;
    }

    /**
     * Used by the parser when the server info is read.
     *
     * @param gameVersion the game version
     * @throws IllegalStateException if already set
     */
    public void setGameVersion(int gameVersion) {
        if (gameVersionSet) throw new IllegalStateException("gameVersion already set");
        this.gameVersion = gameVersion;
        this.gameVersionSet = true;
    }

    /**
     * Used by the parser when the send tables are processed.
     *
     * @param pointerCount the number of pointer fields
     * @throws IllegalStateException if already set
     */
    public void setPointerCount(int pointerCount) {
        if (pointerCountSet) throw new IllegalStateException("pointerCount already set");
        this.pointerCount = pointerCount;
        this.pointerCountSet = true;
    }

    // --- query API ---

    /**
     * Returns the processor instance of exactly the given class that takes part in this run.
     *
     * @param processorClass the class of the processor
     * @param <T> the processor type
     * @return the processor, or {@code null} if there is none of that class
     */
    public <T> T getProcessor(Class<T> processorClass) {
        return executionModel.getProcessor(processorClass);
    }

    /**
     * @return the runner's current tick; {@code -1} before processing has started
     */
    public int getTick() {
        return executionModel.getRunner().getTick();
    }

    /**
     * @return the engine type of the replay
     */
    public EngineType getEngineType() {
        return executionModel.getRunner().getEngineType();
    }

    /**
     * @return the build number from the replay header, or {@code -1} if not (yet) known
     */
    public int getBuildNumber() {
        return buildNumber;
    }

    /**
     * @return the game version from the server info, or {@code -1} if not (yet) known or not available for the engine
     */
    public int getGameVersion() {
        return gameVersion;
    }

    /**
     * @return milliseconds per tick from the server info, or {@code NaN} if not yet known
     */
    public float getMillisPerTick() {
        return millisPerTick;
    }

    /**
     * @return the number of pointer fields in the Source 2 serializers; {@code 0} before the send tables are processed
     */
    public int getPointerCount() {
        return pointerCount;
    }

    /**
     * @return the entity class filter, or {@code null} if none was configured
     */
    public Predicate<DTClass> getEntityFilter() {
        return entityFilter;
    }

    /**
     * Creates an event that calls all registered listeners of the given {@code @On*} annotation.
     *
     * @param eventType the annotation type
     * @param <A> the annotation type
     * @param <E> the event type
     * @return a new event
     */
    @SuppressWarnings("unchecked")
    public <A extends Annotation, E extends Event<A>> E createEvent(Class<A> eventType) {
        return (E) executionModel.createEvent(eventType);
    }

}
