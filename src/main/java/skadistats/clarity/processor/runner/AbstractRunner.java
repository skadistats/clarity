package skadistats.clarity.processor.runner;

public abstract class AbstractRunner implements Runner {

    private Context context;
    private int tick;

    protected abstract class AbstractSource implements Source {
        @Override
        public void setTick(int tick) {
            AbstractRunner.this.tick = tick;
        }
    }

    protected ExecutionModel createExecutionModel(Object... processors) {
        ExecutionModel executionModel = new ExecutionModel(this);
        for (Object p : processors) {
            executionModel.addProcessor(p);
        }
        return executionModel;
    }

    abstract protected Source getSource();

    @Override
    public Runner runWith(Object... processors) {
        ExecutionModel em = createExecutionModel(processors);
        context = new Context(em);
        em.initialize(context);
        context.createEvent(OnInputSource.class, Source.class).raise(getSource());
        return this;
    }

    public Context getContext() {
        return context;
    }

    @Override
    public int getTick() {
        return tick;
    }

}
