package skadistats.clarity.model;

import skadistats.clarity.model.s1.S1FieldPath;
import skadistats.clarity.model.s2.S2FieldPath;

public interface FieldPath<F extends FieldPath> extends Comparable<F> {

    default S1FieldPath s1() {
        return (S1FieldPath) this;
    }

    default S2FieldPath s2() {
        return (S2FieldPath) this;
    }

}
