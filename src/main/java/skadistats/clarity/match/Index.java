package skadistats.clarity.match;

import java.util.HashMap;
import java.util.Map;

public abstract class Index<K, T> {
    private final Map<K, T> idx = new HashMap<K, T>();

    abstract K getKey(T value);

    public void add(T value) {
        idx.put(getKey(value), value);
    }

    public void remove(T value) {
        idx.remove(getKey(value));
    }

    public void clear() {
        idx.clear();
    }

    public T get(K key) {
        return idx.get(key);
    }

}
