package skadistats.clarity.parser;

import java.io.IOException;
import java.util.NoSuchElementException;

public class PeekIterator extends AbstractDemoInputStreamIterator<Peek> {

    private Peek p = null;
    private Boolean next = null;
    
    public PeekIterator(DemoInputStream s) {
        super(s);
    }

    @Override
    public boolean hasNext() {
        if (next == null) {
            try {
                p = s.read();
                next = p != null;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return next.booleanValue();
    }

    @Override
    public Peek next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        next = null;
        return p;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
