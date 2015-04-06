package skadistats.clarity.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class Iterators {

    private static final Iterator<Object> EMPTY  = new Iterator<Object>() {
        @Override
        public boolean hasNext() {
            return false;
        }
        @Override
        public Object next() {
            throw new NoSuchElementException();
        }
    };

    public static <T> Iterator<T> emptyIterator() {
        return (Iterator<T>) EMPTY;
    }
}
