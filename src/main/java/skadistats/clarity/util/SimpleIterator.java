package skadistats.clarity.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class SimpleIterator<T> implements Iterator<T> {

    private T v = null;
    private Boolean next = null;

    public abstract T readNext();

    @Override
    public boolean hasNext() {
        if (next == null) {
            v = readNext();
            next = v != null;
        }
        return next.booleanValue();    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        next = null;
        return v;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }
}
