package skadistats.clarity.decoder.unpacker.factory.s2;

import skadistats.clarity.decoder.s2.field.UnpackerProperties;
import skadistats.clarity.decoder.unpacker.Unpacker;

public interface UnpackerFactory<T> {

    Unpacker<T> createUnpacker(UnpackerProperties f);

}
