package skadistats.clarity.decoder;

import skadistats.clarity.model.DTClass;

import java.io.PrintStream;

public abstract class FieldReader<T extends DTClass> {

    public static final int MAX_PROPERTIES = 0x3FFF;
    public static PrintStream DEBUG_STREAM = System.out;

    public abstract int readFields(BitStream bs, T dtClass, Object[] state, boolean debug);
    public abstract int readDeletions(BitStream bs, int indexBits, int[] deletions);

}
