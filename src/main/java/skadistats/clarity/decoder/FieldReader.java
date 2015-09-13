package skadistats.clarity.decoder;

import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.FieldPath;

public interface FieldReader<T extends DTClass> {

    int MAX_PROPERTIES = 0x3fff;

    int readFields(BitStream bs, T dtClass, FieldPath[] fieldPaths, Object[] state, boolean debug);
    int readDeletions(BitStream bs, int indexBits, int[] deletions);

}
