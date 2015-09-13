package skadistats.clarity.decoder;

import skadistats.clarity.model.DTClass;

public interface FieldReader<T extends DTClass> {

    int MAX_PROPERTIES = 0x3fff;

    int readFields(BitStream bs, T dtClass, Object[] state, boolean debug);
    int readDeletions(BitStream bs, int indexBits, int[] deletions);

}
