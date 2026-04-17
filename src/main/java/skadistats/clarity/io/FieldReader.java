package skadistats.clarity.io;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.state.EntityState;

import java.io.PrintStream;

public interface FieldReader<D extends DTClass, FP extends FieldPath, S extends EntityState> {

    int MAX_PROPERTIES = 0x3FFF;

    class Debug {
        public static PrintStream STREAM = System.out;
    }

    FieldChanges<FP> readFields(BitStream bs, D dtClass, S state, boolean debug, boolean materialize);

    default FieldChanges<FP> readFields(BitStream bs, D dtClass, S state, boolean debug) {
        return readFields(bs, dtClass, state, debug, false);
    }

    int readDeletions(BitStream bs, int indexBits, int[] deletions);

}
