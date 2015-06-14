package skadistats.clarity.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.lang.UnsupportedOperationException;


public final class Iterators {

    private static final Iterator<Object> EMPTY  = new SimpleIterator<Object>() {
        @Override
        public Object readNext() {
            return null;
        }
        @Override
        public void remove(){
            throw new UnsupportedOperationException();
        }
    };

    public static <T> Iterator<T> emptyIterator() {
        return (Iterator<T>) EMPTY;
    }
}
