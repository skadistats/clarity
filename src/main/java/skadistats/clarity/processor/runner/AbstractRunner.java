package skadistats.clarity.processor.runner;

import com.google.protobuf.CodedInputStream;
import skadistats.clarity.processor.reader.OnTickEnd;
import skadistats.clarity.processor.reader.OnTickStart;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public abstract class AbstractRunner<T extends Runner> implements Runner<AbstractRunner<T>> {

    private Context context;

    /* tick the user is at the end of */
    protected int tick = -1;

    protected ExecutionModel createExecutionModel(Object... processors) {
        ExecutionModel executionModel = new ExecutionModel(this);
        for (Object p : processors) {
            executionModel.addProcessor(p);
        }
        return executionModel;
    }

    protected int ensureDemHeader(InputStream ms) throws IOException {
        byte[] header = new byte[12];
        if (ms.read(header) != 12 || !"PBUFDEM\0".equals(new String(Arrays.copyOfRange(header, 0, 8)))) {
            throw new IOException("given stream does not seem to contain a valid replay");
        }
        return ByteBuffer.wrap(header, 8, 4).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get();
    }

    protected CodedInputStream createCodedInputStream(InputStream is) {
        CodedInputStream codedInputStream = CodedInputStream.newInstance(is);
        codedInputStream.setSizeLimit(Integer.MAX_VALUE);
        return codedInputStream;
    }

    protected void endTicksUntil(Context ctx, int untilTick) {
        while (tick < untilTick) {
            if (tick != -1) {
                ctx.createEvent(OnTickEnd.class).raise();
            }
            tick++;
            ctx.createEvent(OnTickStart.class).raise();
        }
        if (tick != -1) {
            ctx.createEvent(OnTickEnd.class).raise();
        }
    }

    protected void startNewTick(Context ctx) {
        tick++;
        ctx.createEvent(OnTickStart.class).raise();
    }

    public int getTick() {
        return tick;
    }

    abstract protected Source getSource();

    @Override
    public AbstractRunner<T> runWith(Object... processors) {
        ExecutionModel em = createExecutionModel(processors);
        context = new Context(em);
        em.initialize(context);
        context.createEvent(OnInputSource.class, Source.class).raise(getSource());
        return this;
    }

    public Context getContext() {
        return context;
    }

}
