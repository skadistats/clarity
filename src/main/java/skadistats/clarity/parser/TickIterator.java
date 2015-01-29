package skadistats.clarity.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class TickIterator extends AbstractDemoInputStreamIterator<Tick> {

    private Peek peek = null;
    private Boolean haveNextPeek = null;
    
    public TickIterator(DemoInputStream s) {
        super(s);
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
            if (!peeks.isEmpty() && p.getTick() != peeks.get(0).getTick()) {
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
