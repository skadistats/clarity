package skadistats.clarity.processor.runner;

public abstract class AbstractRunner<I> implements Runner<I> {

    private Context context;
    private int tick;

    protected abstract class AbstractSource<I> implements Source {
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

    protected void setTick(int tick) {
        this.tick = tick;
    }

    abstract protected Source getSource(I input );

    @Override
    public Runner runWith(I input, Object... processors) {
        ExecutionModel em = createExecutionModel(processors);
        context = new Context(em);
        em.initialize(context);
        context.createEvent(OnInputSource.class, Source.class).raise(getSource(input));
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
