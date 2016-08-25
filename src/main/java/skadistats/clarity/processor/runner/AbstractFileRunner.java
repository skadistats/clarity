package skadistats.clarity.processor.runner;

import skadistats.clarity.model.EngineType;
import skadistats.clarity.source.Source;

import java.io.IOException;

public abstract class AbstractFileRunner<T extends AbstractFileRunner<? super T>> extends AbstractRunner<AbstractFileRunner<T>> implements FileRunner<AbstractFileRunner<T>> {

    protected final Source source;
    protected LoopController loopController;

    public AbstractFileRunner(Source source, EngineType engineType) throws IOException {
        super(engineType);
        this.source = source;
        engineType.skipHeaderOffsets(source);
    }

    @Override
    public AbstractFileRunner<T> runWith(Object... processors) {
        initWithProcessors(processors);
        context.createEvent(OnInputSource.class, Source.class, LoopController.class).raise(source, loopController);
        return this;
    }

    @Override
    public Source getSource() {
        return source;
    }

}
