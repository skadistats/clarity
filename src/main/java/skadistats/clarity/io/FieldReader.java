package skadistats.clarity.io;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.FieldPath;

import java.io.PrintStream;

public abstract class FieldReader {

    public static final int MAX_PROPERTIES = 0x3FFF;
    public static PrintStream DEBUG_STREAM = System.out;

    protected final FieldPath[] fieldPaths = new FieldPath[MAX_PROPERTIES];

    public abstract FieldChanges readFields(BitStream bs, DTClass dtClass, boolean debug);
    public abstract int readDeletions(BitStream bs, int indexBits, int[] deletions);

}
