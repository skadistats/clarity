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

public class Context {

    private final ExecutionModel executionModel;

    // structural (set at construction, immutable)
    private final S1EntityStateType s1EntityStateType;
    private final S2EntityStateType s2EntityStateType;
    private final S2FieldPathType s2FieldPathType;
    private final FieldLayoutBuilder layoutBuilder;

    // parse-time initialized (set-once)
    private int buildNumber = -1;
    private boolean buildNumberSet;
    private float millisPerTick = Float.NaN;
    private boolean millisPerTickSet;
    private int gameVersion = -1;
    private boolean gameVersionSet;
    private int pointerCount = 0;
    private boolean pointerCountSet;

    public Context(ExecutionModel executionModel, S1EntityStateType s1EntityStateType, S2EntityStateType s2EntityStateType, S2FieldPathType s2FieldPathType) {
        this.executionModel = executionModel;
        this.s1EntityStateType = s1EntityStateType;
        this.s2EntityStateType = s2EntityStateType;
        this.s2FieldPathType = s2FieldPathType;
        this.layoutBuilder = new FieldLayoutBuilder();
    }

    // --- construction API ---

    public EntityState newEntityState(DTClass cls) {
        return switch (cls) {
            case S2DTClass s2 -> s2EntityStateType.createState(s2.getField(), pointerCount, layoutBuilder);
            case S1DTClass s1 -> s1EntityStateType.createState(s1);
        };
    }

    public FieldReader newFieldReader() {
        return getEngineType().getNewFieldReader(s2FieldPathType);
    }

    // --- set-once parse-time initializers ---

    public void setBuildNumber(int buildNumber) {
        if (buildNumberSet) throw new IllegalStateException("buildNumber already set");
        this.buildNumber = buildNumber;
        this.buildNumberSet = true;
    }

    public void setMillisPerTick(float millisPerTick) {
        if (millisPerTickSet) throw new IllegalStateException("millisPerTick already set");
        this.millisPerTick = millisPerTick;
        this.millisPerTickSet = true;
    }

    public void setGameVersion(int gameVersion) {
        if (gameVersionSet) throw new IllegalStateException("gameVersion already set");
        this.gameVersion = gameVersion;
        this.gameVersionSet = true;
    }

    public void setPointerCount(int pointerCount) {
        if (pointerCountSet) throw new IllegalStateException("pointerCount already set");
        this.pointerCount = pointerCount;
        this.pointerCountSet = true;
    }

    // --- query API ---

    public <T> T getProcessor(Class<T> processorClass) {
        return executionModel.getProcessor(processorClass);
    }

    public int getTick() {
        return executionModel.getRunner().getTick();
    }

    public EngineType getEngineType() {
        return executionModel.getRunner().getEngineType();
    }

    public int getBuildNumber() {
        return buildNumber;
    }

    public int getGameVersion() {
        return gameVersion;
    }

    public float getMillisPerTick() {
        return millisPerTick;
    }

    public int getPointerCount() {
        return pointerCount;
    }

    @SuppressWarnings("unchecked")
    public <A extends Annotation, E extends Event<A>> E createEvent(Class<A> eventType) {
        return (E) executionModel.createEvent(eventType);
    }

}
