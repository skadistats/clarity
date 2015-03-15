package skadistats.clarity.model;

import java.util.Arrays;

public class Vector {

    private float[] v;

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

}
