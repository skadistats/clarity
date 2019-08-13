package skadistats.clarity.model;

public interface FieldPath<F extends FieldPath> extends Comparable<F> {

    int cur();

}
