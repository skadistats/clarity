package skadistats.clarity.decoder.s2.field;

import skadistats.clarity.model.s2.S2FieldPath;

public interface AccessorFunction<V> {

    AccessorFunction<V> down(int i);
    V get();

    static <V> V performAccess(AccessorFunction<V> accessor, S2FieldPath fp) {
        switch (fp.last()) {
            case 0: return accessor.down(fp.get(0)).get();
            case 1: return accessor.down(fp.get(0)).down(fp.get(1)).get();
            case 2: return accessor.down(fp.get(0)).down(fp.get(1)).down(fp.get(2)).get();
            case 3: return accessor.down(fp.get(0)).down(fp.get(1)).down(fp.get(2)).down(fp.get(3)).get();
            case 4: return accessor.down(fp.get(0)).down(fp.get(1)).down(fp.get(2)).down(fp.get(3)).down(fp.get(4)).get();
            case 5: return accessor.down(fp.get(0)).down(fp.get(1)).down(fp.get(2)).down(fp.get(3)).down(fp.get(4)).down(fp.get(5)).get();
            default: throw new UnsupportedOperationException();
        }
    }

}
