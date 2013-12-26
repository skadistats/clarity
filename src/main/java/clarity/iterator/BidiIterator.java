package clarity.iterator;

public interface BidiIterator<T> {

    public boolean hasNext();

    public boolean hasPrev();

    public T prev();

    public T next();

}
