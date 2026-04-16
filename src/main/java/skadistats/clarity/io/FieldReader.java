package skadistats.clarity.io;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.state.EntityState;

import java.io.PrintStream;

public abstract class FieldReader {

    public static final int MAX_PROPERTIES = 0x3FFF;
    public static PrintStream DEBUG_STREAM = System.out;

    protected final FieldPath[] fieldPaths = new FieldPath[MAX_PROPERTIES];

    public abstract FieldChanges readFields(BitStream bs, DTClass dtClass, EntityState state, boolean debug, boolean materialize);

    public final FieldChanges readFields(BitStream bs, DTClass dtClass, EntityState state, boolean debug) {
        return readFields(bs, dtClass, state, debug, false);
    }

    public abstract int readDeletions(BitStream bs, int indexBits, int[] deletions);

}
