package skadistats.clarity.state;

import skadistats.clarity.model.FieldPath;

/**
 * Concrete {@link StateDelta} backing.
 *
 * Sized to exactly the number of captured field paths. Per-slot storage is
 * one of four shapes selected by a compact type tag:
 *
 * <ul>
 *   <li>{@link #TAG_INT}   — primitive bits stored in {@link #prim}</li>
 *   <li>{@link #TAG_LONG}  — primitive bits stored in {@link #prim}</li>
 *   <li>{@link #TAG_FLOAT} — float bits stored in {@link #prim} via {@link Float#floatToRawIntBits}</li>
 *   <li>{@link #TAG_OBJECT} — reference stored in {@link #obj}</li>
 * </ul>
 *
 * The {@link #obj} array is only allocated when at least one slot is
 * object-typed. Field-path lookup is a linear scan over {@link #fps} —
 * adequate for the small-num case that is the overwhelming default and
 * cheaper than hashing overhead at that scale.
 */
public final class SparseStateDelta implements StateDelta {

    public static final byte TAG_EMPTY  = 0;
    public static final byte TAG_INT    = 1;
    public static final byte TAG_LONG   = 2;
    public static final byte TAG_FLOAT  = 3;
    public static final byte TAG_OBJECT = 4;

    private final FieldPath[] fps;
    private final byte[] tags;
    private final long[] prim;
    private final Object[] obj;

    public SparseStateDelta(int num) {
        this.fps  = new FieldPath[num];
        this.tags = new byte[num];
        this.prim = new long[num];
        this.obj  = null;
    }

    public SparseStateDelta(int num, boolean needsObjectSlab) {
        this.fps  = new FieldPath[num];
        this.tags = new byte[num];
        this.prim = new long[num];
        this.obj  = needsObjectSlab ? new Object[num] : null;
    }

    public void putInt(int i, FieldPath fp, int value) {
        fps[i] = fp;
        tags[i] = TAG_INT;
        prim[i] = value & 0xFFFFFFFFL;
    }

    public void putLong(int i, FieldPath fp, long value) {
        fps[i] = fp;
        tags[i] = TAG_LONG;
        prim[i] = value;
    }

    public void putFloat(int i, FieldPath fp, float value) {
        fps[i] = fp;
        tags[i] = TAG_FLOAT;
        prim[i] = Float.floatToRawIntBits(value) & 0xFFFFFFFFL;
    }

    public void putObject(int i, FieldPath fp, Object value) {
        fps[i] = fp;
        tags[i] = TAG_OBJECT;
        ensureObjSlab()[i] = value;
    }

    public void putEmpty(int i, FieldPath fp) {
        fps[i] = fp;
        tags[i] = TAG_EMPTY;
    }

    private Object[] ensureObjSlab() {
        if (obj != null) return obj;
        throw new IllegalStateException(
                "SparseStateDelta was allocated without an object slab; "
              + "use the (num, needsObjectSlab=true) constructor when any captured slot is object-typed");
    }

    public byte tagAt(int i) {
        return tags[i];
    }

    public long primAt(int i) {
        return prim[i];
    }

    public Object objAt(int i) {
        return obj == null ? null : obj[i];
    }

    @Override
    public FieldPath[] fields() {
        return fps;
    }

    @Override
    public int getInt(FieldPath fp) {
        var i = indexOf(fp);
        if (i < 0) return 0;
        if (tags[i] == TAG_INT) return (int) prim[i];
        // Nested-source captures store the wrapper as-is; allow free unbox.
        if (tags[i] == TAG_OBJECT && obj != null && obj[i] instanceof Integer v) return v;
        return 0;
    }

    @Override
    public long getLong(FieldPath fp) {
        var i = indexOf(fp);
        if (i < 0) return 0L;
        if (tags[i] == TAG_LONG) return prim[i];
        if (tags[i] == TAG_OBJECT && obj != null && obj[i] instanceof Long v) return v;
        return 0L;
    }

    @Override
    public float getFloat(FieldPath fp) {
        var i = indexOf(fp);
        if (i < 0) return 0.0f;
        if (tags[i] == TAG_FLOAT) return Float.intBitsToFloat((int) prim[i]);
        if (tags[i] == TAG_OBJECT && obj != null && obj[i] instanceof Float v) return v;
        return 0.0f;
    }

    @Override
    public Object getObject(FieldPath fp) {
        var i = indexOf(fp);
        if (i < 0 || tags[i] != TAG_OBJECT || obj == null) return null;
        return obj[i];
    }

    public int indexOf(FieldPath fp) {
        for (var i = 0; i < fps.length; i++) {
            if (fps[i] != null && fps[i].equals(fp)) return i;
        }
        return -1;
    }
}
