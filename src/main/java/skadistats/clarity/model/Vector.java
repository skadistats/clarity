package skadistats.clarity.model;

import java.util.Arrays;

/**
 * An immutable-by-convention float vector of arbitrary dimension, used as the
 * value type of vector-typed entity properties. Wraps the given array without
 * copying. {@code equals} and {@code hashCode} compare the elements.
 */
public class Vector {

    private final float[] v;

    /** Creates a vector from the given components; the array is not copied. */
    public Vector(float... v) {
        this.v = v;
    }

    /**
     * @return the number of elements
     */
    public int getDimension() {
        return v.length;
    }

    /**
     * @throws ArrayIndexOutOfBoundsException if {@code i} is out of range
     */
    public float getElement(int i) {
        return v[i];
    }

    @Override
    public String toString() {
        return Arrays.toString(v);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        var vector = (Vector) o;

        return Arrays.equals(v, vector.v);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(v);
    }

}
