package skadistats.clarity.decoder.unpacker.factory.s1;

import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.s1.SendProp;

public interface UnpackerFactory<T> {

    Unpacker<T> createUnpacker(SendProp prop);

}
