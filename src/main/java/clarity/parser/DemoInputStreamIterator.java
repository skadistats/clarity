package clarity.parser;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class DemoInputStreamIterator implements Iterator<Peek> {

    private final DemoInputStream s;
    private Peek p = null;
    private Boolean next = null;
    
    public DemoInputStreamIterator(DemoInputStream s) {
        this.s = s;
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
