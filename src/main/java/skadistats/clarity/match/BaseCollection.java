package skadistats.clarity.match;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;
import com.rits.cloning.Cloner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class BaseCollection<S, T> implements Cloneable, Iterable<T> {

    private static final Cloner CLONER = new Cloner();

    protected final List<Index> indices;
    protected final List<T> values;

    public BaseCollection() {
        values = initialValues();
        indices = initialIndices();
    }

    protected List<T> initialValues() {
        return new ArrayList<T>();
    }

    protected List<Index> initialIndices() {
        return new ArrayList<Index>();
    }

    public void add(T value) {
        values.add(value);
        for (Index i : indices) {
            i.add(value);
        }
    }

    public void clear() {
        values.clear();
        for (Index i : indices) {
            i.clear();
        }
    }

    public T getByIndex(int index) {
        return values.get(index);
    }

    public Iterator<T> iteratorForPredicate(Predicate<T> predicate) {
        return Iterators.filter(
            values.iterator(),
            Predicates.and(
                Predicates.notNull(),
                predicate
            )
        );
    }

    public T getByPredicate(Predicate<T> predicate) {
        Iterator<T> iter = iteratorForPredicate(predicate);
        return iter.hasNext() ? iter.next() : null;
    }

    public List<T> getAll() {
        return Collections.unmodifiableList(values);
    }

    @Override
    public Iterator<T> iterator() {
        return values.iterator();
    }

    @Override
    public S clone() { return (S) CLONER.deepClone(this); }

}
