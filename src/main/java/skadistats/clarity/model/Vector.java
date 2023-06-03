package skadistats.clarity.model;

import java.util.Arrays;

public class Vector {

    private final float[] v;

    public Vector(float... v) {
        this.v = v;
    }

    public int getDimension() {
        return v.length;
    }

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
