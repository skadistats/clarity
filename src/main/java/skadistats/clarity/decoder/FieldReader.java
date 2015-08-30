package skadistats.clarity.decoder;

import skadistats.clarity.model.DTClass;

public interface FieldReader<T extends DTClass> {

    void readFields(BitStream bs, T dtClass, Object[] state, boolean debug);

}
