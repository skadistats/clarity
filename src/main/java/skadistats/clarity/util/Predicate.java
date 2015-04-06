package skadistats.clarity.util;

public interface Predicate<T> {
    boolean apply(T value);
}
