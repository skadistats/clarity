package skadistats.clarity.util;

import java.util.Iterator;

public final class Iterators {

    private static final Iterator<Object> EMPTY  = new SimpleIterator<>() {
        @Override
        public Object readNext() {
            return null;
        }
    };

    public static <T> Iterator<T> emptyIterator() {
        return (Iterator<T>) EMPTY;
    }
}
