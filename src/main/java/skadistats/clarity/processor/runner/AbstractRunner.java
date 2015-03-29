package skadistats.clarity.processor.runner;

import com.google.protobuf.CodedInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

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
