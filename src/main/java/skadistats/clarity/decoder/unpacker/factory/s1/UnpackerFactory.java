package skadistats.clarity.decoder.unpacker.factory.s1;

import skadistats.clarity.decoder.s1.SendProp;
import skadistats.clarity.decoder.unpacker.Unpacker;

public interface UnpackerFactory<T> {

    Unpacker<T> createUnpacker(SendProp prop);

}
