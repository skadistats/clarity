package skadistats.clarity.processor.runner;

import skadistats.clarity.event.Provides;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.source.Source;

import java.io.IOException;

@Provides({OnInputSource.class})
public abstract class AbstractFileRunner extends AbstractRunner implements FileRunner {

    protected final Source source;
    protected LoopController loopController;

    public AbstractFileRunner(Source source, EngineType engineType) throws IOException {
        super(engineType);
        this.source = source;
        engineType.skipHeaderOffsets(source);
    }

    protected void initAndRunWith(Object... processors) {
        initWithProcessors(processors);
        context.createEvent(OnInputSource.class, Source.class, LoopController.class).raise(source, loopController);
    }

    @Override
    public Source getSource() {
        return source;
    }

}
