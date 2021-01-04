package skadistats.clarity.io;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.state.EntityState;

import java.io.PrintStream;

public abstract class FieldReader<T extends DTClass> {

    public static final int MAX_PROPERTIES = 0x3FFF;
    public static PrintStream DEBUG_STREAM = System.out;

    protected final FieldPath[] fieldPaths = new FieldPath[MAX_PROPERTIES];

    public abstract Result readFields(BitStream bs, T dtClass, EntityState state, FieldPathUpdateListener fieldPathUpdateListener, boolean debug);
    public abstract int readDeletions(BitStream bs, int indexBits, int[] deletions);

    public interface FieldPathUpdateListener {
        void fieldPathUpdated(int index, FieldPath fieldPath);
    }

    public static class Result {
        private final int updateCount;
        private final boolean capacityChanged;

        public Result(int updateCount, boolean capacityChanged) {
            this.updateCount = updateCount;
            this.capacityChanged = capacityChanged;
        }

        public int getUpdateCount() {
            return updateCount;
        }

        public boolean isCapacityChanged() {
            return capacityChanged;
        }
    }

}
