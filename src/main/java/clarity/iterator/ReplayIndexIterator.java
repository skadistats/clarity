package clarity.iterator;

import java.util.NoSuchElementException;

import clarity.parser.Peek;
import clarity.parser.ReplayIndex;

public class ReplayIndexIterator implements BidiIterator<Peek> {

    private final ReplayIndex index;
    private final int first;
    private final int last;
    private int pos;

    public ReplayIndexIterator(ReplayIndex index, int first, int last) {
        this.index = index;
        this.first = first;
        this.last = last;
        this.pos = first;
    }

    @Override
    public boolean hasNext() {
        return pos <= last;
    }

    @Override
    public boolean hasPrev() {
        return pos > first;
    }

    @Override
    public Peek prev() {
        if (!hasPrev()) {
            throw new NoSuchElementException();
        }
        pos--;
        return index.get(pos);
    }

    @Override
    public Peek next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        Peek p = index.get(pos);
        pos++;
        return p;
    }

}
