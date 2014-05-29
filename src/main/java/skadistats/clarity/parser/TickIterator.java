package skadistats.clarity.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class TickIterator implements Iterator<Tick> {

    private final DemoInputStream s;
    private Peek peek = null;
    private Boolean haveNextPeek = null;
    
    public TickIterator(DemoInputStream s) {
        this.s = s;
    }
    
    private Peek getNextPeek() {
        if (haveNextPeek == null) {
            try {
                peek = s.read();
                haveNextPeek = peek != null;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return peek;
    }
    
    private void consumeNextPeek() {
        haveNextPeek = null;
    }
    
    @Override
    public boolean hasNext() {
        return getNextPeek() != null;
    }

    @Override
    public Tick next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        List<Peek> peeks = new ArrayList<Peek>();
        while (true) {
            Peek p = getNextPeek();
            if (p == null) {
                break;
            }
            if (!peeks.isEmpty() && p.getBorder().isPeekTickBorder()) {
                break;
            }
            consumeNextPeek();
            peeks.add(p);
        }
        return new Tick(peeks);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
