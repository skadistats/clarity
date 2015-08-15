package skadistats.clarity.decoder.s2;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.model.s2.Field;

public interface FieldDecoder<T> {

    T decode(BitStream bs, Field f);

}
